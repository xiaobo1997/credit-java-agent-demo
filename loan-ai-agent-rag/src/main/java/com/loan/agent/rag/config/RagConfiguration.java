package com.loan.agent.rag.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.time.Duration;
import java.util.Map;

/**
 * RAG 模块配置 — 注册 DeepSeek ChatLanguageModel Bean
 *
 * <p>API Key 从仓库外的 Moon Bridge 配置文件读取，不硬编码。</p>
 */
@Configuration
public class RagConfiguration {

    @Value("${deepseek.model:deepseek-chat}")
    private String modelName;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    /**
     * 注册 ChatLanguageModel Bean，连接 DeepSeek
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String apiKey = loadApiKey();
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    /**
     * 从 ~/tools/moon-bridge/config.yml 读取 API Key
     * 兼容两种 YAML 结构：provider.providers.deepseek 和 providers.deepseek
     */
    private String loadApiKey() {
        try {
            String configPath = System.getProperty("user.home") + "/tools/moon-bridge/config.yml";
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(new FileInputStream(configPath));

            // 兼容 provider.providers.deepseek.api_key
            Object provider = config.get("provider");
            if (provider instanceof Map) {
                Object providers = ((Map<?, ?>) provider).get("providers");
                if (providers instanceof Map) {
                    Object deepseek = ((Map<?, ?>) providers).get("deepseek");
                    if (deepseek instanceof Map) {
                        Object key = ((Map<?, ?>) deepseek).get("api_key");
                        if (key != null) return key.toString();
                    }
                }
            }

            // 兼容 providers.deepseek.api_key
            Object providers = config.get("providers");
            if (providers instanceof Map) {
                Object deepseek = ((Map<?, ?>) providers).get("deepseek");
                if (deepseek instanceof Map) {
                    Object key = ((Map<?, ?>) deepseek).get("api_key");
                    if (key != null) return key.toString();
                }
            }

            throw new RuntimeException("无法从 Moon Bridge 配置中读取 DeepSeek API Key");
        } catch (Exception e) {
            throw new RuntimeException("读取 DeepSeek API Key 失败: " + e.getMessage(), e);
        }
    }
}
