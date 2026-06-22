package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.message.Prompt;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;

/**
 * Prompt 构建器服务接口，根据不同的命令类型和上下文组装 LLM Prompt。
 * 确保每个命令发送给 LLM 的提示信息清晰、结构化。
 */
public interface PromptBuilder {

    /** 构建内容生成的 Prompt（包含需求、文件列表等） */
    Prompt buildGenerationPrompt(GenerationTask task, WorkspaceContext context);

    /** 构建解释请求的 Prompt */
    Prompt buildExplanationPrompt(String target, WorkspaceContext context);

    /** 构建对话的 Prompt */
    Prompt buildChatPrompt(String userInput, WorkspaceContext context);
}
