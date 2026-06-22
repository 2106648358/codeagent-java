package com.edianyun.codeagentjava.application.port;

import com.edianyun.codeagentjava.application.dto.GenerateRequest;
import com.edianyun.codeagentjava.application.dto.GenerateResponse;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator.StreamConsumer;

/**
 * 生成用例入站端口，定义内容生成功能的两种调用方式。
 */
public interface GenerateUseCase {

    GenerateResponse generate(GenerateRequest request);

    GenerateResponse generateStream(GenerateRequest request, StreamConsumer consumer);
}
