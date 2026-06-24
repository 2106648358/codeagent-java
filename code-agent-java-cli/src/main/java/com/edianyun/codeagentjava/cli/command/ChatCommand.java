package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.ChatRequest;
import com.edianyun.codeagentjava.application.dto.ChatResponse;
import com.edianyun.codeagentjava.application.port.ChatUseCase;
import com.edianyun.codeagentjava.cli.output.TerminalRenderer;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.common.util.StringUtils;
import com.edianyun.codeagentjava.infrastructure.config.CodeAgentProperties;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * chat 命令处理器：与 AI 编程助手进行对话。
 * 支持流式输出（实时逐 Token 打印）和阻塞模式。
 */
@Component
public class ChatCommand {

    private final ChatUseCase chatUseCase;
    private final TerminalRenderer renderer;
    private final CodeAgentProperties codeAgentProperties;

    public ChatCommand(ChatUseCase chatUseCase, TerminalRenderer renderer, CodeAgentProperties codeAgentProperties) {
        this.chatUseCase = chatUseCase;
        this.renderer = renderer;
        this.codeAgentProperties = codeAgentProperties;
    }

    @Command(name = "chat", description = "Chat with the coding assistant")
    public String chat(@Option(longName = "prompt", shortName = 'p', required = true) String prompt,
                        @Option(longName = "session", shortName = 's', defaultValue = "") String sessionId,
                        @Option(longName = "user", shortName = 'u', defaultValue = "") String userId) {
        ChatRequest request = new ChatRequest(StringUtils.blankToNull(sessionId), StringUtils.blankToNull(userId), prompt);
        if (codeAgentProperties.getCli().isStreamOutput()) {
            chatUseCase.chatStream(request, new StreamingAgentOrchestrator.StreamConsumer() {
                @Override
                public void onToken(String token) {
                    renderer.printToken(token);
                }

                @Override
                public void onComplete(String text) {
                    renderer.flush();
                }

                @Override
                public void onError(Throwable error) {
                    renderer.flush();
                    throw new RuntimeException(error);
                }
            });
            return "";
        } else {
            ChatResponse response = chatUseCase.chat(request);
            return response.reply();
        }
    }

}
