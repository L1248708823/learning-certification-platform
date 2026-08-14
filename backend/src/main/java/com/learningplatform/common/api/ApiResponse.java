package com.learningplatform.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一 API 响应体。所有接口都返回这个结构，契约见 docs/spec/0001 第 5 章。
 *
 * <p>三个字段：
 * <ul>
 *   <li>{@code code}：业务错误码，0 表示成功，其余取值见 {@link ErrorCode}。</li>
 *   <li>{@code message}：给人看的提示，成功固定 "OK"，失败是对应错误的说明。</li>
 *   <li>{@code data}：业务数据，失败时通常为 null。</li>
 * </ul>
 *
 * <p>用 record 承载，Java 21 自带构造、getter 与 equals/hashCode，不需要 Lombok。
 * 后续业务 controller 只返回这个类型，响应格式由全局异常处理器统一兜底，见
 * com.learningplatform.common.web.GlobalExceptionHandler。
 *
 * @param <T> 业务数据类型
 */
public record ApiResponse<T>(

        /** 业务结果码，0 表示成功，其他取值见 {@link ErrorCode}。 */
        @Schema(description = "业务结果码。0 表示成功，失败时取值见错误码定义。", example = "0")
        int code,

        /** 给调用方看的结果说明，成功时固定为 OK。 */
        @Schema(description = "结果说明。成功时固定为 OK，失败时是可展示的错误说明。", example = "OK")
        String message,

        /** 业务数据，失败时通常为 null，参数校验失败时为字段错误明细列表。 */
        @Schema(description = "业务数据。失败时通常为 null，参数校验失败时为字段错误明细列表。")
        T data) {

    /** 成功固定 code=0，message 固定 "OK"。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "OK", data);
    }

    /** 成功但不带业务数据。 */
    public static ApiResponse<Void> success() {
        return success(null);
    }

    /** 失败响应，data 为 null。 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return error(code, message, null);
    }

    /** 失败响应，可携带数据（例如参数校验错误的字段明细）。 */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
