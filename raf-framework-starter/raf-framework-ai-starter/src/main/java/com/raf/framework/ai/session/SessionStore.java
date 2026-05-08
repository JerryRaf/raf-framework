package com.raf.framework.ai.session;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/** 会话历史存储接口 */
public interface SessionStore {

    /** 加载指定会话的历史消息，不存在时返回空列表 */
    List<Message> load(String sessionId);

    /** 保存（覆盖）指定会话的历史消息 */
    void save(String sessionId, List<Message> messages, long ttlSeconds);

    /** 删除指定会话 */
    void delete(String sessionId);
}
