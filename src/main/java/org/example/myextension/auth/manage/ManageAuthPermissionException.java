package org.example.myextension.auth.manage;

import org.springframework.http.HttpStatus;

/**
 * 管理后台权限异常
 * <p>
 * 当用户访问需要权限的资源但未满足权限要求时抛出此异常。
 * 此异常包含 HTTP 响应状态码，用于统一错误处理和返回给客户端。
 * <p>
 * <b>使用场景：</b>
 * <ul>
 *   <li>用户未登录或 Token 无效</li>
 *   <li>用户在当前店铺没有相应角色</li>
 *   <li>用户在所有店铺都没有允许的角色</li>
 *   <li>其他权限校验失败场景</li>
 * </ul>
 * <p>
 * <b>异常处理：</b>
 * 此异常通常被全局异常处理器捕获，返回相应的 HTTP 状态码和错误消息。
 * 默认使用 401 Unauthorized 状态码。
 *
 * @see ManageAuthPermission 权限校验注解
 * @see ManageAuthPermissionAspect 权限校验切面
 */
public class ManageAuthPermissionException extends RuntimeException {

    /**
     * HTTP 响应状态码
     * <p>
     * 用于标识异常的 HTTP 状态码，通常为 401（未授权）。
     * 全局异常处理器会根据此状态码返回相应的响应给客户端。
     */
    private final int responseCode;

    /**
     * 构造函数：指定错误消息，使用默认状态码 401
     * <p>
     * 适用于标准的未授权场景。
     *
     * @param message 错误消息，描述权限失败的原因
     */
    public ManageAuthPermissionException(String message) {
        super(message);
        this.responseCode = HttpStatus.UNAUTHORIZED.value();
    }

    /**
     * 构造函数：指定错误消息和响应状态码
     * <p>
     * 适用于需要自定义状态码的场景，如使用 403（禁止访问）等。
     *
     * @param message     错误消息，描述权限失败的原因
     * @param responseCode HTTP 响应状态码
     */
    public ManageAuthPermissionException(String message, int responseCode) {
        super(message);
        this.responseCode = responseCode;
    }

    /**
     * 获取 HTTP 响应状态码
     *
     * @return HTTP 响应状态码
     */
    public int getResponseCode() {
        return responseCode;
    }
}
