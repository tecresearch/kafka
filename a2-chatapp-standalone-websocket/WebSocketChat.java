import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/chat")
public class WebSocketChat {

    // username → session
    private static final Map<String, Session> users = new ConcurrentHashMap<>();
    private String username;

    @OnOpen
    public void onOpen(Session session) throws IOException {
        session.getBasicRemote().sendText("Welcome! Please set your username with /name yourname");
    }

    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        message = message.trim();

        // Set username
        if (message.startsWith("/name ")) {
            String name = message.substring(6).trim();
            if (name.isEmpty()) {
                session.getBasicRemote().sendText("Invalid username.");
                return;
            }
            if (users.containsKey(name)) {
                session.getBasicRemote().sendText("Username already taken.");
                return;
            }
            username = name;
            users.put(username, session);
            broadcast("SERVER: " + username + " joined the chat", null);
            return;
        }

        if (username == null) {
            session.getBasicRemote().sendText("Set your username first with /name yourname");
            return;
        }

        // List online users
        if (message.equalsIgnoreCase("/users")) {
            session.getBasicRemote().sendText("ONLINE USERS: " + String.join(", ", users.keySet()));
            return;
        }

        // Private message
        if (message.startsWith("/pm ")) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                session.getBasicRemote().sendText("Invalid private message. Use /pm username message");
                return;
            }
            String target = parts[1];
            String privateMsg = parts[2];
            sendPrivate(username, target, privateMsg);
            return;
        }

        // Broadcast
        broadcast(username + ": " + message, username);
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        if (username != null) {
            users.remove(username);
            broadcast("SERVER: " + username + " left the chat", null);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

    private void broadcast(String message, String sender) {
        users.forEach((name, sess) -> {
            if (!name.equals(sender)) {
                try { sess.getBasicRemote().sendText(message); } catch (IOException ignored) {}
            }
        });
        System.out.println(message);
    }

    private void sendPrivate(String from, String to, String message) throws IOException {
        Session targetSession = users.get(to);
        if (targetSession != null) {
            targetSession.getBasicRemote().sendText("[PRIVATE] " + from + " → you: " + message);
        } else {
            Session senderSession = users.get(from);
            if (senderSession != null) {
                senderSession.getBasicRemote().sendText("SERVER: User '" + to + "' not found.");
            }
        }
    }

    // -------- Main method to start server --------
    public static void main(String[] args) throws Exception {
        org.glassfish.tyrus.server.Server server =
                new org.glassfish.tyrus.server.Server("localhost", 8080, "/", null, WebSocketChat.class);

        System.out.println("WebSocket chat server started at ws://localhost:8080/chat");
        server.start();
        System.out.println("Press Enter to stop the server...");
        System.in.read();
        server.stop();
    }
}