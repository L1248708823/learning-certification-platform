package com.learningplatform.iam.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验中国大陆手机号的格式。
 *
 * <p>本约束只负责格式，不负责确认号码真实存在或验证号码归属。需要允许空值时，
 * 与 {@code @NotBlank} 分开使用；本约束会把空值交给 {@code @NotBlank} 处理。
 */
@Documented
@Constraint(validatedBy = MainlandMobilePhoneValidator.class)
@Target({
        ElementType.FIELD,
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.ANNOTATION_TYPE,
        ElementType.RECORD_COMPONENT
})
@Retention(RetentionPolicy.RUNTIME)
public @interface MainlandMobilePhone {

    /** 校验失败时返回的提示信息。 */
    String message() default "手机号格式不正确";

    /** Bean Validation 分组，用于按场景启用不同校验规则。 */
    Class<?>[] groups() default {};

    /** Bean Validation 扩展载荷，当前项目不使用。 */
    Class<? extends Payload>[] payload() default {};
}
