package com.edianyun.codeagentjava.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 遥测配置属性类，映射 codeagent.telemetry.* 配置项。
 * 包含遥测开关、批量大小、刷新间隔和 PostgreSQL 连接信息。
 */
@ConfigurationProperties(prefix = "codeagent.telemetry")
public class TelemetryProperties {

    private boolean enabled = true;
    private int batchSize = 100;
    private int flushIntervalSeconds = 30;
    private PostgresProperties postgres = new PostgresProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getFlushIntervalSeconds() {
        return flushIntervalSeconds;
    }

    public void setFlushIntervalSeconds(int flushIntervalSeconds) {
        this.flushIntervalSeconds = flushIntervalSeconds;
    }

    public PostgresProperties getPostgres() {
        return postgres;
    }

    public void setPostgres(PostgresProperties postgres) {
        this.postgres = postgres;
    }

    public static class PostgresProperties {
        private String url;
        private String user;
        private String password;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
