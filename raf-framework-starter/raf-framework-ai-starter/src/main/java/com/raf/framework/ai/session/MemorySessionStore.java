package com.raf.framework.ai.session;

import org.springframework.ai.chat.messages.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** 内存会话存储，重启后丢失，适合开发/单机场景 */
public class MemorySessionStore implements SessionStore {

    private final ConcurrentHashMap<String, List<Message>> store = new ConcurrentHashMap<>();

    @Override
    public List<Message> load(String sessionId) {
        return store.getOrDefault(sessionId, new ArrayList<>());
    }

    @Override
    public void save(String sessionId, List<Message> messages, long ttlSeconds) {
        store.put(sessionId, new ArrayList<>(messages));
    }

    @Override
    public void delete(String sessionId) {
        store.remove(sessionId);
    }
}
