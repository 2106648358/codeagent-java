package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
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
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
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

    /**
     * 构建 AgentScope HarnessAgent。
     * 通过编程式构建 Model 实例（而非字符串），将应用的 apiKey/baseUrl 配置
     * 显式注入到 AgentScope 模型中，避免依赖 AgentScope ModelRegistry 的
     * 内置工厂（后者只能从环境变量读取凭据，无法使用我们配置的 baseUrl）。
     * <p>
     * 这种编程式注入完全在 infrastructure 层完成，没有向 domain 层泄漏
     * 任何 AgentScope 框架依赖，符合六边形架构的分层原则。
     */
    private HarnessAgent buildAgent(CodeAgentProperties codeAgentProperties, LlmProperties llmProperties) {
        String modelId = llmProperties.getModel();
        Model model = createModel(modelId, llmProperties);
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

    /**
     * 根据模型 ID 解析并构建 AgentScope Model 实例。
     * <p>
     * 使用 switch 而非 provider 注册模式的原因：
     * - provider 种类少且稳定（openai、dashscope 两个），对应 AgentScope 的内置实现
     * - 每个 provider 的构建逻辑各不相同（apiKey 来源、baseUrl 必要性、额外参数），
     *   不适合统一抽象；强行引入工厂接口反而增加不必要的间接层
     * - 如需新增 provider，直接追加 case 即可，修改范围限制在本类内部，
     *   不影响上层调用者和 domain 层
     */
    private Model createModel(String modelId, LlmProperties llmProperties) {
        int colonIndex = modelId.indexOf(':');
        if (colonIndex <= 0) {
            throw new IllegalArgumentException("Model ID must be in format '<provider>:<model>', got: " + modelId);
        }
        String provider = modelId.substring(0, colonIndex).toLowerCase();
        String modelName = modelId.substring(colonIndex + 1);
        switch (provider) {
            case "openai":
                String apiKey = llmProperties.getOpenai().getApiKey();
                if (apiKey == null || apiKey.isBlank()) {
                    throw new IllegalArgumentException(
                            "OpenAI API key is not configured. Set codeagent.llm.openai.api-key in application-local.properties");
                }
                return OpenAIChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(llmProperties.getOpenai().getBaseUrl())
                        .modelName(modelName)
                        .stream(true)
                        .build();
            case "dashscope":
                String dashscopeApiKey = llmProperties.getDashscope().getApiKey();
                if (dashscopeApiKey == null || dashscopeApiKey.isBlank()) {
                    throw new IllegalArgumentException(
                            "DashScope API key is not configured. Set codeagent.llm.dashscope.api-key in application-local.properties");
                }
                return DashScopeChatModel.builder()
                        .apiKey(dashscopeApiKey)
                        .modelName(modelName)
                        .stream(true)
                        .build();
            default:
                throw new IllegalArgumentException("Unsupported model provider: '" + provider
                        + "'. Supported: openai, dashscope");
        }
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
