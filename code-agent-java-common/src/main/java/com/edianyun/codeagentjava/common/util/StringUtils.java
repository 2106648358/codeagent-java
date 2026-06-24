package com.edianyun.codeagentjava.common.util;

/**
 * 通用字符串工具方法，提供项目中多处复用的字符串处理函数。
 * 该模块独立于所有业务模块，作为最底层的通用基础设施。
 */
public final class StringUtils {

    private StringUtils() {
        // 工具类禁止实例化
    }

    /**
     * 将空字符串或空白字符串转换为 null，用于命令行选项中空白默认值的规范化处理。
     *
     * @param value 输入字符串
     * @return 若 value 为 null 或 blank 则返回 null，否则返回原值
     */
    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
