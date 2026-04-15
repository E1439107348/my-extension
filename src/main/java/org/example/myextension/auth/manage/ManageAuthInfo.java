package org.example.myextension.auth.manage;

import lombok.Data;

/**
 * 管理后台认证用户信息模型
 * <p>
 * 封装从 ISV Token 解析服务获取的管理后台用户身份信息。
 * 此对象用于在应用中传递和管理当前登录用户的详细信息。
 * <p>
 * <b>字段说明：</b>
 * <ul>
 *   <li>{@code authorizedShops}: 用户有权限访问的店铺列表，每个元素是一个 Map，key 为店铺 ID，value 为角色 ID</li>
 *   <li>{@code avatar}: 用户头像 URL</li>
 *   <li>{@code nickName}: 用户昵称</li>
 *   <li>{@code userId}: 用户唯一标识</li>
 *   <li>{@code role}: 用户角色，0 表示普通管理员，1 表示高级管理员</li>
 *   <li>{@code mobile}: 用户手机号</li>
 * </ul>
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>用户认证：从 Token 解析服务获取并存储用户信息</li>
 *   <li>权限校验：根据用户的店铺权限和角色判断是否有权限访问某个资源</li>
 *   <li>审计日志：记录操作用户的身份信息</li>
 * </ul>
 * <p>
 * <b>角色说明：</b>
 * <ul>
 *   <li>0：普通管理员 - 基础权限</li>
 *   <li>1：高级管理员 - 拥有所有权限</li>
 *   <li>2：代理商 - 特定代理商权限</li>
 *   <li>3：审核员 - 审核相关权限</li>
 * </ul>
 *
 * @see ManagePermissionEnum 权限枚举定义
 * @see ManagePermissionShop 店铺范围枚举定义
 * @see ManageAuthIsvTokenUtil ISV Token 工具类
 */
@Data
public class ManageAuthInfo {
    /**
     * 授权店铺列表
     * <p>
     * 用户有权限访问的所有店铺信息，每个元素是一个 Map，格式为：
     * <pre>
     * {
     *   "店铺ID": 角色ID,
     *   "123456": 1,
     *   "789012": 0
     * }
     * </pre>
     * 一个用户可能拥有多个店铺的不同角色权限。
     */
    private java.util.List<java.util.Map<String, Integer>> authorizedShops;

    /**
     * 用户头像 URL
     * <p>
     * 用户头像图片的访问地址，用于在管理后台展示用户头像。
     */
    private String avatar;

    /**
     * 用户昵称
     * <p>
     * 用户的显示名称，不一定唯一，用于友好展示。
     */
    private String nickName;

    /**
     * 用户唯一标识
     * <p>
     * 用户的系统唯一 ID，用于关联用户的业务数据。
     */
    private String userId;

    /**
     * 用户角色
     * <p>
     * 用户的主要角色标识：
     * <ul>
     *   <li>0：普通管理员</li>
     *   <li>1：高级管理员</li>
     *   <li>2：代理商</li>
     *   <li>3：审核员</li>
     * </ul>
     * 注意：用户的实际权限由 {@code authorizedShops} 中的角色决定，此字段可能仅作为用户主要角色的标识。
     */
    private Integer role;

    /**
     * 用户手机号
     * <p>
     * 用户的注册手机号，可用于身份验证和联系用户。
     * 在特殊权限场景下（如 admin 账号），会根据此字段进行特殊处理。
     */
    private String mobile;
}
