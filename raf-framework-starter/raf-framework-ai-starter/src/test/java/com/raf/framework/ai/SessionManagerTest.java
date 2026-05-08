package com.raf.framework.ai;

import com.raf.framework.ai.session.MemorySessionStore;
import com.raf.framework.ai.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        AiProperties props = new AiProperties();
        props.getSession().setMaxHistory(4);
        sessionManager = new SessionManager(new MemorySessionStore(), props);
    }

    @Test
    void new_session_returns_empty_history() {
        List<Message> history = sessionManager.loadHistory("new-session");
        assertThat(history).isEmpty();
    }

    @Test
    void saves_and_loads_messages() {
        sessionManager.appendUserMessage("s1", "hello");
        sessionManager.appendAssistantMessage("s1", "hi there");

        List<Message> history = sessionManager.loadHistory("s1");
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getText()).isEqualTo("hello");
        assertThat(history.get(1).getText()).isEqualTo("hi there");
    }

    @Test
    void sliding_window_drops_oldest_when_exceeds_max_history() {
        sessionManager.appendUserMessage("s2", "msg1");
        sessionManager.appendAssistantMessage("s2", "reply1");
        sessionManager.appendUserMessage("s2", "msg2");
        sessionManager.appendAssistantMessage("s2", "reply2");
        sessionManager.appendUserMessage("s2", "msg3");

        List<Message> history = sessionManager.loadHistory("s2");
        assertThat(history).hasSize(4);
        assertThat(history.get(0).getText()).isEqualTo("reply1");
    }

    @Test
    void clear_removes_session() {
        sessionManager.appendUserMessage("s3", "hello");
        sessionManager.clearSession("s3");
        assertThat(sessionManager.loadHistory("s3")).isEmpty();
    }
}
