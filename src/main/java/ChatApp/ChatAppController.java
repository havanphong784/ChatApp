package ChatApp;

import ChatApp.proto.ChatMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChatAppController implements ChatClient.MessageListener {
    @FXML
    private VBox sideBar;

    @FXML
    private Label lblClient;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private VBox chatArea;

    @FXML
    private TextField txtMessage;

    @FXML
    private Button btnSend;

    @FXML
    private Label lblStatus;

    @FXML
    private VBox userListContainer;

    @FXML
    private ListView<String> userListView;

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtServerHost;

    @FXML
    private TextField txtServerPort;

    @FXML
    private Button btnConnect;

    @FXML
    private VBox connectPanel;

    @FXML
    private BorderPane chatPanel;

    private ChatClient chatClient;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    @FXML
    public void initialize() {
        chatClient = new ChatClient();
        chatClient.addMessageListener(this);

        txtServerHost.setText("localhost");
        txtServerPort.setText("4444");
        txtUsername.setText("User_" + System.currentTimeMillis() % 1000);

        btnConnect.setOnAction(e -> connectToServer());
        btnSend.setOnAction(e -> sendMessage());
        txtMessage.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                sendMessage();
            }
        });

        chatPanel.setVisible(false);
        connectPanel.setVisible(true);
    }

    @FXML
    private void connectToServer() {
        String host = txtServerHost.getText().trim();
        String username = txtUsername.getText().trim();
        int port;

        if (host.isEmpty() || username.isEmpty()) {
            showAlert("Lỗi", "Vui lòng nhập tên người dùng và địa chỉ server");
            return;
        }

        try {
            port = Integer.parseInt(txtServerPort.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Cổng phải là một số");
            return;
        }

        new Thread(() -> {
            try {
                if (chatClient.connect(host, port, username)) {
                    Platform.runLater(() -> {
                        lblClient.setText("👤 " + username);
                        connectPanel.setVisible(false);
                        chatPanel.setVisible(true);
                        lblStatus.setText("✅ Kết nối thành công");
                        lblStatus.setTextFill(Color.GREEN);
                    });
                }
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Không thể kết nối đến server: " + ex.getMessage());
                    lblStatus.setText("❌ Kết nối thất bại");
                    lblStatus.setTextFill(Color.RED);
                });
            }
        }).start();
    }

    private void sendMessage() {
        String message = txtMessage.getText().trim();
        if (message.isEmpty()) return;

        txtMessage.clear();
        chatClient.sendMessage(message);
    }

    @Override
    public void onMessageReceived(ChatMessage message) {
        Platform.runLater(() -> {
            TextFlow messageBox = createMessageBox(message);
            chatArea.getChildren().add(messageBox);
            scrollPane.setVvalue(1.0);
        });
    }

    private TextFlow createMessageBox(ChatMessage message) {
        TextFlow textFlow = new TextFlow();
        textFlow.setStyle("-fx-padding: 8px;");

        if (message.getSender().equals("System")) {
            // System message
            Text systemMsg = new Text(message.getContent());
            systemMsg.setStyle("-fx-font-style: italic; -fx-fill: #888888;");
            textFlow.getChildren().add(systemMsg);
            textFlow.setStyle("-fx-padding: 8px; -fx-text-alignment: center;");
        } else if (message.getSender().equals(chatClient.getUsername())) {
            // Own message (right aligned)
            VBox container = new VBox();
            container.setAlignment(Pos.CENTER_RIGHT);

            HBox messageRow = new HBox(5);
            messageRow.setAlignment(Pos.CENTER_RIGHT);

            Text senderText = new Text(message.getSender());
            senderText.setStyle("-fx-font-weight: bold; -fx-fill: #0066cc;");

            Text timeText = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
            timeText.setStyle("-fx-font-size: 10; -fx-fill: #999999;");

            messageRow.getChildren().addAll(senderText, timeText);

            Text contentText = new Text(message.getContent());
            contentText.setStyle("-fx-padding: 5px; -fx-fill: white;");
            contentText.setWrappingWidth(300);

            TextFlow contentFlow = new TextFlow(contentText);
            contentFlow.setStyle("-fx-padding: 8px; -fx-background-color: #0066cc; -fx-border-radius: 5; -fx-background-radius: 5;");

            container.getChildren().addAll(messageRow, contentFlow);
            textFlow.getChildren().add(container);
        } else {
            // Other user's message (left aligned)
            VBox container = new VBox();
            container.setAlignment(Pos.CENTER_LEFT);

            HBox messageRow = new HBox(5);
            messageRow.setAlignment(Pos.CENTER_LEFT);

            Text senderText = new Text(message.getSender());
            senderText.setStyle("-fx-font-weight: bold; -fx-fill: #00aa00;");

            Text timeText = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
            timeText.setStyle("-fx-font-size: 10; -fx-fill: #999999;");

            messageRow.getChildren().addAll(senderText, timeText);

            Text contentText = new Text(message.getContent());
            contentText.setStyle("-fx-padding: 5px; -fx-fill: black;");
            contentText.setWrappingWidth(300);

            TextFlow contentFlow = new TextFlow(contentText);
            contentFlow.setStyle("-fx-padding: 8px; -fx-background-color: #e8e8e8; -fx-border-radius: 5; -fx-background-radius: 5;");

            container.getChildren().addAll(messageRow, contentFlow);
            textFlow.getChildren().add(container);
        }

        return textFlow;
    }

    @Override
    public void onUserListUpdated(List<String> users) {
        Platform.runLater(() -> {
            userListView.getItems().setAll(users);
        });
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        Platform.runLater(() -> {
            if (!connected) {
                showAlert("Thông báo", "Bạn đã bị ngắt kết nối");
                connectPanel.setVisible(true);
                chatPanel.setVisible(false);
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
