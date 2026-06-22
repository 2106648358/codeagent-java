package com.edianyun.codeagentjava.domain.model.message;

/**
 * 消息角色枚举，标识一条消息的发送者身份。
 * - USER: 用户输入
 * - ASSISTANT: AI 助手的回复
 * - SYSTEM: 系统级提示/指令
 * - TOOL: 工具调用结果
 */
public enum Role {

    USER,
    ASSISTANT,
    SYSTEM,
    TOOL
}
