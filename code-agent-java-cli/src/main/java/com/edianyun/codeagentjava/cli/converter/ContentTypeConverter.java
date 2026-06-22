package com.edianyun.codeagentjava.cli.converter;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring Shell 参数转换器，将字符串参数转换为 ContentType 枚举。
 * 支持直接输入枚举名（如 CODE）或文件扩展名（如 java）。
 */
@Component
public class ContentTypeConverter implements Converter<String, ContentType> {

    @Override
    public ContentType convert(String source) {
        if (source == null || source.isBlank()) {
            return ContentType.UNKNOWN;
        }
        try {
            return ContentType.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContentType.fromExtension(source);
        }
    }
}
