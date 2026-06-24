package com.edianyun.codeagentjava.domain.model.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TenantId 值对象单元测试。
 * 验证租户标识符校验和默认租户工厂方法。
 */
class TenantIdTest {

    @Test
    void shouldCreateValidTenantId() {
        TenantId id = new TenantId("org-xyz");
        assertThat(id.value()).isEqualTo("org-xyz");
    }

    @Test
    void shouldRejectNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new TenantId(null));
    }

    @Test
    void shouldRejectBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> new TenantId(""));
        assertThrows(IllegalArgumentException.class, () -> new TenantId("  "));
    }

    @Test
    void shouldReturnDefaultTenant() {
        TenantId id = TenantId.defaultTenant();
        assertThat(id.value()).isEqualTo("default");
    }

    @Test
    void shouldSupportEquality() {
        assertThat(new TenantId("t1")).isEqualTo(new TenantId("t1"));
        assertThat(new TenantId("t1")).isNotEqualTo(new TenantId("t2"));
    }
}
