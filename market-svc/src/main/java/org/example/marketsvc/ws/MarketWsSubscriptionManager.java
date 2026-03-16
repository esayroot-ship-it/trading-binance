package org.example.marketsvc.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MarketWsSubscriptionManager {

    private final ConcurrentMap<String, SessionState> sessionStateMap = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessionStateMap.put(session.getId(), new SessionState(session));
    }

    public void remove(String sessionId) {
        if (sessionId == null) {
            return;
        }
        sessionStateMap.remove(sessionId);
    }

    public boolean addTopic(String sessionId, String topic) {
        SessionState state = sessionStateMap.get(sessionId);
        return state != null && state.topics.add(topic);
    }

    public boolean removeTopic(String sessionId, String topic) {
        SessionState state = sessionStateMap.get(sessionId);
        return state != null && state.topics.remove(topic);
    }

    public long nextSeq(String sessionId) {
        SessionState state = sessionStateMap.get(sessionId);
        return state == null ? -1L : state.seq.incrementAndGet();
    }

    public List<SessionSnapshot> snapshot() {
        List<SessionSnapshot> snapshots = new ArrayList<>();
        for (SessionState state : sessionStateMap.values()) {
            snapshots.add(new SessionSnapshot(
                    state.session.getId(),
                    state.session,
                    Set.copyOf(state.topics)));
        }
        return snapshots;
    }

    private static final class SessionState {
        private final WebSocketSession session;
        private final Set<String> topics = ConcurrentHashMap.newKeySet();
        private final AtomicLong seq = new AtomicLong(0);

        private SessionState(WebSocketSession session) {
            this.session = session;
        }
    }

    public record SessionSnapshot(String sessionId, WebSocketSession session, Set<String> topics) {
    }
}
