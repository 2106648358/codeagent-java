package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;

/**
 * Agent 编排器端口（出站端口），定义 Agent/LLM 调用的阻塞接口。
 * 领域层定义此接口，基础设施层（如 AgentScopeOrchestrator）实现。
 * 将 LLM 调用完全隔离在领域层之外。
 */
public interface AgentOrchestrator {

    /** 执行一轮对话，返回助手的回复消息 */
    Message chat(Session session, Message userMessage);

    /** 执行内容生成任务，返回完成后的 GenerationTask（含片段） */
    GenerationTask generate(GenerationTask task, WorkspaceContext context);

    /** 解释指定目标（文件/符号），返回解释文本 */
    String explain(String target, WorkspaceContext context);
}
