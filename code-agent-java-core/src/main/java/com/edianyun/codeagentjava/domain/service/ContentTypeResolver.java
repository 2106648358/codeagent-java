package com.edianyun.codeagentjava.domain.service;

import com.edianyun.codeagentjava.domain.model.content.ContentType;

/**
 * 内容类型解析器服务接口，根据文件名推断其内容类型。
 * 用于扫描工作区时自动标记文件类型。
 */
public interface ContentTypeResolver {

    /** 根据文件名解析内容类型 */
    ContentType resolve(String fileName);
}
