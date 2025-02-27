// src/main/java/com/example/websocketclient/service/WebSocketClientService.java
package com.example.websocketclient.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.util.function.Consumer;

@Service
public class WebSocketClientService {

    private final WebSocketClient client = new ReactorNettyWebSocketClient();
    private Sinks.Many<String> outboundSink;
    private Mono<Void> connection;

    public Mono<Void> connect(String connectionId, Consumer<String> messageHandler) {
        try {
            URI uri = new URI("ws://localhost:8080/ws/" + connectionId);
            outboundSink = Sinks.many().unicast().onBackpressureBuffer();

            connection = client.execute(uri, session -> {
                Mono<Void> receive = session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(messageHandler)
                        .then();

                Mono<Void> send = session.send(
                        outboundSink.asFlux()
                                .map(session::textMessage)
                );

                return Mono.zip(receive, send).then();
            });

            return connection;
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public void sendMessage(String message) {
        if (outboundSink != null) {
            outboundSink.tryEmitNext(message);
        }
    }

    public void disconnect() {
        if (outboundSink != null) {
            outboundSink.tryEmitComplete();
        }
    }
}
