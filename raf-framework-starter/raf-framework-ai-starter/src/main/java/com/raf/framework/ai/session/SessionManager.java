package com.raf.framework.ai.session;

import com.raf.framework.ai.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话管理器，负责加载/追加/保存历史消息，并维护滑动窗口。
 */
@RequiredArgsConstructor
public class SessionManager {

    private final SessionStore store;
    private final AiProperties properties;

    /** 加载指定会话的历史消息 */
    public List<Message> loadHistory(String sessionId) {
        return store.load(sessionId);
    }

    /** 追加用户消息并持久化 */
    public void appendUserMessage(String sessionId, String content) {
        List<Message> messages = new ArrayList<>(store.load(sessionId));
        messages.add(new UserMessage(content));
        trim(messages);
        store.save(sessionId, messages, properties.getSession().getTtl());
    }

    /** 追加 AI 响应消息并持久化 */
    public void appendAssistantMessage(String sessionId, String content) {
        List<Message> messages = new ArrayList<>(store.load(sessionId));
        messages.add(new AssistantMessage(content));
        trim(messages);
        store.save(sessionId, messages, properties.getSession().getTtl());
    }

    /** 清除会话 */
    public void clearSession(String sessionId) {
        store.delete(sessionId);
    }

    /** 滑动窗口：超过 maxHistory 时丢弃最早的消息 */
    private void trim(List<Message> messages) {
        int max = properties.getSession().getMaxHistory();
        while (messages.size() > max) {
            messages.remove(0);
        }
    }
}
