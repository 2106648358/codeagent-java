package com.edianyun.codeagentjava.domain.model.llm;

import com.edianyun.codeagentjava.domain.model.content.ModelId;

/**
 * LLM 调用选项值对象，封装模型调用时的参数配置。
 * 包括模型 ID、温度、最大 Token 数和超时时间等。
 */
public record LlmOptions(ModelId modelId, Double temperature, Integer maxTokens, Long timeoutSeconds) {

    /** 使用默认参数创建 LLM 调用选项 */
    public static LlmOptions defaults(ModelId modelId) {
        return new LlmOptions(modelId, 0.2, 4096, 60L);
    }

    /** 切换使用的模型，保留其他参数不变 */
    public LlmOptions withModel(ModelId modelId) {
        return new LlmOptions(modelId, temperature, maxTokens, timeoutSeconds);
    }
}
