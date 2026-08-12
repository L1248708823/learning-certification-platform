package com.learningplatform.iam.registration;

import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

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
    void verify_shouldConsumeCodeAndRejectReuse() {
        service.sendCode("13800138000");

        service.verifyAndConsume("13800138000", "123456");

        assertThatThrownBy(() -> service.verifyAndConsume("13800138000", "123456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码无效或已过期");
    }

    private static final class InMemoryVerificationCodeStore implements VerificationCodeStore {

        private final Map<String, String> savedCodes = new HashMap<>();
        private Duration savedCodeTtl;
        private final Map<String, Boolean> permits = new HashMap<>();

        @Override
        public boolean acquireSendPermit(String phone, Duration ttl) {
            if (permits.containsKey(phone)) {
                return false;
            }
            permits.put(phone, true);
            return true;
        }

        @Override
        public void saveCode(String phone, String code, Duration ttl) {
            savedCodes.put(phone, code);
            savedCodeTtl = ttl;
        }

        @Override
        public boolean consumeCode(String phone, String code) {
            if (!code.equals(savedCodes.get(phone))) {
                return false;
            }
            savedCodes.remove(phone);
            return true;
        }
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
