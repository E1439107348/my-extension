package org.example.myextension.auth.manage;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.example.myextension.ExtensionEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * ISV Token 认证工具类
 * <p>
 * 负责处理管理后台的 ISV Token 认证相关操作，包括：
 * <ul>
 *   <li>从请求 Cookie 中解析 ISV Token</li>
 *   <li>调用 Token 解析服务获取用户身份信息</li>
 *   <li>在 Session 中存储用户信息</li>
 *   <li>从 Session 中获取当前登录用户信息</li>
 * </ul>
 * <p>
 * <b>ISV Token 说明：</b>
 * ISV Token 是由主平台颁发给 ISV（独立软件开发商）的认证令牌，
 * 用于标识和验证 ISV 应用的访问权限。
 * <p>
 * <b>使用流程：</b>
 * <ol>
 *   <li>用户通过 Cookie 携带 isv-token 访问管理后台</li>
 *   <li>拦截器从 Cookie 中提取 token</li>
 *   <li>调用 Token 解析服务验证 token 并获取用户信息</li>
 *   <li>将用户信息存储到 Session 中</li>
 *   <li>后续请求直接从 Session 中获取用户信息</li>
 * </ol>
 * <p>
 * <b>依赖配置：</b>
 * 需要配置 Token 解析服务的地址：{@code extension.env.tokenResolverUrl}
 *
 * @see ManageAuthInfo 用户信息模型
 * @see ManageAuthPermissionAspect 权限校验切面
 */
@Component
public class ManageAuthIsvTokenUtil {
    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(ManageAuthIsvTokenUtil.class);

    /**
     * Session 中存储用户信息的 Key
     * <p>
     * 用于在 Session 中唯一标识用户信息存储位置。
     */
    private static final String SESSION_USER_KEY = "AUTH_USER_INFO";

    /**
     * 环境配置对象
     * <p>
     * 用于获取 Token 解析服务的 URL 等配置信息。
     */
    private final ExtensionEnv env;

    /**
     * Spring RestTemplate 实例
     * <p>
     * 用于调用 Token 解析服务的 HTTP 客户端。
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 构造函数：通过依赖注入初始化
     *
     * @param env 环境配置对象
     */
    public ManageAuthIsvTokenUtil(ExtensionEnv env) {
        this.env = env;
    }

    /**
     * 从请求 Cookie 中解析 ISV Token 并获取身份信息
     * <p>
     * 此方法执行以下操作：
     * <ol>
     *   <li>从请求 Cookie 中查找名为 "isv-token" 的 Cookie</li>
     *   <li>调用 Token 解析服务，传入 isv-token 参数</li>
     *   <li>解析响应结果，提取用户身份信息</li>
     * </ol>
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果 Cookie 中没有 isv-token，抛出 {@link ManageAuthPermissionException}</li>
     *   <li>如果 Token 解析失败或返回错误，记录日志并返回 null</li>
     * </ul>
     *
     * @param request HTTP 请求对象
     * @return 解析到的用户身份信息，如果解析失败则返回 null
     * @throws ManageAuthPermissionException 当 Cookie 中没有 isv-token 时抛出
     */
    public ManageAuthInfo parse(HttpServletRequest request) {
        // 从 Cookie 中提取 isv-token
        String token = Arrays.stream(request.getCookies() != null ? request.getCookies() : new Cookie[0])
                .filter(c -> "isv-token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new ManageAuthPermissionException("用户未授权"));

        try {
            // 调用 Token 解析服务
            String url = env.getTokenResolverUrl() + "/oauth/getIdentityInfo?isvToken=" + token;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("认证: token 解析数据返回: {}", response.getBody());

            // 解析响应结果
            if (response.getStatusCode() == HttpStatus.OK) {
                TokenResolveResult result = JSON.parseObject(response.getBody(), TokenResolveResult.class);
                if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
                    return result.getData();
                }
            }
        } catch (Exception e) {
            log.error("ISV-Token: 请求解析 token 失败", e);
        }
        return null;
    }

    /**
     * 更新请求 Session 中的用户信息
     * <p>
     * 将用户信息存储到当前请求的 Session 中，方便后续请求快速获取。
     *
     * @param request HTTP 请求对象
     * @param user    用户信息对象
     */
    public void updateUserInfo(HttpServletRequest request, ManageAuthInfo user) {
        request.getSession().setAttribute(SESSION_USER_KEY, user);
    }

    /**
     * 获取当前请求的用户信息
     * <p>
     * 从当前请求的 Session 中获取用户信息。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果无法获取请求上下文，抛出 {@link ManageAuthPermissionException}</li>
     *   <li>如果 Session 中没有用户信息，抛出 {@link ManageAuthPermissionException}</li>
     * </ul>
     *
     * @return 当前登录用户的身份信息
     * @throws ManageAuthPermissionException 当无法获取用户信息时抛出
     */
    public ManageAuthInfo userInfo() {
        // 获取请求上下文
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) {
            throw new ManageAuthPermissionException("当前请求环境异常");
        }

        // 从 Session 中获取用户信息
        ManageAuthInfo info = (ManageAuthInfo) attr.getRequest().getSession().getAttribute(SESSION_USER_KEY);
        if (info == null) {
            throw new ManageAuthPermissionException("用户未授权");
        }
        return info;
    }

    /**
     * Token 解析结果包装类
     * <p>
     * 用于封装 Token 解析服务的响应结果。
     */
    @Data
    public static class TokenResolveResult {
        /**
         * 响应码
         */
        private Integer code;

        /**
         * 解析到的用户信息
         */
        private ManageAuthInfo data;

        /**
         * 响应消息
         */
        private String msg;

        /**
         * 是否成功
         */
        private Boolean success;
    }
}
