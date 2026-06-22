package com.edianyun.codeagentjava.domain.model.identity;

import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;

/**
 * 用户身份值对象，包含用户 ID、租户 ID 和机器标识。
 * 用于在多租户场景下追踪用户操作归属。
 */
public record UserIdentity(UserId userId, TenantId tenantId, String machineId) {

    public UserIdentity {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant id must not be null");
        }
    }

    /** 创建匿名用户身份（首次使用或未配置身份时） */
    public static UserIdentity anonymous(String machineId) {
        return new UserIdentity(UserId.anonymous(), TenantId.defaultTenant(), machineId);
    }
}
