package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.ExplainRequest;
import com.edianyun.codeagentjava.application.dto.ExplainResponse;
import com.edianyun.codeagentjava.application.port.ExplainUseCase;
import com.edianyun.codeagentjava.cli.output.TerminalRenderer;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.infrastructure.config.CodeAgentProperties;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

/**
 * explain 命令处理器：解释指定文件或符号的用途和结构。
 * 支持流式输出，可选指定作用域范围。
 */
@Component
public class ExplainCommand {

    private final ExplainUseCase explainUseCase;
    private final TerminalRenderer renderer;
    private final CodeAgentProperties codeAgentProperties;

    public ExplainCommand(ExplainUseCase explainUseCase, TerminalRenderer renderer, CodeAgentProperties codeAgentProperties) {
        this.explainUseCase = explainUseCase;
        this.renderer = renderer;
        this.codeAgentProperties = codeAgentProperties;
    }

    @Command(name = "explain", description = "Explain a file or symbol")
    public String explain(
            @Option(longName = "target", shortName = 't', required = true) String target,
            @Option(longName = "scope", shortName = 'c', defaultValue = "") String scope,
            @Option(longName = "session", shortName = 's', defaultValue = "") String sessionId,
            @Option(longName = "user", shortName = 'u', defaultValue = "") String userId) {

        ExplainRequest request = new ExplainRequest(blankToNull(sessionId), blankToNull(userId), target, scope);

        ExplainResponse response;
        if (codeAgentProperties.getCli().isStreamOutput()) {
            response = explainUseCase.explainStream(request, new StreamingAgentOrchestrator.StreamConsumer() {
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
        } else {
            response = explainUseCase.explain(request);
        }
        return codeAgentProperties.getCli().isStreamOutput() ? "" : response.explanation();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
