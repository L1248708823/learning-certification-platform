package com.learningplatform.common.web;

/**
 * 参数校验错误明细。参数校验失败时，ApiResponse 的 data 字段放这个列表，
 * 前端可以逐字段展示错误，不用从整段 message 里解析。
 *
 * @param field   出错的字段名（与请求体字段名一致）
 * @param message 该字段的错误说明
 */
public record FieldErrorInfo(String field, String message) {
}
