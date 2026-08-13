package com.learningplatform.iam.dict;

/**
 * 字典项对外视图。不含 enabled：查询接口只返回启用项，禁用项对调用方不存在。
 */
public record DictItemView(
        long id,
        String code,
        String label,
        int sort) {
}
