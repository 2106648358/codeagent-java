package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;

/**
 * 流式 Agent 编排器端口，提供流式（非阻塞）调用接口。
 * 与 AgentOrchestrator 互补，用于需要实时输出 Token 的场景。
 */
public interface StreamingAgentOrchestrator {

    /** 流式对话，通过 consumer 回调逐 Token 输出 */
    void chatStream(Session session, Message userMessage, StreamConsumer consumer);

    /** 流式内容生成 */
    void generateStream(GenerationTask task, WorkspaceContext context, StreamConsumer consumer);

    /** 流式解释 */
    void explainStream(String target, WorkspaceContext context, StreamConsumer consumer);

    /** 流式消费回调接口 */
    interface StreamConsumer {

        /** 收到一个 Token 片段 */
        void onToken(String token);

        /** 流式输出完成 */
        void onComplete(String fullText);

        /** 流式输出出错 */
        void onError(Throwable error);
    }
}
