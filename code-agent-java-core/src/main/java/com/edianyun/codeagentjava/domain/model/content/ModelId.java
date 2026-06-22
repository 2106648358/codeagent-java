package com.edianyun.codeagentjava.domain.model.content;

/**
 * 模型 ID 值对象，标识一个具体的 LLM 模型。
 * 格式如 "openai:gpt-4.1" 或 "dashscope:qwen-plus"，
 * 由 Provider:ModelName 两部分组成。
 */
public record ModelId(String value) {

    public ModelId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Model id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
