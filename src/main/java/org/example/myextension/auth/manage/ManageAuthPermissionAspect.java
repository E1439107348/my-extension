package org.example.myextension.auth.manage;

import org.example.myextension.ExtensionEnv;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台权限校验切面
 * <p>
 * 使用 Spring AOP 拦截带有 {@link ManageAuthPermission} 注解的方法，
 * 在方法执行前进行权限校验。如果校验失败，抛出 {@link ManageAuthPermissionException}。
 * <p>
 * <b>校验流程：</b>
 * <ol>
 *   <li>检查认证开关是否开启，未开启则使用模拟用户跳过校验</li>
 *   <li>检查是否为 admin 用户（Cookie 中的 mobile），是则直接通过</li>
 *   <li>获取当前店铺 ID（Cookie 中的 kdt_id）</li>
 *   <li>解析 ISV Token 获取用户信息和店铺权限</li>
 *   <li>根据注解参数校验用户是否有所需权限</li>
 *   <li>校验通过则执行方法，校验失败则抛出异常</li>
 * </ol>
 * <p>
 * <b>权限校验逻辑：</b>
 * <ul>
 *   <li>当 {@code shopMatch = true} 时：用户在当前店铺的角色必须包含在允许的角色列表中</li>
 *   <li>当 {@code shopMatch = false} 时：用户在任意店铺的角色包含在允许的角色列表中即可</li>
 *   <li>当权限列表为空时：仅通过店铺范围限制，不限制角色</li>
 * </ul>
 * <p>
 * <b>依赖配置：</b>
 * <ul>
 *   <li>{@code extension.manage.auth.enable}: 是否开启管理后台认证（默认 false）</li>
 *   <li>{@code extension.env.tokenResolverUrl}: Token 解析服务地址</li>
 * </ul>
 *
 * @see ManageAuthPermission 权限校验注解
 * @see ManageAuthIsvTokenUtil ISV Token 工具类
 * @see ManageAuthInfo 用户信息模型
 */
@Aspect
@Component
public class ManageAuthPermissionAspect {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ManageAuthPermissionAspect.class);

    /**
     * 环境配置对象
     * <p>
     * 用于获取认证开关等配置信息。
     */
    private final ExtensionEnv env;

    /**
     * ISV Token 工具类
     * <p>
     * 用于解析 Token 和管理 Session 中的用户信息。
     */
    private final ManageAuthIsvTokenUtil tokenUtil;

    /**
     * 构造函数：通过依赖注入初始化
     *
     * @param env       环境配置对象
     * @param tokenUtil ISV Token 工具类
     */
    public ManageAuthPermissionAspect(ExtensionEnv env, ManageAuthIsvTokenUtil tokenUtil) {
        this.env = env;
        this.tokenUtil = tokenUtil;
    }

    /**
     * 定义切点：拦截所有带有 {@link ManageAuthPermission} 注解的方法
     */
    @Pointcut("@annotation(org.example.myextension.auth.manage.ManageAuthPermission)")
    public void point() {}

    /**
     * 前置通知：在目标方法执行前进行权限校验
     * <p>
     * 执行完整的权限校验流程，如果校验失败则抛出 {@link ManageAuthPermissionException}。
     *
     * @param joinPoint 连接点，包含被拦截方法的信息
     * @throws ManageAuthPermissionException 权限校验失败时抛出
     */
    @Before("point()")
    public void doBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 1. 开关校验：如果未开启认证，使用模拟用户跳过校验
        if (!"true".equalsIgnoreCase(env.getManageAuthEnable())) {
            log.info("认证: 无需认证");
            ManageAuthInfo mockInfo = new ManageAuthInfo();
            mockInfo.setNickName("Admin");
            mockInfo.setUserId("0");
            tokenUtil.updateUserInfo(request, mockInfo);
            return;
        }

        // 2. 超级管理员/Admin 逻辑：如果是 admin 用户，直接通过
        String mobile = getCookieValue(request, "mobile");
        if ("admin".equals(mobile)) {
            log.info("认证: 认证通过 (Admin)");
            ManageAuthInfo adminInfo = new ManageAuthInfo();
            adminInfo.setNickName("Admin");
            adminInfo.setUserId("0");
            adminInfo.setMobile(mobile);
            tokenUtil.updateUserInfo(request, adminInfo);
            return;
        }

        // 3. 店铺 ID 获取：从 Cookie 中获取当前店铺 ID
        String currentKdtId = getCookieValue(request, "kdt_id");
        if (currentKdtId == null) {
            throw new ManageAuthPermissionException("无法获取当前所在店铺");
        }

        // 4. 获取注解参数：解析方法上的 {@link ManageAuthPermission} 注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        ManageAuthPermission annotation = signature.getMethod().getAnnotation(ManageAuthPermission.class);

        // 5. 用户角色解析与权限合并
        // 调用 Token 解析服务获取用户信息
        ManageAuthInfo info = tokenUtil.parse(request);
        Map<String, Integer> shopRoleInfo = new HashMap<>();
        if (info != null && info.getAuthorizedShops() != null) {
            // 合并所有授权店铺的角色（格式：kdtId -> roleId）
            for (Map<String, Integer> shopMap : info.getAuthorizedShops()) {
                shopRoleInfo.putAll(shopMap);
            }
        }

        // 提取注解中要求的角色列表
        List<Integer> requiredRoles = Arrays.stream(annotation.permissions())
                .map(ManagePermissionEnum::getValue)
                .collect(Collectors.toList());

        // 6. 执行核心校验：判断用户是否有权限
        boolean hasPermission = validatePermission(currentKdtId, annotation.shopMatch(), shopRoleInfo, requiredRoles);

        if (hasPermission) {
            log.info("认证: 认证通过");
            if (info != null) {
                info.setMobile(mobile);
                tokenUtil.updateUserInfo(request, info);
            }
        } else {
            throw new ManageAuthPermissionException("暂无访问权限");
        }
    }

    /**
     * 核心权限校验逻辑
     * <p>
     * 根据店铺匹配模式和所需角色列表，判断用户是否有权限。
     *
     * @param kdtId     当前店铺 ID
     * @param shopMatch 是否必须与当前店铺匹配（true 表示必须在当前店铺有权限）
     * @param roleMap   用户的店铺角色映射（kdtId -> roleId）
     * @param limits    允许的角色列表
     * @return true 表示有权限，false 表示无权限
     */
    private boolean validatePermission(String kdtId, boolean shopMatch, Map<String, Integer> roleMap, List<Integer> limits) {
        if (shopMatch) {
            // 必须与当前店铺匹配
            if (limits.isEmpty()) {
                return true; // 没有限制角色，通过
            }
            Integer role = roleMap.get(kdtId);
            return role != null && limits.contains(role);
        } else {
            // 不限制店铺，只要在任意店铺有允许的角色即可
            if (limits.isEmpty()) {
                return true; // 没有限制角色，通过
            }
            return roleMap.values().stream().anyMatch(limits::contains);
        }
    }

    /**
     * 从 Cookie 中获取指定名称的值
     *
     * @param request HTTP 请求对象
     * @param name   Cookie 名称
     * @return Cookie 值，如果不存在则返回 null
     */
    private String getCookieValue(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
