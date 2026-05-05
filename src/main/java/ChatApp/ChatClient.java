package ChatApp;

import ChatApp.proto.ChatMessage;
import com.google.protobuf.ByteString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatClient {
    private String username;
    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;
    private final List<MessageListener> listeners;
    private boolean connected = false;

    public interface MessageListener {
        void onMessageReceived(ChatMessage message);

        void onUserListUpdated(List<String> users);

        void onConnectionStatusChanged(boolean connected);
    }

    public ChatClient() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public boolean connect(String serverHost, int serverPort, String username) throws IOException {
        this.username = username;
        this.socket = new Socket(serverHost, serverPort);
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());

        // Send JOIN message
        ChatMessage joinMessage = ChatMessage.newBuilder()
                .setType(ChatMessage.MessageType.JOIN)
                .setSender(username)
                .setTimestamp(System.currentTimeMillis())
                .build();

        sendMessage(joinMessage);
        this.connected = true;
        notifyConnectionStatusChanged(true);

        // Start listening for messages
        startListening();
        return true;
    }

    public void sendMessage(String content) {
        if (!connected) return;

        ChatMessage message = ChatMessage.newBuilder()
                .setType(ChatMessage.MessageType.MESSAGE)
                .setSender(username)
                .setContent(content)
                .setTimestamp(System.currentTimeMillis())
                .build();

        sendMessage(message);
    }

    private void sendMessage(ChatMessage message) {
        try {
            byte[] data = message.toByteArray();
            output.writeInt(data.length);
            output.write(data);
            output.flush();
        } catch (IOException e) {
            System.err.println("Lỗi gửi tin nhắn: " + e.getMessage());
            disconnect();
        }
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (connected) {
                    int messageSize = input.readInt();
                    byte[] messageBuffer = new byte[messageSize];
                    input.readFully(messageBuffer);

                    ChatMessage message = ChatMessage.parseFrom(ByteString.copyFrom(messageBuffer));
                    handleMessage(message);
                }
            } catch (EOFException e) {
                if (connected) {
                    disconnect();
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("Lỗi nhận tin nhắn: " + e.getMessage());
                    disconnect();
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleMessage(ChatMessage message) {
        if (message.getType() == ChatMessage.MessageType.USER_LIST) {
            notifyUserListUpdated(message.getUsersList());
        } else {
            notifyMessageReceived(message);
        }
    }

    public void disconnect() {
        try {
            connected = false;
            if (socket != null && !socket.isClosed()) {
                ChatMessage leaveMessage = ChatMessage.newBuilder()
                        .setType(ChatMessage.MessageType.LEAVE)
                        .setSender(username)
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                sendMessage(leaveMessage);
                socket.close();
            }
            notifyConnectionStatusChanged(false);
        } catch (IOException e) {
            System.err.println("Lỗi khi ngắt kết nối: " + e.getMessage());
        }
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    private void notifyMessageReceived(ChatMessage message) {
        for (MessageListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyUserListUpdated(List<String> users) {
        for (MessageListener listener : listeners) {
            listener.onUserListUpdated(users);
        }
    }

    private void notifyConnectionStatusChanged(boolean connected) {
        for (MessageListener listener : listeners) {
            listener.onConnectionStatusChanged(connected);
        }
    }

    public String getUsername() {
        return username;
    }
}


