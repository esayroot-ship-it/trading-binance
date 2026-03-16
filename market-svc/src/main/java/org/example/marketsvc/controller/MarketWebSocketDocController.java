package org.example.marketsvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.R;

import java.util.List;
import java.util.Map;

/**
 * Market WebSocket 文档接口。
 */
@RestController

/**
 * Market WebSocket 文档接口。
 */
@RequestMapping("/ws/market")
@Tag(name = "Market WebSocket")
public class MarketWebSocketDocController {

    @Operation(summary = "Market WS handshake doc", description = "WebSocket endpoint: ws://{host}/ws/market")
    @GetMapping(headers = "!Upgrade")
    public R<Map<String, Object>> websocketDoc() {
        Map<String, Object> body = Map.of(
                "protocol", "websocket",
                "endpoint", "/ws/market",
                "gatewayUrl", "ws://localhost:8080/ws/market",
                "directUrl", "ws://localhost:8082/ws/market",
                "topicFormat", List.of("quote:BTCUSDT", "kline:BTCUSDT:1m"),
                "clientMessages", List.of(
                        Map.of("op", "subscribe", "args", List.of("quote:BTCUSDT")),
                        Map.of("op", "unsubscribe", "args", List.of("quote:BTCUSDT")),
                        Map.of("op", "ping", "ts", 1710000000000L)
                ),
                "serverEvents", List.of("connected", "subscribed", "unsubscribed", "pong", "quote", "kline", "error")
        );
        return R.ok("ws endpoint doc", body);
    }
}
