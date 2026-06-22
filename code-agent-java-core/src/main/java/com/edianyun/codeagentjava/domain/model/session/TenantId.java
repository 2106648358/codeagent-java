package com.edianyun.codeagentjava.domain.model.session;

/**
 * 租户 ID 值对象，用于多租户场景下的租户隔离。
 * 单用户场景使用 "default" 作为默认租户。
 */
public record TenantId(String value) {

    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tenant id must not be blank");
        }
    }

    /** 返回默认租户的 TenantId */
    public static TenantId defaultTenant() {
        return new TenantId("default");
    }

    @Override
    public String toString() {
        return value;
    }
}
