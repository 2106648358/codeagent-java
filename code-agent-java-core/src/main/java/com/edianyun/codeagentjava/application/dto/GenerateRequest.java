package com.edianyun.codeagentjava.application.dto;

import com.edianyun.codeagentjava.domain.model.content.ContentType;

import java.util.List;

/**
 * 生成请求 DTO，封装来自 CLI 的 generate 命令参数。
 * 包含需求描述、目标内容类型和指定的目标文件列表。
 */
public record GenerateRequest(String sessionId, String userId, String requirements, ContentType contentType, List<String> targetFiles) {

    public GenerateRequest {
        if (requirements == null || requirements.isBlank()) {
            throw new IllegalArgumentException("Requirements must not be blank");
        }
    }
}
