package com.learningplatform.common.exception;

import com.learningplatform.common.api.ErrorCode;

/**
 * 业务异常。业务代码在规则不满足时抛出，携带错误码，由全局异常处理器
 * （com.learningplatform.common.web.GlobalExceptionHandler）转成 ApiResponse 和对应
 * HTTP 状态码，业务层不需要自己拼响应。
 *
 * <p>两个构造的区别只在提示文案：带 message 的用于需要给前端更具体说明的场景，
 * 错误码和 HTTP 状态始终由 {@link ErrorCode} 决定。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** 使用错误码自带的默认提示文案。 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /** 错误码固定，但覆盖默认提示文案。 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
