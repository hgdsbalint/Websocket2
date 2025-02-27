// src/main/java/com/example/websocketdemo/controller/WebSocketController.java
package com.example.websocketdemo.controller;

import com.example.websocketdemo.websocket.WebSocketHandler;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/websocket")
public class WebSocketController {

    private final WebSocketHandler webSocketHandler;

    public WebSocketController(WebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    // Create a new WebSocket connection and return the connection ID
    @PostMapping("/connect")
    public Mono<String> createConnection() {
        String connectionId = UUID.randomUUID().toString();
        return Mono.just(connectionId);
    }

    // Send a message to a specific WebSocket connection
    @PostMapping(value = "/send/{connectionId}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<Void> sendMessage(@PathVariable String connectionId, @RequestBody String message) {
        webSocketHandler.sendMessage(connectionId, message);
        return Mono.empty();
    }
}
