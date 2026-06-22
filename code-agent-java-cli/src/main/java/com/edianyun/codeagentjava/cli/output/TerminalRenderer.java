package com.edianyun.codeagentjava.cli.output;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.FileChange;
import com.edianyun.codeagentjava.domain.service.FileDiffService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

/**
 * 终端渲染器，负责 CLI 的输出展示和交互确认。
 * 提供流式 Token 输出、diff 展示、内容片段渲染和用户确认对话框。
 */
@Component
public class TerminalRenderer {

    private final FileDiffService fileDiffService;

    public TerminalRenderer(FileDiffService fileDiffService) {
        this.fileDiffService = fileDiffService;
    }

    public void printToken(String token) {
        System.out.print(token);
    }

    public void flush() {
        System.out.println();
    }

    public void renderDiff(FileChange change) {
        System.out.println("=== " + change.path() + " ===");
        System.out.println(fileDiffService.diff(change.oldContent(), change.newContent()));
    }

    public void renderFragments(List<ContentFragment> fragments) {
        for (ContentFragment fragment : fragments) {
            System.out.println("--- " + fragment.path() + " ---");
            System.out.println(fragment.content());
            System.out.println();
        }
    }

    public boolean confirm(String message) {
        System.out.print(message + " [y/N] ");
        System.out.flush();
        Scanner scanner = new Scanner(System.in);
        String input = scanner.nextLine().trim().toLowerCase();
        return input.equals("y") || input.equals("yes");
    }
}
