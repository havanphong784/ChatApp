package Server;

import ChatApp.proto.ChatMessage;
import com.google.protobuf.ByteString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;
    private String username;
    private final ChatServer server;
    private DataInputStream input;
    private DataOutputStream output;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());

            int messageSize = input.readInt();
            byte[] messageBuffer = new byte[messageSize];
            input.readFully(messageBuffer);
            ChatMessage firstMessage = ChatMessage.parseFrom(ByteString.copyFrom(messageBuffer));

            if (firstMessage.getType() == ChatMessage.MessageType.JOIN) {
                this.username = firstMessage.getSender();
                server.addClient(username, this);

                ChatMessage joinNotification = ChatMessage.newBuilder()
                        .setType(ChatMessage.MessageType.MESSAGE)
                        .setSender("System")
                        .setContent(username + " đã tham gia trò chuyện")
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                server.broadcastMessage(joinNotification);

                sendUserList();

                while (true) {
                    try {
                        messageSize = input.readInt();
                        messageBuffer = new byte[messageSize];
                        input.readFully(messageBuffer);

                        ChatMessage message = ChatMessage.parseFrom(ByteString.copyFrom(messageBuffer));

                        if (message.getType() == ChatMessage.MessageType.LEAVE) {
                            break;
                        } else if (message.getType() == ChatMessage.MessageType.MESSAGE) {
                            ChatMessage broadcastMsg = ChatMessage.newBuilder()
                                    .setType(ChatMessage.MessageType.MESSAGE)
                                    .setSender(message.getSender())
                                    .setContent(message.getContent())
                                    .setTimestamp(System.currentTimeMillis())
                                    .build();
                            server.broadcastMessage(broadcastMsg);
                        }
                    } catch (EOFException e) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi ClientHandler: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    public void sendMessage(ChatMessage message) {
        try {
            byte[] data = message.toByteArray();
            output.writeInt(data.length);
            output.write(data);
            output.flush();
        } catch (IOException e) {
            System.err.println("Lỗi gửi thông báo: " + e.getMessage());
        }
    }

    private void sendUserList() {
        List<String> users = server.getConnectedUsers();
        ChatMessage userListMsg = ChatMessage.newBuilder()
                .setType(ChatMessage.MessageType.USER_LIST)
                .addAllUsers(users)
                .setTimestamp(System.currentTimeMillis())
                .build();
        sendMessage(userListMsg);
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.removeClient(username);
                ChatMessage leaveMsg = ChatMessage.newBuilder()
                        .setType(ChatMessage.MessageType.MESSAGE)
                        .setSender("System")
                        .setContent(username + " đã rời khỏi trò chuyện")
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                server.broadcastMessage(leaveMsg);
            }
            socket.close();
        } catch (IOException e) {
            System.err.println("Lỗi cleanup: " + e.getMessage());
        }
    }
}


