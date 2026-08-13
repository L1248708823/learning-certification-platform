package com.learningplatform.iam.registration;

import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationCodeServiceTest {

    private final InMemoryVerificationCodeStore store = new InMemoryVerificationCodeStore();
    private final RecordingSmsSender sender = new RecordingSmsSender();
    private final VerificationCodeService service = new VerificationCodeService(
            store,
            sender,
            () -> "123456"
    );

    @Test
    void sendCode_shouldStoreFiveMinutesAndSendMockCode() {
        service.sendCode("13800138000");

        assertThat(store.savedCodes).containsEntry("13800138000", "123456");
        assertThat(store.savedCodeTtl).isEqualTo(Duration.ofMinutes(5));
        assertThat(sender.phone).isEqualTo("13800138000");
        assertThat(sender.code).isEqualTo("123456");
    }

    @Test
    void sendCode_twiceWithinSixtySeconds_shouldBeRejected() {
        service.sendCode("13800138000");

        assertThatThrownBy(() -> service.sendCode("13800138000"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送过于频繁");
    }

    @Test
    void assertValid_shouldNotConsumeCode() {
        service.sendCode("13800138000");

        service.assertValid("13800138000", "123456");
        service.assertValid("13800138000", "123456");
        service.consume("13800138000", "123456");
    }

    @Test
    void consume_shouldRejectReuse() {
        service.sendCode("13800138000");

        service.consume("13800138000", "123456");

        assertThatThrownBy(() -> service.consume("13800138000", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码无效或已过期");
    }

    private static final class RecordingSmsSender implements SmsSender {

        private String phone;
        private String code;

        @Override
        public void send(String phone, String code) {
            this.phone = phone;
            this.code = code;
        }
    }
}
