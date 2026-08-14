package com.learningplatform.common.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 参数校验错误明细。参数校验失败时，ApiResponse 的 data 字段放这个列表，
 * 前端可以逐字段展示错误，不用从整段 message 里解析。
 *
 * @param field   出错的字段名（与请求体字段名一致）
 * @param message 该字段的错误说明
 */
public record FieldErrorInfo(

        /** 出错的请求字段名。 */
        @Schema(description = "出错的请求字段名，与请求体字段名一致。", example = "phone")
        String field,

        /** 该字段不符合约束的原因。 */
        @Schema(description = "该字段不符合约束的原因。", example = "手机号格式不正确")
        String message) {
}
