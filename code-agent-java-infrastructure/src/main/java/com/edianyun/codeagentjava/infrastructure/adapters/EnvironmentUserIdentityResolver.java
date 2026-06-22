package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.session.TenantId;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;
import com.edianyun.codeagentjava.infrastructure.config.CodeAgentProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 环境变量用户身份解析适配器。
 * 从 CODEAGENT_USER_ID / CODEAGENT_TENANT_ID 环境变量读取用户身份，
 * 若未配置则回退到配置文件或默认值 "anonymous"/"default"。
 */
public class EnvironmentUserIdentityResolver implements UserIdentityResolver {

    private static final String USER_ID_ENV = "CODEAGENT_USER_ID";
    private static final String TENANT_ID_ENV = "CODEAGENT_TENANT_ID";

    private final CodeAgentProperties codeAgentProperties;

    public EnvironmentUserIdentityResolver(CodeAgentProperties codeAgentProperties) {
        this.codeAgentProperties = codeAgentProperties;
    }

    @Override
    public UserIdentity resolve() {
        String userId = System.getenv(USER_ID_ENV);
        if (userId == null || userId.isBlank()) {
            userId = codeAgentProperties.getIdentity().getUserId();
        }
        if (userId == null || userId.isBlank() || userId.startsWith("${")) {
            userId = "anonymous";
        }

        String tenantId = System.getenv(TENANT_ID_ENV);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = codeAgentProperties.getIdentity().getTenantId();
        }
        if (tenantId == null || tenantId.isBlank() || tenantId.startsWith("${")) {
            tenantId = "default";
        }

        return new UserIdentity(new UserId(userId), new TenantId(tenantId), machineId());
    }

    private String machineId() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
