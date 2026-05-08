package com.raf.framework.ai;

import com.raf.framework.ai.provider.AiProvider;
import com.raf.framework.ai.provider.ProviderFactory;
import com.raf.framework.ai.router.AiRouter;
import com.raf.framework.ai.session.SessionManager;
import com.raf.framework.core.common.exception.BusinessException;
import com.raf.framework.core.common.result.RafResponseEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ProviderFactory providerFactory;
    private final AiRouter router;
    private final SessionManager sessionManager;

    @Override
    public String chat(String prompt) {
        return chat(AiRequest.builder().prompt(prompt).build());
    }

    @Override
    public String chat(String provider, String prompt) {
        return chat(AiRequest.builder().provider(provider).prompt(prompt).build());
    }

    @Override
    public String chat(AiRequest request) {
        validatePrompt(request.getPrompt());
        String providerName = router.resolve(request);
        AiProvider provider = providerFactory.get(providerName);
        List<Message> history = loadHistory(request);
        String response = provider.chat(request, history);
        saveHistory(request, response);
        return response;
    }

    @Override
    public Flux<String> streamChat(String prompt) {
        return streamChat(AiRequest.builder().prompt(prompt).build());
    }

    @Override
    public Flux<String> streamChat(AiRequest request) {
        validatePrompt(request.getPrompt());
        String providerName = router.resolve(request);
        AiProvider provider = providerFactory.get(providerName);
        List<Message> history = loadHistory(request);

        // 流式场景：先追加用户消息，响应完成后追加 AI 消息
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            sessionManager.appendUserMessage(request.getSessionId(), request.getPrompt());
        }

        StringBuilder fullResponse = new StringBuilder();
        return provider.streamChat(request, history)
            .doOnNext(fullResponse::append)
            .doOnComplete(() -> {
                if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
                    sessionManager.appendAssistantMessage(request.getSessionId(), fullResponse.toString());
                }
            });
    }

    private void validatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(RafResponseEnum.PARAM_ERROR, "prompt must not be blank");
        }
    }

    private List<Message> loadHistory(AiRequest request) {
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return sessionManager.loadHistory(request.getSessionId());
        }
        return Collections.emptyList();
    }

    private void saveHistory(AiRequest request, String response) {
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            sessionManager.appendUserMessage(request.getSessionId(), request.getPrompt());
            sessionManager.appendAssistantMessage(request.getSessionId(), response);
        }
    }
}
