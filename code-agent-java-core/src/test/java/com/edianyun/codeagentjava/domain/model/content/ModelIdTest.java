package com.edianyun.codeagentjava.domain.model.content;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ModelId 值对象单元测试。
 * 验证模型标识符校验规则。
 */
class ModelIdTest {

    @Test
    void shouldCreateValidModelId() {
        ModelId id = new ModelId("openai:gpt-4.1");
        assertThat(id.value()).isEqualTo("openai:gpt-4.1");
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new ModelId(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new ModelId(""));
        assertThrows(IllegalArgumentException.class, () -> new ModelId("  "));
    }

    @Test
    void shouldSupportEquality() {
        assertThat(new ModelId("openai:gpt-4.1")).isEqualTo(new ModelId("openai:gpt-4.1"));
        assertThat(new ModelId("a")).isNotEqualTo(new ModelId("b"));
    }
}
