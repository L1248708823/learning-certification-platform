package com.learningplatform.common.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 全局异常处理器，统一把异常转成 ApiResponse 并保证 HTTP 状态码语义化
 * （200 / 400 / 401 / 403 / 404 / 409 / 500），契约见 docs/spec/0001 第 5 章。
 *
 * <p>优先级从上到下：业务异常最先匹配，参数校验第二，请求体解析失败第三，
 * 兜底处理器最后，保证任何未被业务代码处理的异常都有安全响应而不是默认错误页。
 *
 * <p>关于兜底处理器的 message：返回固定的「系统内部错误」，不把异常文本透传给
 * 前端（避免泄漏 SQL、堆栈等内部信息），完整堆栈只记在服务端日志。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：按错误码自带的状态码与提示返回。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), ex.getMessage()));
    }

    /**
     * 参数校验失败：请求体上的 @Valid 注解未通过时抛出，响应 400 且 data 带字段明细。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<FieldErrorInfo>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        List<FieldErrorInfo> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new FieldErrorInfo(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(),
                        ErrorCode.PARAM_INVALID.getDefaultMessage(), errors));
    }

    /**
     * 请求体解析失败：JSON 格式错误、字段类型对不上、缺请求体等，不等同于校验失败，
     * 但语义上都归「参数有问题」，统一 400 + 参数错误码。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID.getCode(), "请求体格式错误"));
    }

    /**
     * 兜底处理器：所有没被上面接住的异常都到这。记完整异常栈供排查，
     * 响应统一 500 + 安全 message。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("未处理的异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
