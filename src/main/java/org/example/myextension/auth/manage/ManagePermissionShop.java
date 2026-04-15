package org.example.myextension.auth.manage;

/**
 * 管理后台店铺范围枚举
 * <p>
 * 定义管理后台权限校验中的店铺类型，用于限制用户只能在特定类型的店铺中访问资源。
 * <p>
 * <b>店铺类型说明：</b>
 * <table border="1">
 *   <tr><th>类型</th><th>说明</th></tr>
 *   <tr><td>Root</td><td>总店，即主店铺，通常拥有最高权限和完整的业务数据</td></tr>
 *   <tr><td>Sub</td><td>分店/子店，即从主店铺分出来的店铺，权限和业务数据可能受限</td></tr>
 * </table>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 仅允许在总店访问
 * &#64;ManageAuthPermission(shopRange = {ManagePermissionShop.Root})
 * public void manageRootShop() { ... }
 *
 * // 允许在总店和分店访问
 * &#64;ManageAuthPermission(shopRange = {ManagePermissionShop.Root, ManagePermissionShop.Sub})
 * public void manageAllShops() { ... }
 *
 * // 仅允许在分店访问
 * &#64;ManageAuthPermission(shopRange = {ManagePermissionShop.Sub})
 * public void manageSubShop() { ... }
 * </pre>
 * <p>
 * <b>注意事项：</b>
 * <ul>
 *   <li>此枚举目前仅在注解层面声明，实际校验逻辑可能需要配合其他实现</li>
 *   <li>店铺类型的判断依赖于店铺数据中的标识字段</li>
 *   <li>同一个用户可能在总店和分店有不同的角色</li>
 * </ul>
 *
 * @see ManageAuthPermission 权限校验注解
 * @see ManageAuthInfo 用户信息模型
 */
public enum ManagePermissionShop {
    /**
     * 总店
     * <p>
     * 表示主店铺，通常拥有完整的业务数据和最高权限。
     * 总店可以管理其下属的分店，并拥有所有业务功能的访问权限。
     */
    Root,

    /**
     * 分店/子店
     * <p>
     * 表示从主店铺分出来的店铺，业务数据和权限可能受限。
     * 分店通常只能访问自己的业务数据，不能访问总店和其他分店的数据。
     */
    Sub
}
