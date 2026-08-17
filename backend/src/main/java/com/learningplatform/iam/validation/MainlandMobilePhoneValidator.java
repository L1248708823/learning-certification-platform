package com.learningplatform.iam.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * {@link MainlandMobilePhone} 的校验器。
 *
 * <p>空值和空白值交给同字段上的 {@code @NotBlank} 处理，避免一次请求产生两条相同原因的错误。
 */
public class MainlandMobilePhoneValidator
        implements ConstraintValidator<MainlandMobilePhone, String> {

    /** 当前规则接受 1 开头、第二位为 3 到 9 的十一位大陆手机号。 */
    private static final Pattern MAINLAND_MOBILE_PATTERN =
            Pattern.compile("^1[3-9][0-9]{9}$");

    /**
     * 校验手机号格式。
     *
     * @param value 待校验的手机号
     * @param context Bean Validation 校验上下文，当前规则不需要修改默认消息
     * @return 值为空或符合大陆手机号格式时返回 {@code true}
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null
                || value.isBlank()
                || MAINLAND_MOBILE_PATTERN.matcher(value).matches();
    }
}
