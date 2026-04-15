package org.example.myextension.auth.manage;

/**
 * 管理后台权限枚举
 * <p>
 * 定义管理后台中的用户角色/权限类型，用于权限校验。
 * 每个枚举值对应一个整数值，表示角色的优先级或唯一标识。
 * <p>
 * <b>角色说明：</b>
 * <table border="1">
 *   <tr><th>角色</th><th>值</th><th>说明</th></tr>
 *   <tr><td>Admin</td><td>0</td><td>普通管理员，拥有基础管理权限</td></tr>
 *   <tr><td>SuperAdmin</td><td>1</td><td>高级管理员，拥有所有权限</td></tr>
 *   <tr><td>Agent</td><td>2</td><td>代理商，拥有代理商相关权限</td></tr>
 *   <tr><td>Verifier</td><td>3</td><td>审核员，拥有审核相关权限</td></tr>
 * </table>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 仅允许高级管理员访问
 * &#64;ManageAuthPermission(permissions = {ManagePermissionEnum.SuperAdmin})
 * public void sensitiveOperation() { ... }
 *
 * // 允许普通管理员和高级管理员访问
 * &#64;ManageAuthPermission(permissions = {ManagePermissionEnum.Admin, ManagePermissionEnum.SuperAdmin})
 * public void commonOperation() { ... }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>角色的实际权限由系统具体实现定义，此枚举仅用于标识角色类型</li>
 *   <li>值越小不一定表示权限越低，具体取决于业务实现</li>
 *   <li>同一个用户在不同店铺可能有不同的角色</li>
 * </ul>
 *
 * @see ManageAuthPermission 权限校验注解
 * @see ManageAuthInfo 用户信息模型
 */
public enum ManagePermissionEnum {
    /**
     * 普通管理员
     * <p>
     * 值为 0，表示基础管理权限。
     * 普通管理员通常拥有日常业务操作权限，但不包括敏感或高级功能。
     */
    Admin(0),

    /**
     * 高级管理员
     * <p>
     * 值为 1，表示拥有所有权限的管理员。
     * 高级管理员通常拥有系统配置、用户管理、权限分配等高级功能权限。
     */
    SuperAdmin(1),

    /**
     * 代理商
     * <p>
     * 值为 2，表示代理商身份。
     * 代理商通常拥有与其代理业务相关的权限，可能包括代理店铺管理、结算查看等。
     */
    Agent(2),

    /**
     * 审核员
     * <p>
     * 值为 3，表示审核员身份。
     * 审核员通常拥有审核相关内容的权限，如订单审核、商品审核等。
     */
    Verifier(3);

    /**
     * 角色对应的整数值
     * <p>
     * 用于数据库存储、权限比较等场景。
     */
    private final int value;

    /**
     * 枚举构造函数
     *
     * @param value 角色对应的整数值
     */
    ManagePermissionEnum(int value) {
        this.value = value;
    }

    /**
     * 获取角色对应的整数值
     *
     * @return 角色整数值
     */
    public int getValue() {
        return value;
    }
}
