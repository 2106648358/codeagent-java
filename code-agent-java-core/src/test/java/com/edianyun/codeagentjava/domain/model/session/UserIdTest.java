package com.edianyun.codeagentjava.domain.model.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UserId 值对象单元测试。
 * 验证用户标识符校验和匿名用户工厂方法。
 */
class UserIdTest {

    @Test
    void shouldCreateValidUserId() {
        UserId id = new UserId("developer-1");
        assertThat(id.value()).isEqualTo("developer-1");
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new UserId(""));
        assertThrows(IllegalArgumentException.class, () -> new UserId("  "));
    }

    @Test
    void shouldReturnAnonymousId() {
        UserId id = UserId.anonymous();
        assertThat(id.value()).isEqualTo("anonymous");
    }

    @Test
    void shouldSupportEquality() {
        assertThat(new UserId("u1")).isEqualTo(new UserId("u1"));
        assertThat(new UserId("u1")).isNotEqualTo(new UserId("u2"));
    }
}
