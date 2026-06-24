package com.edianyun.codeagentjava.infrastructure.adapters;

import com.edianyun.codeagentjava.domain.model.identity.UserIdentity;
import com.edianyun.codeagentjava.domain.model.session.UserId;
import com.edianyun.codeagentjava.infrastructure.config.CodeAgentProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EnvironmentUserIdentityResolver 单元测试。
 * 验证从环境变量/配置读取用户身份的逻辑。
 */
class EnvironmentUserIdentityResolverTest {

    @Test
    void shouldResolveAnonymousIdentity() {
        CodeAgentProperties props = new CodeAgentProperties();
        CodeAgentProperties.IdentityProperties idProps = new CodeAgentProperties.IdentityProperties();
        idProps.setUserId("anonymous");
        idProps.setTenantId("default");
        props.setIdentity(idProps);

        EnvironmentUserIdentityResolver resolver = new EnvironmentUserIdentityResolver(props);
        UserIdentity identity = resolver.resolve();

        assertThat(identity.userId().value()).isEqualTo("anonymous");
        assertThat(identity.tenantId().value()).isEqualTo("default");
    }

    @Test
    void shouldResolveCustomIdentity() {
        CodeAgentProperties props = new CodeAgentProperties();
        CodeAgentProperties.IdentityProperties idProps = new CodeAgentProperties.IdentityProperties();
        idProps.setUserId("developer-1");
        idProps.setTenantId("org-xyz");
        props.setIdentity(idProps);

        EnvironmentUserIdentityResolver resolver = new EnvironmentUserIdentityResolver(props);
        UserIdentity identity = resolver.resolve();

        assertThat(identity.userId().value()).isEqualTo("developer-1");
        assertThat(identity.tenantId().value()).isEqualTo("org-xyz");
    }

    @Test
    void shouldResolveEmptyTenantAsDefault() {
        CodeAgentProperties props = new CodeAgentProperties();
        CodeAgentProperties.IdentityProperties idProps = new CodeAgentProperties.IdentityProperties();
        idProps.setUserId("user-1");
        idProps.setTenantId(null);
        props.setIdentity(idProps);

        EnvironmentUserIdentityResolver resolver = new EnvironmentUserIdentityResolver(props);
        UserIdentity identity = resolver.resolve();

        assertThat(identity.tenantId().value()).isEqualTo("default");
    }
}
