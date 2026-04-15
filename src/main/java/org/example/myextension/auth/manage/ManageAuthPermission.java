package org.example.myextension.auth.manage;

import java.lang.annotation.*;

/**
 * 管理后台权限校验注解
 * <p>
 * 用于标记需要进行权限校验的方法。
 * 使用 AOP 切面拦截带有此注解的方法，在方法执行前进行权限校验。
 * 如果权限校验失败，会抛出 {@link ManageAuthPermissionException}。
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 示例 1：仅允许高级管理员访问
 * &#64;ManageAuthPermission(permissions = {ManagePermissionEnum.SuperAdmin})
 * public void sensitiveOperation() {
 *     // 敏感操作代码
 * }
 *
 * // 示例 2：允许在任意店铺中拥有管理员角色的用户访问
 * &#64;ManageAuthPermission(permissions = {ManagePermissionEnum.Admin}, shopMatch = false)
 * public void viewAllShops() {
 *     // 查看所有店铺的代码
 * }
 *
 * // 示例 3：仅允许在总店中拥有代理商标识的用户访问
 * &#64;ManageAuthPermission(permissions = {ManagePermissionEnum.Agent}, shopRange = {ManagePermissionShop.Root})
 * public void manageRootShop() {
 *     // 管理总店的代码
 * }
 * </pre>
 * <p>
 * <b>权限校验逻辑：</b>
 * <ol>
 *   <li>检查认证开关是否开启，未开启则使用模拟用户跳过校验</li>
 *   <li>检查是否为 admin 用户，是则直接通过</li>
   *   <li>解析 Token 获取用户信息和店铺权限</li>
   *   <li>根据注解参数校验用户是否有所需权限</li>
 *   <li>校验通过则执行方法，校验失败则抛出异常</li>
 * </ol>
 *
 * @see ManageAuthPermissionAspect 权限校验切面实现
 * @see ManagePermissionEnum 权限枚举
 * @see ManagePermissionShop 店铺范围枚举
 * @see ManageAuthPermissionException 权限异常
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManageAuthPermission {
    /**
     * 所需权限列表
     * <p>
     * 指定允许访问该方法所需的用户角色/权限。
     * 用户在当前店铺的角色必须包含在列表中才允许访问。
     * <p>
     * 默认为空数组，表示不限制权限（只要能通过店铺范围限制即可）。
     * <p>
     * <b>支持的权限：</b>
     * <ul>
     *   <li>{@link ManagePermissionEnum#Admin}：普通管理员</li>
     *   <li>{@link ManagePermissionEnum#SuperAdmin}：高级管理员</li>
     *   <li>{@link ManagePermissionEnum#Agent}：代理商</li>
     *   <li>{@link ManagePermissionEnum#Verifier}：审核员</li>
     * </ul>
     *
     * @return 允许的权限列表
     */
    ManagePermissionEnum[] permissions() default {};

    /**
     * 店铺范围限制
     * <p>
     * 指定允许访问的店铺类型，用于限制用户只能在总店或分店中访问。
     * 默认包含总店和分店，即不限制店铺类型。
     * <p>
     * <b>支持的店铺类型：</b>
     * <ul>
     *   <li>{@link ManagePermissionShop#Root}：总店</li>
     *   <li>{@link ManagePermissionShop#Sub}：分店/子店</li>
     * </ul>
     * <p>
     * <b>注意：</b> 此参数目前仅在注解层面声明，实际校验逻辑可能需要配合其他实现。
     *
     * @return 允许的店铺类型列表
     */
    ManagePermissionShop[] shopRange() default {ManagePermissionShop.Root, ManagePermissionShop.Sub};

    /**
     * 是否必须与当前所处店铺匹配
     * <p>
     * <ul>
     *   <li>true（默认）：用户在当前店铺的角色必须满足权限要求</li>
     *   <li>false：用户在任意店铺的角色满足权限要求即可</li>
     * </ul>
     * <p>
     * <b>使用场景：</b>
     * <ul>
     *   <li>true：适用于需要操作当前店铺数据的场景</li>
   *   <li>false：适用于需要跨店铺查询或操作的场景</li>
     * </ul>
     *
     * @return true 表示必须与当前店铺匹配，false 表示不限制
     */
    boolean shopMatch() default true;
}
