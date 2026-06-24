package com.edianyun.codeagentjava.domain.model.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SessionId 值对象单元测试。
 * 验证标识符校验规则和 UUID 生成唯一性。
 */
class SessionIdTest {

    @Test
    void shouldCreateValidSessionId() {
        SessionId id = new SessionId("abc-123");
        assertThat(id.value()).isEqualTo("abc-123");
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new SessionId(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new SessionId(""));
        assertThrows(IllegalArgumentException.class, () -> new SessionId("   "));
    }

    @Test
    void shouldGenerateNonBlankId() {
        SessionId id = SessionId.generate();
        assertThat(id).isNotNull();
        assertThat(id.value()).isNotBlank();
    }

    @Test
    void shouldGenerateUniqueIds() {
        SessionId id1 = SessionId.generate();
        SessionId id2 = SessionId.generate();
        assertThat(id1.value()).isNotEqualTo(id2.value());
    }

    @Test
    void shouldSupportEqualsAndHashCode() {
        SessionId id1 = new SessionId("abc");
        SessionId id2 = new SessionId("abc");
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
