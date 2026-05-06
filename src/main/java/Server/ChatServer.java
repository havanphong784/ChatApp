package Server;

import ChatApp.proto.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private final Map<String, ClientHandler> clients;

    public ChatServer() {
        this.clients = new ConcurrentHashMap<>();
    }

    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[SERVER] " + username + " đã kết nối. Tổng: " + clients.size());
    }

    public void removeClient(String username) {
        clients.remove(username);
        System.out.println("[SERVER] " + username + " đã ngắt kết nối. Tổng: " + clients.size());
    }

    public void broadcastMessage(ChatMessage message) {
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(message);
        }
    }

    public List<String> getConnectedUsers() {
        return new ArrayList<>(clients.keySet());
    }

    /**
     * Send a message to a specific user by username.
     * Used for video call signaling (request, accept, reject, end).
     */
    public void sendToUser(String username, ChatMessage message) {
        ClientHandler handler = clients.get(username);
        if (handler != null) {
            handler.sendMessage(message);
        }
    }

    /**
     * Get the IP address of a connected client.
     */
    public String getClientAddress(String username) {
        ClientHandler handler = clients.get(username);
        if (handler != null) {
            return handler.getClientAddress();
        }
        return null;
    }

}

