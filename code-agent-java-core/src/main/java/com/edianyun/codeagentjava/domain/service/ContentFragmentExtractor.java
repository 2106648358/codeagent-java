package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentFragment;
import com.edianyun.codeagentjava.domain.model.content.ContentType;

import java.util.List;

/**
 * 内容片段提取器服务接口，从 LLM 返回的原始文本中解析出结构化的 ContentFragment。
 * 例如从 Markdown 代码块中提取文件路径和内容。
 */
public interface ContentFragmentExtractor {

    /** 解析 rawText，返回内容片段列表（可按默认类型回退） */
    List<ContentFragment> extract(String rawText, ContentType defaultType);
}
