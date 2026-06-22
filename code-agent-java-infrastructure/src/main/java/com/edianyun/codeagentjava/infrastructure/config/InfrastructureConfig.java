package com.edianyun.codeagentjava.infrastructure.config;

import com.edianyun.codeagentjava.application.port.ChatUseCase;
import com.edianyun.codeagentjava.application.port.ExplainUseCase;
import com.edianyun.codeagentjava.application.port.GenerateUseCase;
import com.edianyun.codeagentjava.application.service.ChatService;
import com.edianyun.codeagentjava.application.service.ExplainService;
import com.edianyun.codeagentjava.application.service.GenerateService;
import com.edianyun.codeagentjava.domain.repository.AgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.FileRepository;
import com.edianyun.codeagentjava.domain.repository.LocalStorage;
import com.edianyun.codeagentjava.domain.repository.RemoteSyncService;
import com.edianyun.codeagentjava.domain.repository.StreamingAgentOrchestrator;
import com.edianyun.codeagentjava.domain.repository.TelemetryCollector;
import com.edianyun.codeagentjava.domain.repository.UserIdentityResolver;
import com.edianyun.codeagentjava.domain.service.ContentFragmentExtractor;
import com.edianyun.codeagentjava.domain.service.ContentTypeResolver;
import com.edianyun.codeagentjava.domain.service.FileDiffService;
import com.edianyun.codeagentjava.domain.service.PromptBuilder;
import com.edianyun.codeagentjava.domain.service.WorkspaceScanner;
import com.edianyun.codeagentjava.domain.service.impl.DefaultPromptBuilder;
import com.edianyun.codeagentjava.domain.service.impl.DefaultWorkspaceScanner;
import com.edianyun.codeagentjava.domain.service.impl.ExtensionContentTypeResolver;
import com.edianyun.codeagentjava.domain.service.impl.MarkdownContentFragmentExtractor;
import com.edianyun.codeagentjava.domain.service.impl.UnifiedDiffFileDiffService;
import com.edianyun.codeagentjava.infrastructure.adapters.AgentScopeOrchestrator;
import com.edianyun.codeagentjava.infrastructure.adapters.EnvironmentUserIdentityResolver;
import com.edianyun.codeagentjava.infrastructure.adapters.JooqLocalStorage;
import com.edianyun.codeagentjava.infrastructure.adapters.LocalFileSystemFileRepository;
import com.edianyun.codeagentjava.infrastructure.adapters.PostgreSqlRemoteSyncService;
import com.edianyun.codeagentjava.infrastructure.adapters.SQLiteBufferedTelemetryCollector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;
import java.io.File;

/**
 * 基础设施 Spring 配置类，装配所有 Bean 到 Spring 上下文。
 * 包括：
 * - 领域服务（PromptBuilder、WorkspaceScanner 等）
 * - 数据源与 JOOQ DSLContext（SQLite）
 * - Flyway 数据库迁移
 * - 适配器（TelemetryCollector、LocalStorage、AgentScopeOrchestrator 等）
 * - 应用服务（ChatUseCase、GenerateUseCase、ExplainUseCase）
 */
@Configuration
public class InfrastructureConfig {

    @Bean
    public PromptBuilder promptBuilder() {
        return new DefaultPromptBuilder();
    }

    @Bean
    public ContentTypeResolver contentTypeResolver() {
        return new ExtensionContentTypeResolver();
    }

    @Bean
    public ContentFragmentExtractor contentFragmentExtractor() {
        return new MarkdownContentFragmentExtractor();
    }

    @Bean
    public WorkspaceScanner workspaceScanner(ContentTypeResolver contentTypeResolver) {
        return new DefaultWorkspaceScanner(contentTypeResolver);
    }

    @Bean
    public FileDiffService fileDiffService() {
        return new UnifiedDiffFileDiffService();
    }

    @Bean
    public FileRepository fileRepository() {
        return new LocalFileSystemFileRepository();
    }

    @Bean
    public UserIdentityResolver userIdentityResolver(CodeAgentProperties codeAgentProperties) {
        return new EnvironmentUserIdentityResolver(codeAgentProperties);
    }

    @Bean
    public DataSource dataSource(StorageProperties storageProperties) {
        HikariConfig config = new HikariConfig();
        String path = storageProperties.getSqlite().getPath();
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        config.setJdbcUrl("jdbc:sqlite:" + path);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(storageProperties.getSqlite().getPoolSize());
        config.setPoolName("codeagent-sqlite");
        return new HikariDataSource(config);
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    public DSLContext dslContext(DataSource dataSource, Flyway flyway) {
        return DSL.using(dataSource, SQLDialect.SQLITE);
    }

    @Bean
    public TelemetryCollector telemetryCollector(DSLContext dslContext) {
        return new SQLiteBufferedTelemetryCollector(dslContext);
    }

    @Bean
    public LocalStorage localStorage(DSLContext dslContext) {
        return new JooqLocalStorage(dslContext);
    }

    @Bean
    public RemoteSyncService remoteSyncService() {
        return new PostgreSqlRemoteSyncService();
    }

    @Lazy
    @Bean
    public AgentScopeOrchestrator agentScopeOrchestrator(CodeAgentProperties codeAgentProperties,
                                                          LlmProperties llmProperties,
                                                          PromptBuilder promptBuilder,
                                                          ContentFragmentExtractor contentFragmentExtractor) {
        return new AgentScopeOrchestrator(codeAgentProperties, llmProperties, promptBuilder, contentFragmentExtractor);
    }

    @Bean
    public AgentOrchestrator agentOrchestrator(AgentScopeOrchestrator agentScopeOrchestrator) {
        return agentScopeOrchestrator;
    }

    @Bean
    public StreamingAgentOrchestrator streamingAgentOrchestrator(AgentScopeOrchestrator agentScopeOrchestrator) {
        return agentScopeOrchestrator;
    }

    @Bean
    public ChatUseCase chatUseCase(AgentOrchestrator agentOrchestrator,
                                    StreamingAgentOrchestrator streamingAgentOrchestrator,
                                    LocalStorage localStorage,
                                    UserIdentityResolver userIdentityResolver,
                                    TelemetryCollector telemetryCollector) {
        return new ChatService(agentOrchestrator, streamingAgentOrchestrator, localStorage,
                userIdentityResolver, telemetryCollector);
    }

    @Bean
    public GenerateUseCase generateUseCase(AgentOrchestrator agentOrchestrator,
                                            StreamingAgentOrchestrator streamingAgentOrchestrator,
                                            FileRepository fileRepository,
                                            LocalStorage localStorage,
                                            UserIdentityResolver userIdentityResolver,
                                            TelemetryCollector telemetryCollector,
                                            ContentFragmentExtractor contentFragmentExtractor) {
        return new GenerateService(agentOrchestrator, streamingAgentOrchestrator, fileRepository, localStorage,
                userIdentityResolver, telemetryCollector, contentFragmentExtractor);
    }

    @Bean
    public ExplainUseCase explainUseCase(AgentOrchestrator agentOrchestrator,
                                          StreamingAgentOrchestrator streamingAgentOrchestrator,
                                          FileRepository fileRepository,
                                          UserIdentityResolver userIdentityResolver,
                                          TelemetryCollector telemetryCollector) {
        return new ExplainService(agentOrchestrator, streamingAgentOrchestrator, fileRepository,
                userIdentityResolver, telemetryCollector);
    }
}
