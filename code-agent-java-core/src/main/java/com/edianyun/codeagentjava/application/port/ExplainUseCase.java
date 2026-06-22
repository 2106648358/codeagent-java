package com.edianyun.codeagentjava.application.port;

import com.edianyun.codeagentjava.application.dto.ExplainRequest;
import com.edianyun.codeagentjava.application.dto.ExplainResponse;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator.StreamConsumer;

/**
 * 解释用例入站端口，定义解释功能的两种调用方式。
 */
public interface ExplainUseCase {

    ExplainResponse explain(ExplainRequest request);

    ExplainResponse explainStream(ExplainRequest request, StreamConsumer consumer);
}
