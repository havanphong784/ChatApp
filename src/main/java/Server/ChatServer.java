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

}

