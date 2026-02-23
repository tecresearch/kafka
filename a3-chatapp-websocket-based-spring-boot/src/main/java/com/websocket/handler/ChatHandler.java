package com.websocket.handler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHandler extends TextWebSocketHandler {

    // Simple storage for users and their sessions
    private final Map<String, WebSocketSession> users = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("New user connected: " + session.getId());
        session.sendMessage(new TextMessage("Welcome! Use /name yourname to set username"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload().trim();
        String sessionId = session.getId();

        // Handle username setting
        if (payload.startsWith("/name ")) {
            String username = payload.substring(6).trim();

            if (username.isEmpty()) {
                session.sendMessage(new TextMessage("Username cannot be empty"));
                return;
            }

            if (users.containsKey(username)) {
                session.sendMessage(new TextMessage("Username already taken"));
                return;
            }

            // Register user
            users.put(username, session);
            sessionUserMap.put(sessionId, username);

            session.sendMessage(new TextMessage("Welcome " + username + "! You can now chat"));

            // Notify others
            broadcast(username + " joined the chat", username);
            return;
        }

        // Check if user is registered
        String username = sessionUserMap.get(sessionId);
        if (username == null) {
            session.sendMessage(new TextMessage("Please set username first: /name yourname"));
            return;
        }

        // Handle private message
        if (payload.startsWith("/pm ")) {
            String[] parts = payload.split(" ", 3);
            if (parts.length < 3) {
                session.sendMessage(new TextMessage("Use: /pm username message"));
                return;
            }

            String toUser = parts[1];
            String privateMsg = parts[2];

            WebSocketSession targetSession = users.get(toUser);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(new TextMessage("[Private from " + username + "] " + privateMsg));
                session.sendMessage(new TextMessage("[Private to " + toUser + "] " + privateMsg));
            } else {
                session.sendMessage(new TextMessage("User " + toUser + " not found"));
            }
            return;
        }

        // Broadcast message to everyone
        broadcast(username + ": " + payload, username);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = sessionUserMap.remove(session.getId());
        if (username != null) {
            users.remove(username);
            broadcast(username + " left the chat", username);
        }
    }

    private void broadcast(String message, String sender) {
        users.forEach((name, session) -> {
            if (!name.equals(sender) && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}