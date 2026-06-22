package com.edianyun.codeagentjava.domain.repository;

import com.edianyun.codeagentjava.domain.model.llm.LlmOptions;
import com.edianyun.codeagentjava.domain.model.message.Prompt;

import java.util.List;

/**
 * LLM Provider 端口（出站端口），定义对大语言模型调用的抽象。
 * 不同的 LLM 实现（OpenAI、DashScope 等）通过此接口适配。
 */
public interface LlmProvider {

    /** 发送 prompt 列表给 LLM，返回生成的文本 */
    String generate(List<Prompt> prompts, LlmOptions options);
}
