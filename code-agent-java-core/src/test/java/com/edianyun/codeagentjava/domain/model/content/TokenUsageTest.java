package com.edianyun.codeagentjava.domain.model.content;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TokenUsage 值对象单元测试。
 * 验证 Token 数量校验和 totalTokens 计算。
 */
class TokenUsageTest {

    @Test
    void shouldCreateValidTokenUsage() {
        TokenUsage usage = new TokenUsage(100, 50);
        assertThat(usage.inputTokens()).isEqualTo(100);
        assertThat(usage.outputTokens()).isEqualTo(50);
    }

    @Test
    void shouldCalculateTotalTokens() {
        TokenUsage usage = new TokenUsage(200, 300);
        assertThat(usage.totalTokens()).isEqualTo(500);
    }

    @Test
    void shouldRejectNegativeInputTokens() {
        assertThrows(IllegalArgumentException.class, () -> new TokenUsage(-1, 10));
    }

    @Test
    void shouldRejectNegativeOutputTokens() {
        assertThrows(IllegalArgumentException.class, () -> new TokenUsage(10, -1));
    }

    @Test
    void shouldAcceptZeroTokens() {
        TokenUsage usage = new TokenUsage(0, 0);
        assertThat(usage.totalTokens()).isZero();
    }

    @Test
    void shouldSupportEquality() {
        assertThat(new TokenUsage(1, 2)).isEqualTo(new TokenUsage(1, 2));
        assertThat(new TokenUsage(1, 2)).isNotEqualTo(new TokenUsage(1, 3));
    }
}
