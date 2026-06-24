package com.edianyun.codeagentjava.cli.command;

import com.edianyun.codeagentjava.application.dto.GenerateRequest;
import com.edianyun.codeagentjava.application.dto.GenerateResponse;
import com.edianyun.codeagentjava.application.port.GenerateUseCase;
import com.edianyun.codeagentjava.cli.output.TerminalRenderer;
import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.common.util.StringUtils;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.FileChange;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.infrastructure.config.CodeAgentProperties;
import org.springframework.shell.core.command.annotation.Command;
import org.springframework.shell.core.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * generate 命令处理器：根据需求生成文件内容。
 * 支持指定文件列表和目标内容类型，展示 diff 差异，
 * 并在写入前要求用户确认（除非指定 --yes）。
 */
@Component
public class GenerateCommand {

    private final GenerateUseCase generateUseCase;
    private final FileRepository fileRepository;
    private final TerminalRenderer renderer;
    private final CodeAgentProperties codeAgentProperties;

    public GenerateCommand(GenerateUseCase generateUseCase, FileRepository fileRepository,
                           TerminalRenderer renderer, CodeAgentProperties codeAgentProperties) {
        this.generateUseCase = generateUseCase;
        this.fileRepository = fileRepository;
        this.renderer = renderer;
        this.codeAgentProperties = codeAgentProperties;
    }

    @Command(name = "generate", description = "Generate content based on requirements")
    public String generate(
            @Option(longName = "requirements", shortName = 'r', required = true) String requirements,
            @Option(longName = "files", shortName = 'f', defaultValue = "") String[] targetFiles,
            @Option(longName = "type", shortName = 't', defaultValue = "CODE") ContentType contentType,
            @Option(longName = "yes", shortName = 'y', defaultValue = "false") boolean yes,
            @Option(longName = "session", shortName = 's', defaultValue = "") String sessionId,
            @Option(longName = "user", shortName = 'u', defaultValue = "") String userId) {

        List<String> files = targetFiles == null || targetFiles.length == 0 ? List.of() : Arrays.asList(targetFiles);
        GenerateRequest request = new GenerateRequest(StringUtils.blankToNull(sessionId), StringUtils.blankToNull(userId), requirements, contentType, files);

        GenerateResponse response;
        if (codeAgentProperties.getCli().isStreamOutput()) {
            response = generateUseCase.generateStream(request, new StreamingAgentOrchestrator.StreamConsumer() {
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
            response = generateUseCase.generate(request);
            renderer.renderFragments(response.fragments());
        }

        if (response.fragments().isEmpty()) {
            return "No content generated.";
        }

        boolean apply = yes;
        if (!apply && codeAgentProperties.getCli().isConfirmWrites()) {
            apply = renderer.confirm("Apply " + response.fragments().size() + " file change(s)?");
        }
        if (apply) {
            applyFragments(response.fragments());
            return "Applied " + response.fragments().size() + " file change(s). Task: " + response.taskId();
        }
        return "Generation completed but not applied. Task: " + response.taskId();
    }

    private void applyFragments(List<ContentFragment> fragments) {
        Path base = Paths.get(".").toAbsolutePath().normalize();
        for (ContentFragment fragment : fragments) {
            RelativePath path = fragment.path();
            Path file = path.toPath(base);
            String oldContent = "";
            try {
                if (Files.exists(file)) {
                    oldContent = Files.readString(file);
                }
            } catch (Exception e) {
                // ignore read errors, treat as new file
            }
            FileChange change = oldContent.isEmpty()
                    ? FileChange.create(path, fragment.content())
                    : FileChange.update(path, oldContent, fragment.content());
            renderer.renderDiff(change);
            fileRepository.apply(change, FileRepository.WriteOptions.defaults());
        }
    }

}
