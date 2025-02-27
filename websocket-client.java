// WebSocket Client Application
// src/main/java/com/example/websocketclient/WebSocketClientApplication.java
package com.example.websocketclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.WebSocketMessage;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.util.Scanner;

@SpringBootApplication
public class WebSocketClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner run() {
        return args -> {
            Scanner scanner = new Scanner(System.in);
            
            System.out.println("Enter connection ID to connect to: ");
            String connectionId = scanner.nextLine();
            
            WebSocketClient client = new ReactorNettyWebSocketClient();
            URI uri = new URI("ws://localhost:8080/ws/" + connectionId);
            
            Sinks.Many<String> outboundSink = Sinks.many().unicast().onBackpressureBuffer();
            
            System.out.println("Connecting to WebSocket server...");
            client.execute(uri, session -> {
                Mono<Void> receive = session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(message -> System.out.println("Received: " + message))
                    .then();
                
                Mono<Void> send = session.send(
                    outboundSink.asFlux()
                        .map(session::textMessage)
                );
                
                // Start a background thread to read from console and send messages
                new Thread(() -> {
                    System.out.println("Connected! Enter messages to send (or 'exit' to quit):");
                    while (true) {
                        String message = scanner.nextLine();
                        if ("exit".equalsIgnoreCase(message)) {
                            outboundSink.tryEmitComplete();
                            break;
                        }
                        outboundSink.tryEmitNext(message);
                    }
                }).start();
                
                return Mono.zip(receive, send).then();
            }).block(Duration.ofSeconds(5));
            
            System.out.println("WebSocket connection closed");
        };
    }
}
