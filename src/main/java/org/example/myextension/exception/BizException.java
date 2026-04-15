package org.example.myextension.exception;


/**
 * 业务异常类
 * <p>
 * 用于封装业务逻辑中的异常情况，包含错误码和错误信息。
 * 继承自 RuntimeException，属于运行时异常，不需要强制捕获。
 * </p>
 * @see RuntimeException
 */
public class BizException extends RuntimeException {

    /**
     * 错误码
     * <p>
     * 用于标识具体的错误类型，便于前端或调用方进行相应的处理。
     * 常见的错误码包括：
     * <ul>
     *   <li>500 - 系统内部错误 (SYSTEM_FAILED)</li>
     *   <li>400 - 业务逻辑错误 (BUSINESS_FAILED)</li>
     * </ul>
     * </p>
     */
    private int code;

    /**
     * 构造业务异常（默认错误码为 500）
     * <p>
     * 当未指定错误码时，默认使用系统失败错误码 500。
     * 适用于一般的业务异常场景。
     * </p>
     *
     * @param message 异常描述信息，用于说明异常发生的原因
     */
    public BizException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造业务异常（指定错误码和消息）
     * <p>
     * 允许自定义错误码和错误信息，提供更精确的异常标识。
     * 适用于需要区分不同业务场景的异常情况。
     * </p>
     *
     * @param code    错误码，用于标识错误类型
     * @param message 异常描述信息，用于说明异常发生的原因
     */
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（指定错误码、消息和原始异常）
     * <p>
     * 保留原始异常信息，便于追踪异常的根本原因。
     * 适用于在捕获底层异常后，包装成业务异常向上抛出的场景。
     * </p>
     *
     * @param code    错误码，用于标识错误类型
     * @param message 异常描述信息，用于说明异常发生的原因
     * @param cause   原始异常对象，用于保留异常链信息
     */
    public BizException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置错误码
     *
     * @param code 错误码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 常量定义：系统失败错误码
     * <p>
     * 用于表示系统内部的错误或异常，通常是由于程序bug、资源不可用等原因导致。
     * HTTP 状态码对应 500 Internal Server Error。
     * </p>
     */
    public static final int SYSTEM_FAILED = 500;

    /**
     * 常量定义：业务失败错误码
     * <p>
     * 用于表示业务逻辑校验失败或业务规则不满足的情况，
     * 例如参数验证失败、权限不足、数据不存在等。
     * HTTP 状态码对应 400 Bad Request。
     * </p>
     */
    public static final int BUSINESS_FAILED = 400;
}
