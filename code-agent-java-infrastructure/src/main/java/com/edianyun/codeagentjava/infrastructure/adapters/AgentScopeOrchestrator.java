package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Message;
import com.edianyun.codeagentjava.domain.model.message.Prompt;
import com.edianyun.codeagentjava.domain.model.session.Session;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.service.ContentFragmentExtractor;
import com.edianyun.codeagentjava.domain.service.PromptBuilder;
import com.edianyun.codeagentjava.infrastructure.config.CodeAgentProperties;
import com.edianyun.codeagentjava.infrastructure.config.LlmProperties;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * AgentScope 编排器适配器，同时实现 AgentOrchestrator 和 StreamingAgentOrchestrator。
 * 封装 AgentScope HarnessAgent，将领域模型对象转换为 AgentScope 的内部消息格式。
 * 延迟构建 HarnessAgent，避免启动时强依赖 API Key。
 */
public class AgentScopeOrchestrator implements AgentOrchestrator, StreamingAgentOrchestrator {

    private HarnessAgent agent;
    private final CodeAgentProperties codeAgentProperties;
    private final LlmProperties llmProperties;
    private final PromptBuilder promptBuilder;
    private final ContentFragmentExtractor contentFragmentExtractor;

    public AgentScopeOrchestrator(CodeAgentProperties codeAgentProperties,
                                  LlmProperties llmProperties,
                                  PromptBuilder promptBuilder,
                                  ContentFragmentExtractor contentFragmentExtractor) {
        this.codeAgentProperties = codeAgentProperties;
        this.llmProperties = llmProperties;
        this.promptBuilder = promptBuilder;
        this.contentFragmentExtractor = contentFragmentExtractor;
    }

    private synchronized HarnessAgent getAgent() {
        if (agent == null) {
            agent = buildAgent(codeAgentProperties, llmProperties);
        }
        return agent;
    }

    private HarnessAgent buildAgent(CodeAgentProperties codeAgentProperties, LlmProperties llmProperties) {
        String model = llmProperties.getModel();
        Path workspace = Paths.get(codeAgentProperties.getAgent().getWorkspace()).toAbsolutePath().normalize();
        CodeAgentProperties.AgentProperties.CompactionProperties compaction = codeAgentProperties.getAgent().getCompaction();
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("code-agent")
                .sysPrompt("You are a helpful coding assistant that operates on local files.")
                .model(model)
                .workspace(workspace);
        if (compaction != null && compaction.isEnabled()) {
            builder.compaction(CompactionConfig.builder()
                    .triggerMessages(compaction.getTriggerMessages())
                    .keepMessages(compaction.getKeepMessages())
                    .build());
        }
        return builder.build();
    }

    @Override
    public Message chat(Session session, Message userMessage) {
        RuntimeContext ctx = buildRuntimeContext(session);
        Msg response = getAgent().call(new UserMessage(userMessage.content()), ctx).block();
        return Message.assistant(response.getTextContent(), null, null);
    }

    @Override
    public GenerationTask generate(GenerationTask task, WorkspaceContext context) {
        Prompt prompt = promptBuilder.buildGenerationPrompt(task, context);
        RuntimeContext ctx = buildRuntimeContext(task.sessionId().value(), null);
        Msg response = getAgent().call(new UserMessage(prompt.content()), ctx).block();
        List<ContentFragment> fragments = contentFragmentExtractor.extract(response.getTextContent(), task.contentType());
        task.addFragments(fragments);
        task.complete();
        return task;
    }

    @Override
    public String explain(String target, WorkspaceContext context) {
        Prompt prompt = promptBuilder.buildExplanationPrompt(target, context);
        RuntimeContext ctx = buildRuntimeContext(null, null);
        Msg response = getAgent().call(new UserMessage(prompt.content()), ctx).block();
        return response.getTextContent();
    }

    @Override
    public void chatStream(Session session, Message userMessage, StreamConsumer consumer) {
        RuntimeContext ctx = buildRuntimeContext(session);
        runStream(getAgent().streamEvents(new UserMessage(userMessage.content()), ctx), consumer);
    }

    @Override
    public void generateStream(GenerationTask task, WorkspaceContext context, StreamConsumer consumer) {
        Prompt prompt = promptBuilder.buildGenerationPrompt(task, context);
        RuntimeContext ctx = buildRuntimeContext(task.sessionId().value(), null);
        runStream(getAgent().streamEvents(new UserMessage(prompt.content()), ctx), consumer);
    }

    @Override
    public void explainStream(String target, WorkspaceContext context, StreamConsumer consumer) {
        Prompt prompt = promptBuilder.buildExplanationPrompt(target, context);
        RuntimeContext ctx = buildRuntimeContext(null, null);
        runStream(getAgent().streamEvents(new UserMessage(prompt.content()), ctx), consumer);
    }

    private void runStream(Flux<AgentEvent> events, StreamConsumer consumer) {
        StringBuilder fullText = new StringBuilder();
        try {
            events.subscribeOn(Schedulers.boundedElastic())
                    .doOnNext(event -> {
                        if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                            String delta = ((TextBlockDeltaEvent) event).getDelta();
                            fullText.append(delta);
                            consumer.onToken(delta);
                        }
                    })
                    .blockLast();
            consumer.onComplete(fullText.toString());
        } catch (Exception e) {
            consumer.onError(e);
        }
    }

    private RuntimeContext buildRuntimeContext(Session session) {
        return buildRuntimeContext(session.id().value(), session.userId().value());
    }

    private RuntimeContext buildRuntimeContext(String sessionId, String userId) {
        RuntimeContext.Builder builder = RuntimeContext.builder();
        if (sessionId != null) {
            builder.sessionId(sessionId);
        }
        if (userId != null) {
            builder.userId(userId);
        }
        return builder.build();
    }
}
