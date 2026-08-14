package com.learningplatform.iam.dict;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字典项对外视图。不含 enabled：查询接口只返回启用项，禁用项对调用方不存在。
 */
public record DictItemView(

        /** 供客户端识别该字典项的公开标识。 */
        @Schema(description = "供客户端识别该字典项的公开标识。", example = "1")
        long id,

        /** 类型内唯一的机器可读代码。 */
        @Schema(description = "类型内唯一的机器可读代码。", example = "FRONTEND")
        String code,

        /** 展示给学习者或调用方的中文标签。 */
        @Schema(description = "展示给学习者或调用方的中文标签。", example = "前端开发")
        String label,

        /** 同一类型内的升序展示顺序。 */
        @Schema(description = "同一类型内的升序展示顺序。", example = "10")
        int sort) {
}
