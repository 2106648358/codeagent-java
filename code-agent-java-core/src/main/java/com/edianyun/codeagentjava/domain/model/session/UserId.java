package com.edianyun.codeagentjava.domain.model.session;

/**
 * 用户 ID 值对象，标识一个开发者用户。
 * 匿名场景下使用 "anonymous" 作为默认值。
 */
public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("User id must not be blank");
        }
    }

    /** 返回匿名用户的 UserId */
    public static UserId anonymous() {
        return new UserId("anonymous");
    }

    @Override
    public String toString() {
        return value;
    }
}
