package com.raf.framework.ai.provider;

import com.raf.framework.ai.AiProperties;
import com.raf.framework.ai.AiRequest;
import com.raf.framework.core.common.exception.InfrastructureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * DeepSeek Provider。
 * DeepSeek 兼容 OpenAI 协议，复用 OpenAiChatModel，通过 baseUrl 区分。
 */
@Slf4j
public class DeepSeekProvider implements AiProvider {

    private final OpenAiChatModel chatModel;
    private final AiProperties.ProviderConfig config;

    public DeepSeekProvider(AiProperties.ProviderConfig config) {
        this.config = config;
        OpenAiApi api = OpenAiApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();
        this.chatModel = OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build())
            .build();
    }

    @Override
    public String name() { return "deepseek"; }

    @Override
    public String chat(AiRequest request, List<Message> history) {
        try {
            ChatResponse response = chatModel.call(buildPrompt(request, history));
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            throw new InfrastructureException("DeepSeek call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(AiRequest request, List<Message> history) {
        try {
            return chatModel.stream(buildPrompt(request, history))
                .mapNotNull(resp -> resp.getResult() != null ? resp.getResult().getOutput().getText() : null)
                .onErrorMap(e -> new InfrastructureException("DeepSeek stream failed: " + e.getMessage(), e));
        } catch (Exception e) {
            return Flux.error(new InfrastructureException("DeepSeek stream failed: " + e.getMessage(), e));
        }
    }

    private Prompt buildPrompt(AiRequest request, List<Message> history) {
        List<Message> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        messages.addAll(history);
        messages.add(new UserMessage(request.getPrompt()));
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(config.getModel())
            .temperature(request.getTemperature() != null ? request.getTemperature() : config.getTemperature())
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
            .build();
        return new Prompt(messages, options);
    }
}
