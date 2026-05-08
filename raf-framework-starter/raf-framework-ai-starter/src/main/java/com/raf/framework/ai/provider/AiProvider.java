package com.raf.framework.ai.provider;

import com.raf.framework.ai.AiRequest;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * AI Provider 统一接口。
 * 每个实现类对应一个具体的 AI 服务商。
 */
public interface AiProvider {

    /** Provider 名称，与 raf.ai.providers 配置 key 对应 */
    String name();

    /**
     * 同步调用，返回完整响应文本。
     *
     * @param request  请求对象
     * @param messages 历史消息列表，无会话时传空列表
     * @return AI 响应文本
     */
    String chat(AiRequest request, List<Message> messages);

    /**
     * 流式调用，逐 token 返回响应文本片段。
     *
     * @param request  请求对象
     * @param messages 历史消息列表，无会话时传空列表
     * @return token 流
     */
    Flux<String> streamChat(AiRequest request, List<Message> messages);
}
