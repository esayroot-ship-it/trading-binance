package org.example.marketsvc.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketWebSocketHandler extends TextWebSocketHandler {

    private static final String OP_SUBSCRIBE = "subscribe";
    private static final String OP_UNSUBSCRIBE = "unsubscribe";
    private static final String OP_PING = "ping";

    private final ObjectMapper objectMapper;
    private final MarketWsSubscriptionManager subscriptionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        subscriptionManager.register(session);
        sendJson(session, Map.of(
                "event", "connected",
                "sessionId", session.getId(),
                "ts", System.currentTimeMillis()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if (!StringUtils.hasText(payload)) {
            sendError(session, "EMPTY_MESSAGE", "payload is empty");
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception ex) {
            sendError(session, "INVALID_JSON", "payload is not valid json");
            return;
        }

        String op = root.path("op").asText("");
        if (OP_PING.equalsIgnoreCase(op)) {
            sendPong(session, root.path("ts").asLong(System.currentTimeMillis()));
            return;
        }

        ArrayNode argsNode = root.has("args") && root.get("args").isArray()
                ? (ArrayNode) root.get("args")
                : null;
        if (argsNode == null || argsNode.isEmpty()) {
            sendError(session, "INVALID_ARGS", "args is required");
            return;
        }

        if (OP_SUBSCRIBE.equalsIgnoreCase(op)) {
            for (JsonNode node : argsNode) {
                handleSubscribe(session, node.asText(""));
            }
            return;
        }
        if (OP_UNSUBSCRIBE.equalsIgnoreCase(op)) {
            for (JsonNode node : argsNode) {
                handleUnsubscribe(session, node.asText(""));
            }
            return;
        }
        sendError(session, "UNSUPPORTED_OP", "unsupported op");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("市场长连接传输异常，会话编号={}", session.getId(), exception);
        subscriptionManager.remove(session.getId());
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        subscriptionManager.remove(session.getId());
    }

    private void handleSubscribe(WebSocketSession session, String rawTopic) {
        MarketWsTopicParser.ParsedTopic parsed = MarketWsTopicParser.parse(rawTopic);
        if (parsed == null) {
            sendError(session, "INVALID_TOPIC", "topic format invalid");
            return;
        }
        subscriptionManager.addTopic(session.getId(), parsed.normalizedTopic());
        sendJson(session, Map.of(
                "event", "subscribed",
                "arg", parsed.normalizedTopic(),
                "ts", System.currentTimeMillis()));
    }

    private void handleUnsubscribe(WebSocketSession session, String rawTopic) {
        MarketWsTopicParser.ParsedTopic parsed = MarketWsTopicParser.parse(rawTopic);
        if (parsed == null) {
            sendError(session, "INVALID_TOPIC", "topic format invalid");
            return;
        }
        subscriptionManager.removeTopic(session.getId(), parsed.normalizedTopic());
        sendJson(session, Map.of(
                "event", "unsubscribed",
                "arg", parsed.normalizedTopic(),
                "ts", System.currentTimeMillis()));
    }

    private void sendPong(WebSocketSession session, long ts) {
        sendJson(session, Map.of(
                "event", "pong",
                "ts", ts));
    }

    private void sendError(WebSocketSession session, String code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "error");
        payload.put("code", code);
        payload.put("message", message);
        payload.put("ts", System.currentTimeMillis());
        sendJson(session, payload);
    }

    private void sendJson(WebSocketSession session, Object payload) {
        if (session == null || payload == null) {
            return;
        }
        try {
            synchronized (session) {
                if (!session.isOpen()) {
                    return;
                }
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (IOException ex) {
            log.warn("市场长连接发送失败，会话编号={}", session.getId(), ex);
            subscriptionManager.remove(session.getId());
            closeQuietly(session, CloseStatus.SERVER_ERROR);
        } catch (Exception ex) {
            log.warn("市场长连接发送失败，会话编号={}", session.getId(), ex);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            if (session != null && session.isOpen()) {
                session.close(status);
            }
        } catch (IOException ex) {
            log.debug("忽略长连接关闭异常", ex);
        }
    }
}
