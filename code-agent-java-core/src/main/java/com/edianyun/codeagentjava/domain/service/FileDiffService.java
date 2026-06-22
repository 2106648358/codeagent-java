package com.edianyun.codeagentjava.domain.service;

/**
 * 文件差异计算服务接口，比较两个文本的差异并输出 diff 格式。
 * 用于在 CLI 中展示文件变更前后的对比。
 */
public interface FileDiffService {

    /** 计算 oldContent 到 newContent 的差异，返回 unified diff 格式字符串 */
    String diff(String oldContent, String newContent);
}
