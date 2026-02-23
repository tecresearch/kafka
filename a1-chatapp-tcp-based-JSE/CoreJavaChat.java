import java.io.*;
import java.net.*;
import java.util.Scanner;

// Run this file as Server first, then multiple Clients
public class CoreJavaChat {

    // Change to "server" or "client" argument
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

            String name = in.readLine(); // first line: username
            broadcast(name + " joined the chat");

            String message;
            while ((message = in.readLine()) != null) {
                broadcast(name + ": " + message);
            }
        } catch (Exception e) {
            // client disconnected
        }
    }

    // Simple broadcast to all clients
    private static final java.util.List<PrintWriter> clients = new java.util.ArrayList<>();
    private static synchronized void broadcast(String message) {
        System.out.println(message);
        for (PrintWriter out : clients) {
            out.println(message);
        }
    }

    // ---------------- CLIENT ----------------
    private static void startClient() throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();

        Socket socket = new Socket("localhost", 5000);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Send username to server
        out.println(username);

        // Thread to read messages from server
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println(msg);
                }
            } catch (Exception e) { }
        }).start();

        // Send messages to server
        while (true) {
            String msg = scanner.nextLine();
            out.println(msg);
        }
    }
}