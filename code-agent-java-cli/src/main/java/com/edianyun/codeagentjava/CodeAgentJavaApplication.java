package com.edianyun.codeagentjava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 编程助手 CLI 应用的 Spring Boot 入口。
 * 基于 Spring Shell 提供交互式命令行界面，
 * 支持 chat/generate/explain 三个核心命令。
 */
@SpringBootApplication
public class CodeAgentJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeAgentJavaApplication.class, args);
    }

}
