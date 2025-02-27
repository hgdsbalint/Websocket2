// src/main/java/com/example/websocketclient/controller/ClientController.java
package com.example.websocketclient.controller;

import com.example.websocketclient.service.WebSocketClientService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/client")
public class ClientController {

    private final WebSocketClientService webSocketClientService;
    private final Sinks.Many<String> messageSink = Sinks.many().multicast().onBackpressureBuffer();

    public ClientController(WebSocketClientService webSocketClientService) {
        this.webSocketClientService = webSocketClientService;
    }

    @PostMapping("/connect/{connectionId}")
    public Mono<String> connect(@PathVariable String connectionId) {
        webSocketClientService.connect(connectionId, message -> 
            messageSink.tryEmitNext(message)
        ).subscribe();
        
        return Mono.just("Connected to WebSocket with ID: " + connectionId);
    }

    @PostMapping(value = "/send", consumes = MediaType.TEXT_PLAIN_VALUE)
    public Mono<Void> sendMessage(@RequestBody String message) {
        webSocketClientService.sendMessage(message);
        return Mono.empty();
    }

    @GetMapping(value = "/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getMessages() {
        return messageSink.asFlux()
                .map(message -> ServerSentEvent.<String>builder()
                        .id(UUID.randomUUID().toString())
                        .event("message")
                        .data(message)
                        .build())
                .delayElements(Duration.ofMillis(100)); // Small delay for backpressure
    }

    @PostMapping("/disconnect")
    public Mono<String> disconnect() {
        webSocketClientService.disconnect();
        return Mono.just("Disconnected from WebSocket");
    }
}
