package com.edianyun.codeagentjava.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用所有 @ConfigurationProperties 绑定。
 * 将 application.properties 中 codeagent.* 前缀的配置映射到对应的 Properties 类。
 */
@Configuration
@EnableConfigurationProperties({
        CodeAgentProperties.class,
        LlmProperties.class,
        StorageProperties.class,
        TelemetryProperties.class
})
public class CodeAgentConfigurationProperties {
}
