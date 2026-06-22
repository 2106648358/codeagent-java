package com.edianyun.codeagentjava.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 顶层配置属性类，映射 codeagent.* 前缀的配置项。
 * 包含身份、Agent、CLI 三个子配置块。
 */
@ConfigurationProperties(prefix = "codeagent")
public class CodeAgentProperties {

    private IdentityProperties identity = new IdentityProperties();
    private AgentProperties agent = new AgentProperties();
    private CliProperties cli = new CliProperties();

    public IdentityProperties getIdentity() {
        return identity;
    }

    public void setIdentity(IdentityProperties identity) {
        this.identity = identity;
    }

    public AgentProperties getAgent() {
        return agent;
    }

    public void setAgent(AgentProperties agent) {
        this.agent = agent;
    }

    public CliProperties getCli() {
        return cli;
    }

    public void setCli(CliProperties cli) {
        this.cli = cli;
    }

    public static class IdentityProperties {
        private String userId = "anonymous";
        private String tenantId = "default";
        private String resolver = "environment";

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getResolver() {
            return resolver;
        }

        public void setResolver(String resolver) {
            this.resolver = resolver;
        }
    }

    public static class AgentProperties {
        private String workspace = System.getProperty("user.home") + "/.codeagent/workspace";
        private CompactionProperties compaction = new CompactionProperties();

        public String getWorkspace() {
            return workspace;
        }

        public void setWorkspace(String workspace) {
            this.workspace = workspace;
        }

        public CompactionProperties getCompaction() {
            return compaction;
        }

        public void setCompaction(CompactionProperties compaction) {
            this.compaction = compaction;
        }

        public static class CompactionProperties {
            private boolean enabled = true;
            private int triggerMessages = 30;
            private int keepMessages = 10;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public int getTriggerMessages() {
                return triggerMessages;
            }

            public void setTriggerMessages(int triggerMessages) {
                this.triggerMessages = triggerMessages;
            }

            public int getKeepMessages() {
                return keepMessages;
            }

            public void setKeepMessages(int keepMessages) {
                this.keepMessages = keepMessages;
            }
        }
    }

    public static class CliProperties {
        private boolean streamOutput = true;
        private boolean confirmWrites = true;

        public boolean isStreamOutput() {
            return streamOutput;
        }

        public void setStreamOutput(boolean streamOutput) {
            this.streamOutput = streamOutput;
        }

        public boolean isConfirmWrites() {
            return confirmWrites;
        }

        public void setConfirmWrites(boolean confirmWrites) {
            this.confirmWrites = confirmWrites;
        }
    }
}
