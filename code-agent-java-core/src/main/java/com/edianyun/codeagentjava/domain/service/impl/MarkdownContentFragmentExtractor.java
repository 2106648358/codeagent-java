package com.edianyun.codeagentjava.domain.service.impl;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import com.edianyun.codeagentjava.domain.service.ContentFragmentExtractor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown 内容片段提取器实现。
 * 从 LLM 返回的 Markdown 文本中解析代码块（``` 包裹），
 * 自动识别文件路径和语言类型，生成结构化的 ContentFragment。
 */
public class MarkdownContentFragmentExtractor implements ContentFragmentExtractor {

    private static final Pattern CODE_BLOCK = Pattern.compile("```\\s*([^\\n]*)\\n(.*?)\\n```", Pattern.DOTALL);
    private static final Pattern PATH_HINT = Pattern.compile("(?:^|[^\\w])((?:[a-zA-Z0-9._-]+/)+[a-zA-Z0-9._-]+)");

    @Override
    public List<ContentFragment> extract(String rawText, ContentType defaultType) {
        List<ContentFragment> fragments = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return fragments;
        }
        Matcher matcher = CODE_BLOCK.matcher(rawText);
        while (matcher.find()) {
            String info = matcher.group(1).trim();
            String content = matcher.group(2);
            RelativePath path = extractPath(info, content);
            ContentType contentType = resolveType(info, defaultType);
            if (path != null) {
                fragments.add(ContentFragment.of(path, content, contentType));
            }
        }
        if (fragments.isEmpty()) {
            fragments.add(ContentFragment.of(new RelativePath("generated.txt"), rawText, defaultType));
        }
        return fragments;
    }

    private RelativePath extractPath(String info, String content) {
        // First token after ``` may be the path if it contains a slash
        if (!info.isBlank()) {
            String first = info.split("\\s+")[0];
            if (first.contains("/") || first.contains("\\")) {
                return new RelativePath(first.replace("\\", "/"));
            }
        }
        // Look for a path hint in the first non-empty line of the content
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            String trimmed = line.trim();
            // Strip comment markers
            if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--")) {
                trimmed = trimmed.substring(2).trim();
            }
            Matcher matcher = PATH_HINT.matcher(trimmed);
            if (matcher.find()) {
                return new RelativePath(matcher.group(1));
            }
            break;
        }
        return null;
    }

    private ContentType resolveType(String info, ContentType defaultType) {
        if (info.isBlank()) {
            return defaultType;
        }
        String[] parts = info.split("\\s+");
        for (String part : parts) {
            ContentType type = ContentType.fromExtension(part.toLowerCase());
            if (type != ContentType.UNKNOWN) {
                return type;
            }
        }
        return defaultType;
    }
}
