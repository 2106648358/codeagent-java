package com.edianyun.codeagentjava.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LLM 配置属性类，映射 codeagent.llm.* 配置项。
 * 包括 Provider 选择、模型名称、温度、最大 Token、超时等，
 * 以及 OpenAI 和 DashScope 的专用配置。
 */
@ConfigurationProperties(prefix = "codeagent.llm")
public class LlmProperties {

    private String provider = "agentscope";
    private String model = "openai:gpt-4.1";
    private double temperature = 0.2;
    private int maxTokens = 4096;
    private int timeoutSeconds = 60;
    private OpenAiProperties openai = new OpenAiProperties();
    private DashscopeProperties dashscope = new DashscopeProperties();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public OpenAiProperties getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAiProperties openai) {
        this.openai = openai;
    }

    public DashscopeProperties getDashscope() {
        return dashscope;
    }

    public void setDashscope(DashscopeProperties dashscope) {
        this.dashscope = dashscope;
    }

    public static class OpenAiProperties {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }

    public static class DashscopeProperties {
        private String apiKey;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
