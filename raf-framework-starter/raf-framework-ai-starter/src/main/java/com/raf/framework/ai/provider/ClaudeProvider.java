package com.raf.framework.ai.provider;

import com.raf.framework.ai.AiProperties;
import com.raf.framework.ai.AiRequest;
import com.raf.framework.core.common.exception.InfrastructureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClaudeProvider implements AiProvider {

    private final AnthropicChatModel chatModel;
    private final AiProperties.ProviderConfig config;

    public ClaudeProvider(AiProperties.ProviderConfig config) {
        this.config = config;
        AnthropicApi api = AnthropicApi.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .build();
        this.chatModel = AnthropicChatModel.builder()
            .anthropicApi(api)
            .defaultOptions(AnthropicChatOptions.builder()
                .model(config.getModel())
                .temperature(config.getTemperature())
                .maxTokens(config.getMaxTokens())
                .build())
            .build();
    }

    @Override
    public String name() { return "claude"; }

    @Override
    public String chat(AiRequest request, List<Message> history) {
        try {
            ChatResponse response = chatModel.call(buildPrompt(request, history));
            return response.getResult().getOutput().getText();
        } catch (Exception e) {
            throw new InfrastructureException("Claude call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(AiRequest request, List<Message> history) {
        try {
            return chatModel.stream(buildPrompt(request, history))
                .mapNotNull(resp -> resp.getResult() != null ? resp.getResult().getOutput().getText() : null)
                .onErrorMap(e -> new InfrastructureException("Claude stream failed: " + e.getMessage(), e));
        } catch (Exception e) {
            return Flux.error(new InfrastructureException("Claude stream failed: " + e.getMessage(), e));
        }
    }

    private Prompt buildPrompt(AiRequest request, List<Message> history) {
        List<Message> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        messages.addAll(history);
        messages.add(new UserMessage(request.getPrompt()));
        AnthropicChatOptions options = AnthropicChatOptions.builder()
            .model(config.getModel())
            .temperature(request.getTemperature() != null ? request.getTemperature() : config.getTemperature())
            .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens())
            .build();
        return new Prompt(messages, options);
    }
}
