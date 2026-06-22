package com.edianyun.codeagentjava.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性类，映射 codeagent.storage.* 配置项。
 * 当前仅包含 SQLite 数据库路径和连接池大小。
 */
@ConfigurationProperties(prefix = "codeagent.storage")
public class StorageProperties {

    private SqliteProperties sqlite = new SqliteProperties();

    public SqliteProperties getSqlite() {
        return sqlite;
    }

    public void setSqlite(SqliteProperties sqlite) {
        this.sqlite = sqlite;
    }

    public static class SqliteProperties {
        private String path = System.getProperty("user.home") + "/.codeagent/codeagent.db";
        private int poolSize = 2;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
    }
}
