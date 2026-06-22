package com.edianyun.codeagentjava.domain.service.impl;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.service.ContentTypeResolver;

import java.nio.file.Path;

/**
 * 基于文件扩展名的内容类型解析器实现。
 * 通过文件后缀名（如 .java, .md, .json）判断内容类型。
 */
public class ExtensionContentTypeResolver implements ContentTypeResolver {

    @Override
    public ContentType resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return ContentType.UNKNOWN;
        }
        String name = Path.of(fileName).getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return ContentType.TEXT;
        }
        String extension = name.substring(dot + 1);
        return ContentType.fromExtension(extension);
    }
}
