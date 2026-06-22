package com.edianyun.codeagentjava.application.dto;

/**
 * 解释响应 DTO，包含被解释的目标和 LLM 返回的解释文本。
 */
public record ExplainResponse(String target, String explanation) {

    public ExplainResponse {
        if (explanation == null) {
            throw new IllegalArgumentException("Explanation must not be null");
        }
    }
}
