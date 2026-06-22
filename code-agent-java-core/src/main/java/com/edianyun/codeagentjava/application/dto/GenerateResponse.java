package com.edianyun.codeagentjava.application.dto;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;

import java.util.List;

/**
 * 生成响应 DTO，包含生成任务 ID 和产出的内容片段列表。
 */
public record GenerateResponse(String taskId, String sessionId, List<ContentFragment> fragments) {

    public GenerateResponse {
        if (fragments == null) {
            throw new IllegalArgumentException("Fragments must not be null");
        }
    }
}
