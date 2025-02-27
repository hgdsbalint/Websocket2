// src/main/java/com/example/websocketdemo/websocket/WebSocketHandler.java
package com.example.websocketdemo.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler implements org.springframework.web.reactive.socket.WebSocketHandler {

    // Store sessions by ID
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    
    // Message sink for broadcasting
    private final Sinks.Many<String> messageSink = Sinks.many().multicast().onBackpressureBuffer();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Extract connection ID from URL path
        String connectionId = extractConnectionId(session);
        
        // Store the session with its ID
        sessions.put(connectionId, session);
        
        // Send messages to the client
        Mono<Void> output = session.send(
                messageSink.asFlux()
                        .filter(message -> {
                            // Parse the message to check if it's meant for this connection
                            String[] parts = message.split(":", 2);
                            return parts.length == 2 && parts[0].equals(connectionId);
                        })
                        .map(message -> {
                            // Remove the ID prefix from the message
                            String[] parts = message.split(":", 2);
                            return parts[1];
                        })
                        .map(session::textMessage)
        );
        
        // Process messages from the client
        Mono<Void> input = session.receive()
                .map(message -> message.getPayloadAsText())
                .doOnNext(message -> {
                    System.out.println("Received message on connection " + connectionId + ": " + message);
                    // Broadcast the message with connection ID
                    messageSink.tryEmitNext(connectionId + ":" + message);
                })
                .doOnTerminate(() -> {
                    // Remove the session when it's terminated
                    sessions.remove(connectionId);
                    System.out.println("Connection " + connectionId + " closed");
                })
                .then();
        
        // Combine input and output to handle both directions
        return Mono.zip(input, output).then();
    }
    
    private String extractConnectionId(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        String[] pathParts = path.split("/");
        return pathParts[pathParts.length - 1];
    }
    
    // Method to send a message to a specific connection
    public void sendMessage(String connectionId, String message) {
        messageSink.tryEmitNext(connectionId + ":" + message);
    }
}
