package com.learningplatform.common.api;

import org.springframework.http.HttpStatus;

/**
 * 业务错误码定义，4 位分段：1xxx 通用、2xxx IAM、3xxx Content、4xxx Learning。
 * 分段规则见 docs/spec/0001 第 5 章。
 *
 * <p>本票据只落地通用段 1xxx，每个错误码同时声明它对应的 HTTP 状态码，
 * 语义化映射由 GlobalExceptionHandler 统一使用。业务段错误码由后续模块票据
 * （首期-03 IAM、首期-04 Content、首期-05 起 Learning）各自补充，做法相同：
 * 在枚举里加常量，附 HTTP 状态与默认提示。
 */
public enum ErrorCode {

    /** 参数校验失败（@Valid 未通过、请求体格式错误等）。 */
    PARAM_INVALID(1001, HttpStatus.BAD_REQUEST, "参数校验失败"),

    /** 请求的资源不存在，例如按 id 查课程未命中。 */
    RESOURCE_NOT_FOUND(1002, HttpStatus.NOT_FOUND, "资源不存在"),

    /** 请求与当前业务状态冲突，例如重复报名、状态流转不合法。 */
    STATE_CONFLICT(1003, HttpStatus.CONFLICT, "请求与当前状态冲突"),

    /** 未认证，例如 token 缺失或已失效（登出后走 jti 黑名单）。 */
    UNAUTHORIZED(1004, HttpStatus.UNAUTHORIZED, "未认证"),

    /** 已认证但无权限，对应规格的轻量 RBAC hasRole 控制。 */
    FORBIDDEN(1005, HttpStatus.FORBIDDEN, "无权限"),

    /** 同一手机号在发送间隔内重复请求验证码。 */
    SMS_TOO_FREQUENT(2001, HttpStatus.CONFLICT, "验证码发送过于频繁"),

    /** 注册验证码不存在、错误或已经被消费。 */
    SMS_CODE_INVALID(2002, HttpStatus.BAD_REQUEST, "验证码无效或已过期"),

    /** 手机号或用户名已经注册。 */
    USER_EXISTS(2003, HttpStatus.CONFLICT, "用户已存在"),

    /** 用户不存在或已经被停用。 */
    USER_NOT_AVAILABLE(2004, HttpStatus.UNAUTHORIZED, "用户不可用"),

    /** 未预期的系统内部错误，兜底处理器使用，响应 message 不泄漏内部细节。 */
    INTERNAL_ERROR(1999, HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final int code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /** 四位业务错误码。 */
    public int getCode() {
        return code;
    }

    /** 该错误语义对应的 HTTP 状态码，保证 200/400/401/403/404/409/500 语义化。 */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /** 默认提示文案，业务需要更具体时可覆盖。 */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
