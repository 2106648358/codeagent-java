package com.edianyun.codeagentjava.domain.model.telemetry;

import com.edianyun.codeagentjava.domain.model.content.ContentType;
import com.edianyun.codeagentjava.domain.model.content.ModelId;

import java.time.Instant;

/**
 * 遥测事件值对象，记录用户每次命令操作的完整指标。
 * 包含命令类型、耗时、模型、Token 用量、文件信息等，
 * 用于远程日志分析和产品改进（不包含实际内容）。
 */
public record TelemetryEvent(
        String commandType,
        String sessionId,
        String userId,
        String tenantId,
        ModelId modelId,
        long durationMs,
        ContentType contentType,
        String fileExtension,
        int fileCount,
        boolean success,
        String errorMessage,
        Instant timestamp
) {

    public TelemetryEvent {
        if (commandType == null || commandType.isBlank()) {
            throw new IllegalArgumentException("Command type must not be blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp must not be null");
        }
    }

    /** 使用建造者模式创建 TelemetryEvent */
    public static Builder builder(String commandType) {
        return new Builder(commandType);
    }

    public static class Builder {
        private final String commandType;
        private String sessionId;
        private String userId;
        private String tenantId;
        private ModelId modelId;
        private long durationMs;
        private ContentType contentType;
        private String fileExtension;
        private int fileCount;
        private boolean success = true;
        private String errorMessage;
        private Instant timestamp = Instant.now();

        private Builder(String commandType) {
            this.commandType = commandType;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder modelId(ModelId modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder fileExtension(String fileExtension) {
            this.fileExtension = fileExtension;
            return this;
        }

        public Builder fileCount(int fileCount) {
            this.fileCount = fileCount;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public TelemetryEvent build() {
            return new TelemetryEvent(commandType, sessionId, userId, tenantId, modelId, durationMs,
                    contentType, fileExtension, fileCount, success, errorMessage, timestamp);
        }
    }
}
