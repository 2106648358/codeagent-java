package com.edianyun.codeagentjava.application.port;

import com.edianyun.codeagentjava.application.dto.ChatRequest;
import com.edianyun.codeagentjava.application.dto.ChatResponse;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator.StreamConsumer;

/**
 * 聊天用例入站端口，定义聊天功能的两种调用方式：
 * - chat：阻塞式，等待完整回复
 * - chatStream：流式，逐 Token 输出
 */
public interface ChatUseCase {

    ChatResponse chat(ChatRequest request);

    ChatResponse chatStream(ChatRequest request, StreamConsumer consumer);
}
