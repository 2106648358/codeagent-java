package com.edianyun.codeagentjava.domain.service.impl;

import com.edianyun.codeagentjava.domain.model.content.GenerationTask;
import com.edianyun.codeagentjava.domain.model.message.Prompt;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.model.workspace.WorkspaceContext;
import com.edianyun.codeagentjava.domain.service.PromptBuilder;

import java.util.stream.Collectors;

/**
 * 默认 Prompt 构建器实现。
 * 根据不同的命令类型（生成/解释/对话）组装结构化 Prompt，
 * 包含工作区上下文、需求描述和目标类型等信息。
 */
public class DefaultPromptBuilder implements PromptBuilder {

    @Override
    public Prompt buildGenerationPrompt(GenerationTask task, WorkspaceContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful assistant that generates file content based on requirements.\n\n");
        sb.append("Requirements:\n").append(task.requirements()).append("\n\n");
        sb.append("Target content type: ").append(task.contentType()).append("\n\n");
        if (!context.files().isEmpty()) {
            sb.append("Workspace files:\n");
            for (RelativePath path : context.files()) {
                sb.append("- ").append(path).append("\n");
            }
            sb.append("\n");
        }
        if (!context.selectedFiles().isEmpty()) {
            sb.append("Selected files for context:\n");
            for (RelativePath path : context.selectedFiles()) {
                sb.append("- ").append(path).append("\n");
            }
            sb.append("\n");
        }
        if (context.dependencyHints() != null && !context.dependencyHints().isBlank()) {
            sb.append("Dependency hints:\n").append(context.dependencyHints()).append("\n\n");
        }
        sb.append("Please provide the generated content in Markdown code blocks, one per file, with the relative path as a comment or header. ");
        sb.append("Only output the requested content, no extra explanation.");
        return Prompt.system(sb.toString());
    }

    @Override
    public Prompt buildExplanationPrompt(String target, WorkspaceContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful assistant that explains file content or symbols.\n\n");
        sb.append("Target: ").append(target).append("\n\n");
        if (!context.files().isEmpty()) {
            sb.append("Workspace files:\n");
            sb.append(context.files().stream().map(RelativePath::toString).collect(Collectors.joining("\n")));
            sb.append("\n\n");
        }
        sb.append("Please explain the target in a clear and concise way. If the target is a file path, explain its purpose and structure.");
        return Prompt.system(sb.toString());
    }

    @Override
    public Prompt buildChatPrompt(String userInput, WorkspaceContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful coding assistant.\n");
        if (!context.files().isEmpty()) {
            sb.append("Current workspace files:\n");
            sb.append(context.files().stream().map(RelativePath::toString).collect(Collectors.joining("\n")));
            sb.append("\n\n");
        }
        sb.append("User: ").append(userInput);
        return Prompt.user(sb.toString());
    }
}
