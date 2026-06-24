package com.edianyun.codeagentjava.domain.model.message;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prompt 值对象单元测试。
 * 验证角色和内容的校验规则以及工厂方法。
 */
class PromptTest {

    @Test
    void shouldCreateUserPrompt() {
        Prompt p = Prompt.user("Hello");
        assertThat(p.role()).isEqualTo("user");
        assertThat(p.content()).isEqualTo("Hello");
    }

    @Test
    void shouldCreateSystemPrompt() {
        Prompt p = Prompt.system("You are a helpful assistant");
        assertThat(p.role()).isEqualTo("system");
        assertThat(p.content()).isEqualTo("You are a helpful assistant");
    }

    @Test
    void shouldCreateAssistantPrompt() {
        Prompt p = Prompt.assistant("The answer is 42");
        assertThat(p.role()).isEqualTo("assistant");
        assertThat(p.content()).isEqualTo("The answer is 42");
    }

    @Test
    void shouldRejectBlankRole() {
        assertThrows(IllegalArgumentException.class, () -> new Prompt("", "content"));
        assertThrows(IllegalArgumentException.class, () -> new Prompt("  ", "content"));
        assertThrows(IllegalArgumentException.class, () -> new Prompt(null, "content"));
    }

    @Test
    void shouldRejectNullContent() {
        assertThrows(IllegalArgumentException.class, () -> new Prompt("user", null));
    }

    @Test
    void shouldAcceptEmptyContent() {
        Prompt p = new Prompt("user", "");
        assertThat(p.content()).isEmpty();
    }

    @Test
    void shouldSupportEquality() {
        assertThat(Prompt.user("hi")).isEqualTo(Prompt.user("hi"));
        assertThat(Prompt.user("hi")).isNotEqualTo(Prompt.system("hi"));
    }
}
