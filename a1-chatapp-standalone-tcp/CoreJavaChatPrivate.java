import java.io.*;
import java.net.*;
import java.util.*;

public class CoreJavaChatPrivate {

    // Map of username → client PrintWriter
    private static final Map<String, PrintWriter> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java CoreJavaChat server|client");
            return;
        }
        if (args[0].equalsIgnoreCase("server")) {
            startServer();
        } else {
            startClient();
        }
    }

    // ---------------- SERVER ----------------
    private static void startServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Chat server started on port 5000...");
        while (true) {
            Socket client = serverSocket.accept();
            new Thread(() -> handleClient(client)).start();
        }
    }

    private static void handleClient(Socket client) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);

            // First line: username
            String name = in.readLine();
            synchronized (clients) {
                if (clients.containsKey(name)) {
                    out.println("SERVER: Username already taken. Disconnecting...");
                    client.close();
                    return;
                }
                clients.put(name, out);
            }

            broadcast("SERVER: " + name + " joined the chat", null);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/users")) {
                    listUsers(out);
                } else if (message.startsWith("/pm ")) {
                    String[] parts = message.split(" ", 3);
                    if (parts.length >= 3) {
                        String targetUser = parts[1];
                        String privateMsg = parts[2];
                        sendPrivate(name, targetUser, privateMsg);
                    } else {
                        out.println("SERVER: Invalid private message. Use /pm username message");
                    }
                } else {
                    broadcast(name + ": " + message, name);
                }
            }
        } catch (Exception e) {
            // client disconnected
        } finally {
            removeClient(client);
        }
    }

    private static void removeClient(Socket client) {
        synchronized (clients) {
            String disconnectedUser = null;
            for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                if (entry.getValue() != null) {
                    try {
                        // We cannot match socket directly; just remove empty writer
                        if (entry.getValue().checkError()) {
                            disconnectedUser = entry.getKey();
                        }
                    } catch (Exception ignored) {}
                }
            }
            if (disconnectedUser != null) {
                clients.remove(disconnectedUser);
                broadcast("SERVER: " + disconnectedUser + " left the chat", null);
            }
        }
    }

    private static void broadcast(String message, String sender) {
        System.out.println(message);
        synchronized (clients) {
            for (Map.Entry<String, PrintWriter> entry : clients.entrySet()) {
                String user = entry.getKey();
                PrintWriter out = entry.getValue();
                if (sender == null || !user.equals(sender)) {
                    out.println(message);
                }
            }
        }
    }

    private static void sendPrivate(String from, String to, String message) {
        PrintWriter out;
        synchronized (clients) {
            out = clients.get(to);
        }
        if (out != null) {
            out.println("[PRIVATE] " + from + " → you: " + message);
        } else {
            PrintWriter senderOut = clients.get(from);
            if (senderOut != null) {
                senderOut.println("SERVER: User '" + to + "' not found.");
            }
        }
    }

    private static void listUsers(PrintWriter out) {
        StringBuilder sb = new StringBuilder("ONLINE USERS: ");
        synchronized (clients) {
            for (String user : clients.keySet()) {
                sb.append(user).append(" ");
            }
        }
        out.println(sb.toString());
    }

    // ---------------- CLIENT ----------------
    private static void startClient() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        Socket socket = new Socket("localhost", 5000);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        out.println(username); // send username to server

        // Thread to read messages from server
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception e) {}
        }).start();

        // Send messages to server
        System.out.println("Type messages to chat.");
        System.out.println("Commands:");
        System.out.println("/users                 → list online users");
        System.out.println("/pm username message   → send private message");

        while (true) {
            String msg = scanner.nextLine();
            out.println(msg);
        }
    }
}