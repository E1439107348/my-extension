package org.example.myextension.model.vo;

import java.io.Serializable;
import java.time.Instant;

/**
 * 统一接口返回结果对象
 * <p>
 * 封装 API 接口的统一响应格式，所有接口的返回结果都应使用此对象包装。
 * 此类采用泛型设计，可以包装任意类型的响应数据。
 * <p>
 * <b>响应格式：</b>
 * <pre>
 * {
 *   "code": 200,           // 响应码
 *   "success": true,        // 是否成功
 *   "message": "成功",      // 响应消息
 *   "timestamp": 1234567890 // 时间戳（毫秒）
 *   "data": { ... }        // 响应数据
 * }
 * </pre>
 * <p>
 * <b>响应码说明：</b>
 * <ul>
 *   <li>{@code 200}: 请求成功</li>
 *   <li>{@code 400}: 请求参数错误</li>
 *   <li>{@code 401}: 未授权</li>
 *   <li>{@code 403}: 禁止访问</li>
 *   <li>{@code 404}: 资源不存在</li>
 *   <li>{@code 500}: 服务器内部错误</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>
 * // 成功返回带数据
 * ApiResult&lt;Item&gt; result = ApiResult.success(item);
 *
 * // 成功返回无数据
 * ApiResult&lt;Void&gt; result = ApiResult.success();
 *
 * // 失败返回（默认错误码）
 * ApiResult&lt;Void&gt; result = ApiResult.fail();
 *
 * // 失败返回（自定义错误码和消息）
 * ApiResult&lt;Void&gt; result = ApiResult.fail(400, "参数错误");
 *
 * // 失败返回（自定义错误消息）
 * ApiResult&lt;Void&gt; result = ApiResult.fail("商品不存在");
 * </pre>
 *
 * @param <T> 响应数据的泛型类型
 */
public class ApiResult<T> implements Serializable {
    /**
     * 响应码
     * <p>
     * 用于标识请求的处理结果，通常使用 HTTP 状态码。
     * 常见值：
     * <ul>
     *   <li>200：请求成功</li>
     *   <li>400：请求参数错误</li>
     *   <li>500：服务器内部错误</li>
     * </ul>
     */
    private Integer code;

    /**
     * 是否成功
     * <p>
     * 布尔值，true 表示请求成功，false 表示请求失败。
     * 前端可根据此字段快速判断请求是否成功。
     */
    private Boolean success;

    /**
     * 响应消息
     * <p>
     * 用于描述请求结果的详细信息，如"成功"、"参数错误"、"商品不存在"等。
     * 成功时通常为"成功"，失败时为具体的错误信息。
     */
    private String message;

    /**
     * 时间戳
     * <p>
     * 服务器处理请求时的时间戳（毫秒），使用 Unix 时间戳格式。
     * 可用于同步客户端时间、排查请求顺序等场景。
     */
    private Long timestamp;

    /**
     * 响应数据
     * <p>
     * 实际的业务数据，泛型类型 T 由调用方指定。
     * 成功时包含数据，失败时为 null。
     */
    private T data;

    /**
     * 无参构造函数
     */
    public ApiResult() {}

    /**
     * 全参构造函数
     *
     * @param code      响应码
     * @param success   是否成功
     * @param message   响应消息
     * @param timestamp 时间戳（毫秒）
     * @param data      响应数据
     */
    public ApiResult(Integer code, Boolean success, String message, Long timestamp, T data) {
        this.code = code;
        this.success = success;
        this.message = message;
        this.timestamp = timestamp;
        this.data = data;
    }

    // ==================== 静态工厂方法：成功返回 ====================

    /**
     * 成功返回（带数据）
     * <p>
     * 创建一个表示成功响应的 ApiResult 对象，包含指定的数据。
     *
     * @param data 响应数据，可以为 null
     * @param <T>  数据类型
     * @return 成功的 ApiResult 对象，code=200，success=true，message="成功"
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, true, "成功", Instant.now().toEpochMilli(), data);
    }

    /**
     * 成功返回（无数据）
     * <p>
     * 创建一个表示成功响应的 ApiResult 对象，不包含数据。
     * 适用于只需确认操作成功，无需返回数据的场景。
     *
     * @param <T> 数据类型
     * @return 成功的 ApiResult 对象，code=200，success=true，message="成功"，data=null
     */
    public static <T> ApiResult<T> success() {
        return success(null);
    }

    // ==================== 静态工厂方法：失败返回 ====================

    /**
     * 失败返回（默认错误码）
     * <p>
     * 创建一个表示失败响应的 ApiResult 对象，使用默认错误码 500 和默认错误消息。
     * 适用于系统异常、未知错误等通用失败场景。
     *
     * @param <T> 数据类型
     * @return 失败的 ApiResult 对象，code=500，success=false，message="系统异常，请稍后再试"，data=null
     */
    public static <T> ApiResult<T> fail() {
        return new ApiResult<>(500, false, "系统异常，请稍后再试", Instant.now().toEpochMilli(), null);
    }

    /**
     * 失败返回（自定义错误码和消息）
     * <p>
     * 创建一个表示失败响应的 ApiResult 对象，使用指定的错误码和错误消息。
     * 适用于需要返回自定义错误的场景。
     *
     * @param code    错误码，通常使用 HTTP 状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败的 ApiResult 对象，success=false，data=null
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, false, message, Instant.now().toEpochMilli(), null);
    }

    /**
     * 失败返回（自定义错误消息）
     * <p>
     * 创建一个表示失败响应的 ApiResult 对象，使用默认错误码 500 和指定的错误消息。
     * 适用于已知错误原因，需要明确告知前端错误的场景。
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 失败的 ApiResult 对象，code=500，success=false，data=null
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, false, message, Instant.now().toEpochMilli(), null);
    }

    // ==================== Getter and Setter ====================

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
