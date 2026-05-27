package com.loan.agent.config;

import com.loan.agent.agent.CreditReviewAgent;
import com.loan.agent.tool.CreditCheckTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

/**
 * DeepSeek 配置：从 Moon Bridge 配置文件读取 API Key，创建 OpenAiChatModel。
 * 走 DeepSeek OpenAI 兼容端点，协议兼容性最好。
 */
@Slf4j
@Configuration
public class DeepSeekConfig {

    @Value("${deepseek.model:deepseek-chat}")
    private String modelName;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${deepseek.moon-bridge-config-path:#{systemProperties['user.home'] + '/tools/moon-bridge/config.yml'}}")
    private String moonBridgeConfigPath;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        String apiKey = loadApiKeyFromMoonBridgeConfig();
        log.info("DeepSeek 模型初始化: model={}, baseUrl={}", modelName, baseUrl);

        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    public CreditReviewAgent creditReviewAgent(ChatLanguageModel chatLanguageModel,
                                                CreditCheckTool creditCheckTool) {
        InMemoryChatMemoryStore memoryStore = new InMemoryChatMemoryStore();
        return AiServices.builder(CreditReviewAgent.class)
                .chatLanguageModel(chatLanguageModel)
                .tools(creditCheckTool)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
    }

    private String loadApiKeyFromMoonBridgeConfig() {
        try (InputStream input = new FileInputStream(moonBridgeConfigPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(input);

            // 兼容两种结构：provider.providers.deepseek.api_key 或 providers.deepseek.api_key
            Map<String, Object> providers;
            if (config.containsKey("provider")) {
                Map<String, Object> provider = (Map<String, Object>) config.get("provider");
                providers = (Map<String, Object>) provider.get("providers");
            } else {
                providers = (Map<String, Object>) config.get("providers");
            }

            Map<String, Object> deepseek = (Map<String, Object>) providers.get("deepseek");
            String apiKey = (String) deepseek.get("api_key");
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("Moon Bridge 配置中未找到 api_key");
            }
            return apiKey;
        } catch (Exception e) {
            throw new IllegalStateException("无法读取 Moon Bridge 配置文件: " + moonBridgeConfigPath, e);
        }
    }
}
