package com.edianyun.codeagentjava.application.dto;

/**
 * 解释请求 DTO，封装来自 CLI 的 explain 命令参数。
 * target 是要解释的文件路径或符号名，scope 是可选的作用域范围。
 */
public record ExplainRequest(String sessionId, String userId, String target, String scope) {

    public ExplainRequest {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Target must not be blank");
        }
    }
}
