package ChatApp;

import ChatApp.proto.ChatMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;

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
    private Button btnImage;

    @FXML
    private Button btnEncryptedImage;
    
    @FXML
    private Button btnFile;

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
        btnImage.setOnAction(e -> sendImage());
        btnEncryptedImage.setOnAction(e -> sendEncryptedImage());
        btnFile.setOnAction(e -> sendFile());
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

    private void sendImage() {
        if (chatClient == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh để gửi");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = chooser.showOpenDialog(btnImage.getScene().getWindow());
        if (file == null) return;

        // Read file and send as base64-embedded content to avoid changing proto
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            String mime = Files.probeContentType(file.toPath());
            if (mime == null) mime = "application/octet-stream";
            String b64 = Base64.getEncoder().encodeToString(data);
            String payload = "IMAGE::" + file.getName() + "::" + mime + "::" + b64;
            chatClient.sendMessage(payload);
        } catch (IOException ex) {
            showAlert("Lỗi", "Không thể đọc file ảnh: " + ex.getMessage());
        }
    }

    /**
     * Gửi ảnh mã hóa AES-256 với mật khẩu do người dùng nhập.
     * Người nhận phải nhập đúng mật khẩu mới xem được ảnh.
     */
    private void sendEncryptedImage() {
        if (chatClient == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh mã hóa để gửi");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        File file = chooser.showOpenDialog(btnEncryptedImage.getScene().getWindow());
        if (file == null) return;

        // Prompt for password
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Mã hóa ảnh");
        passwordDialog.setHeaderText("Nhập mật khẩu để mã hóa ảnh");
        passwordDialog.setContentText("Mật khẩu:");
        Optional<String> passwordResult = passwordDialog.showAndWait();
        if (passwordResult.isEmpty() || passwordResult.get().trim().isEmpty()) {
            showAlert("Thông báo", "Bạn chưa nhập mật khẩu. Hủy gửi ảnh mã hóa.");
            return;
        }
        String password = passwordResult.get().trim();

        try {
            byte[] rawData = Files.readAllBytes(file.toPath());
            String mime = Files.probeContentType(file.toPath());
            if (mime == null) mime = "application/octet-stream";

            // Encrypt image bytes with AES-256
            byte[] encryptedData = ImageEncryptor.encrypt(rawData, password);
            String b64 = Base64.getEncoder().encodeToString(encryptedData);

            // Payload format: ENCRYPTED_IMAGE::filename::mime::base64EncryptedData
            String payload = "ENCRYPTED_IMAGE::" + file.getName() + "::" + mime + "::" + b64;
            chatClient.sendMessage(payload);
        } catch (IOException ex) {
            showAlert("Lỗi", "Không thể đọc file ảnh: " + ex.getMessage());
        } catch (Exception ex) {
            showAlert("Lỗi", "Mã hóa ảnh thất bại: " + ex.getMessage());
        }
    }

    private void sendFile() {
        if (chatClient == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn file để gửi");
        File file = chooser.showOpenDialog(btnFile.getScene().getWindow());
        if (file == null) return;

        try {
            long fileSize = Files.size(file.toPath());
            // Limit to 10 MB for simplicity; adjust as needed
            long maxBytes = 10L * 1024L * 1024L;
            if (fileSize > maxBytes) {
                showAlert("Lỗi", "Kích thước file quá lớn (>10MB). Vui lòng chọn file nhỏ hơn.");
                return;
            }

            byte[] data = Files.readAllBytes(file.toPath());
            String mime = Files.probeContentType(file.toPath());
            if (mime == null) mime = "application/octet-stream";
            String b64 = Base64.getEncoder().encodeToString(data);
            String payload = "FILE::" + file.getName() + "::" + mime + "::" + b64;
            chatClient.sendMessage(payload);
        } catch (IOException ex) {
            showAlert("Lỗi", "Không thể đọc file: " + ex.getMessage());
        }
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

        // System messages
        if (message.getSender().equals("System")) {
            Text systemMsg = new Text(message.getContent());
            systemMsg.setStyle("-fx-font-style: italic; -fx-fill: #888888;");
            textFlow.getChildren().add(systemMsg);
            textFlow.setStyle("-fx-padding: 8px; -fx-text-alignment: center;");
            return textFlow;
        }

        boolean isOwn = message.getSender().equals(chatClient.getUsername());
        String ownBg = "-fx-padding: 8px; -fx-background-color: #0066cc; -fx-border-radius: 5; -fx-background-radius: 5;";
        String otherBg = "-fx-padding: 8px; -fx-background-color: #e8e8e8; -fx-border-radius: 5; -fx-background-radius: 5;";

        // Encrypted image message
        if (message.getContent() != null && message.getContent().startsWith("ENCRYPTED_IMAGE::")) {
            String[] parts = message.getContent().split("::", 4);
            if (parts.length == 4) {
                String encFilename = parts[1];
                String encB64 = parts[3];

                VBox container = new VBox(5);
                container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                // Sender + time row
                HBox messageRow = new HBox(5);
                messageRow.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Text senderText = new Text(message.getSender());
                senderText.setStyle(isOwn ? "-fx-font-weight: bold; -fx-fill: #0066cc;" : "-fx-font-weight: bold; -fx-fill: #00aa00;");
                Text timeText = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
                timeText.setStyle("-fx-font-size: 10; -fx-fill: #999999;");
                messageRow.getChildren().addAll(senderText, timeText);

                // Locked image placeholder
                VBox lockedBox = new VBox(8);
                lockedBox.setAlignment(Pos.CENTER);
                lockedBox.setStyle("-fx-padding: 15; -fx-background-color: #2c2c2c; -fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #ff6600; -fx-border-width: 2;");
                lockedBox.setPrefWidth(300);
                lockedBox.setPrefHeight(180);

                Label lockIcon = new Label("🔒");
                lockIcon.setStyle("-fx-font-size: 36;");

                Label lockLabel = new Label("Ảnh mã hóa");
                lockLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ff9933;");

                Label fileLabel = new Label(encFilename);
                fileLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #cccccc;");

                Button btnUnlock = new Button("🔓 Mở khóa xem ảnh");
                btnUnlock.setStyle("-fx-padding: 8 16; -fx-font-size: 12; -fx-background-color: #ff6600; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");

                btnUnlock.setOnAction(e -> {
                    TextInputDialog pwDialog = new TextInputDialog();
                    pwDialog.setTitle("Giải mã ảnh");
                    pwDialog.setHeaderText("Nhập mật khẩu để xem ảnh");
                    pwDialog.setContentText("Mật khẩu:");
                    Optional<String> pwResult = pwDialog.showAndWait();
                    if (pwResult.isEmpty() || pwResult.get().trim().isEmpty()) return;

                    try {
                        byte[] encryptedBytes = Base64.getDecoder().decode(encB64);
                        byte[] decryptedBytes = ImageEncryptor.decrypt(encryptedBytes, pwResult.get().trim());
                        Image img = new Image(new ByteArrayInputStream(decryptedBytes));
                        if (img.isError()) {
                            showAlert("Sai mật khẩu", "Mật khẩu không đúng hoặc dữ liệu bị hỏng!");
                            return;
                        }
                        ImageView iv = new ImageView(img);
                        iv.setPreserveRatio(true);
                        iv.setFitWidth(300);

                        // Replace locked box with the decrypted image
                        lockedBox.getChildren().clear();
                        lockedBox.setStyle("-fx-padding: 5; -fx-background-color: transparent; -fx-border-color: transparent;");
                        lockedBox.setPrefHeight(-1);
                        Label unlockedLabel = new Label("🔓 Đã giải mã");
                        unlockedLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #00cc00;");
                        lockedBox.getChildren().addAll(iv, unlockedLabel);
                    } catch (Exception ex) {
                        showAlert("Sai mật khẩu", "Mật khẩu không đúng hoặc dữ liệu bị hỏng!\n" + ex.getMessage());
                    }
                });

                lockedBox.getChildren().addAll(lockIcon, lockLabel, fileLabel, btnUnlock);

                container.getChildren().addAll(messageRow, lockedBox);
                textFlow.getChildren().add(container);
                return textFlow;
            }
        }

        // Image message
        if (message.getContent() != null && message.getContent().startsWith("IMAGE::")) {
            String[] parts = message.getContent().split("::", 4);
            if (parts.length == 4) {
                try {
                    byte[] imgBytes = Base64.getDecoder().decode(parts[3]);
                    Image img = new Image(new ByteArrayInputStream(imgBytes));
                    ImageView iv = new ImageView(img);
                    iv.setPreserveRatio(true);
                    iv.setFitWidth(300);

                    VBox container = new VBox();
                    container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                    HBox messageRow = new HBox(5);
                    messageRow.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    Text senderText = new Text(message.getSender());
                    senderText.setStyle(isOwn ? "-fx-font-weight: bold; -fx-fill: #0066cc;" : "-fx-font-weight: bold; -fx-fill: #00aa00;");
                    Text timeText = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
                    timeText.setStyle("-fx-font-size: 10; -fx-fill: #999999;");
                    messageRow.getChildren().addAll(senderText, timeText);

                    TextFlow contentFlow = new TextFlow(iv);
                    contentFlow.setStyle(isOwn ? ownBg : otherBg);

                    container.getChildren().addAll(messageRow, contentFlow);
                    textFlow.getChildren().add(container);
                    return textFlow;
                } catch (IllegalArgumentException ex) {
                    // fall through to show as broken text
                }
            }
        }

        // File message
        if (message.getContent() != null && message.getContent().startsWith("FILE::")) {
            String[] parts = message.getContent().split("::", 4);
            if (parts.length == 4) {
                String filename = parts[1];
                String mime = parts[2];
                String b64 = parts[3];

                VBox containerFile = new VBox();
                containerFile.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                HBox messageRowFile = new HBox(8);
                messageRowFile.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Text senderTextFile = new Text(message.getSender());
                senderTextFile.setStyle(isOwn ? "-fx-font-weight: bold; -fx-fill: #0066cc;" : "-fx-font-weight: bold; -fx-fill: #00aa00;");
                Text timeTextFile = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
                timeTextFile.setStyle("-fx-font-size: 10; -fx-fill: #999999;");
                messageRowFile.getChildren().addAll(senderTextFile, timeTextFile);

                HBox fileBox = new HBox(6);
                fileBox.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Label lbl = new Label(filename);
                lbl.setStyle("-fx-border-color: #cccccc; -fx-padding: 6; -fx-background-radius: 4; -fx-border-radius: 4;");
                Button btnDownload = new Button("Tải");
                btnDownload.setOnAction(e -> {
                    try {
                        byte[] fileBytes = Base64.getDecoder().decode(b64);
                        FileChooser saver = new FileChooser();
                        saver.setInitialFileName(filename);
                        File target = saver.showSaveDialog(btnDownload.getScene().getWindow());
                        if (target != null) {
                            Files.write(target.toPath(), fileBytes);
                        }
                    } catch (IllegalArgumentException | IOException ex) {
                        showAlert("Lỗi", "Không thể lưu file: " + ex.getMessage());
                    }
                });

                fileBox.getChildren().addAll(lbl, btnDownload);
                TextFlow contentFlow = new TextFlow(fileBox);
                contentFlow.setStyle(isOwn ? ownBg : otherBg);

                containerFile.getChildren().addAll(messageRowFile, contentFlow);
                textFlow.getChildren().add(containerFile);
                return textFlow;
            }
        }

        // Plain text message
        VBox container = new VBox();
        container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        HBox messageRow = new HBox(5);
        messageRow.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        Text senderText = new Text(message.getSender());
        senderText.setStyle(isOwn ? "-fx-font-weight: bold; -fx-fill: #0066cc;" : "-fx-font-weight: bold; -fx-fill: #00aa00;");
        Text timeText = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
        timeText.setStyle("-fx-font-size: 10; -fx-fill: #999999;");
        messageRow.getChildren().addAll(senderText, timeText);

        Text contentText = new Text(message.getContent());
        contentText.setWrappingWidth(300);
        contentText.setStyle(isOwn ? "-fx-padding: 5px; -fx-fill: white;" : "-fx-padding: 5px; -fx-fill: black;");
        TextFlow contentFlow = new TextFlow(contentText);
        contentFlow.setStyle(isOwn ? ownBg : otherBg);

        container.getChildren().addAll(messageRow, contentFlow);
        textFlow.getChildren().add(container);
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
