package ChatApp;

import ChatApp.proto.ChatMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.LineUnavailableException;

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
    private Button btnIcon;

    @FXML
    private Button btnEncryptedImage;
    
    @FXML
    private Button btnFile;

    @FXML
    private Button btnVoice;

    @FXML
    private Label lblConnectStatus;

    @FXML
    private Label lblChatStatus;

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
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private VoiceHandler voiceHandler;
    private boolean isRecording = false;
    private javafx.stage.Popup iconPopup;
    private final List<String> recentIcons = new ArrayList<>();

    private record EmojiIcon(String emoji, String keywords) {}

    private static final Map<String, List<EmojiIcon>> ICON_CATEGORIES = new LinkedHashMap<>();

    static {
        ICON_CATEGORIES.put("😀", List.of(
                icon("😀", "smile happy"), icon("😄", "smile"), icon("😁", "grin"), icon("😂", "laugh"),
                icon("🤣", "lol"), icon("😊", "blush"), icon("😍", "love"), icon("🥰", "heart eyes"),
                icon("😘", "kiss"), icon("😎", "cool"), icon("🤩", "star"), icon("🥳", "party")
        ));
        ICON_CATEGORIES.put("👍", List.of(
                icon("👍", "like ok"), icon("👏", "clap"), icon("🙏", "thanks"), icon("🤝", "handshake"),
                icon("💪", "strong"), icon("👌", "perfect"), icon("✌️", "victory"), icon("🙌", "raise hands"),
                icon("🤞", "hope"), icon("🫶", "heart hands")
        ));
        ICON_CATEGORIES.put("❤️", List.of(
                icon("❤️", "heart love"), icon("💙", "blue heart"), icon("💚", "green heart"), icon("💛", "yellow heart"),
                icon("🧡", "orange heart"), icon("💜", "purple heart"), icon("🩷", "pink heart"), icon("💔", "broken heart"),
                icon("💕", "two hearts"), icon("💯", "hundred")
        ));
        ICON_CATEGORIES.put("🎉", List.of(
                icon("🎉", "party"), icon("🎊", "celebrate"), icon("🔥", "fire"), icon("⭐", "star"),
                icon("✨", "sparkles"), icon("🎈", "balloon"), icon("🥂", "cheers"), icon("🏆", "trophy"),
                icon("🎵", "music"), icon("🎮", "game")
        ));
        ICON_CATEGORIES.put("🐶", List.of(
                icon("🐶", "dog"), icon("🐱", "cat"), icon("🐼", "panda"), icon("🦊", "fox"),
                icon("🐻", "bear"), icon("🐯", "tiger"), icon("🐵", "monkey"), icon("🐸", "frog"),
                icon("🦄", "unicorn"), icon("🐧", "penguin")
        ));
    }

    private static EmojiIcon icon(String emoji, String keywords) {
        return new EmojiIcon(emoji, keywords);
    }

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
        btnIcon.setOnAction(e -> showIconPicker());
        btnEncryptedImage.setOnAction(e -> sendEncryptedImage());
        btnFile.setOnAction(e -> sendFile());
        voiceHandler = new VoiceHandler();
        btnVoice.setOnAction(e -> {
            if (!isRecording) {
                try {
                    voiceHandler.startRecording();
                    isRecording = true;
                    btnVoice.setText("⏹️");
                    btnSend.setDisable(true);
                    btnImage.setDisable(true);
                    btnFile.setDisable(true);
                    txtMessage.setDisable(true);
                    setChatStatus("● Đang thu âm...", Color.ORANGE);
                } catch (LineUnavailableException ex) {
                    showAlert("Lỗi", "Không thể bắt đầu thu âm: " + ex.getMessage());
                }
            } else {
                // stop and send
                new Thread(() -> {
                    try {
                        byte[] wav = voiceHandler.stopRecording();
                        if (wav != null && wav.length > 0) {
                            String b64 = Base64.getEncoder().encodeToString(wav);
                            String filename = "voice_" + System.currentTimeMillis() + ".wav";
                            String payload = "VOICE::" + filename + "::audio/wav::" + b64;
                            chatClient.sendMessage(payload);
                        }
                    } catch (IOException ex) {
                        Platform.runLater(() -> showAlert("Lỗi", "Không thể dừng thu âm: " + ex.getMessage()));
                    } finally {
                        Platform.runLater(() -> {
                            isRecording = false;
                            btnVoice.setText("🎤");
                            btnSend.setDisable(false);
                            btnImage.setDisable(false);
                            btnFile.setDisable(false);
                            txtMessage.setDisable(false);
                            setChatStatus("✅ Kết nối thành công", Color.GREEN);
                        });
                    }
                }).start();
            }
        });
        txtMessage.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER")) {
                sendMessage();
            }
        });

        chatPanel.setVisible(false);
        connectPanel.setVisible(true);
    }

    private void showIconPicker() {
        if (iconPopup != null && iconPopup.isShowing()) {
            iconPopup.hide();
            return;
        }

        VBox root = new VBox(10);
        root.setStyle("-fx-padding: 12; -fx-background-color: white; -fx-border-color: #dcdfe5; -fx-border-radius: 12; -fx-background-radius: 12;");
        root.setPrefWidth(320);

        Label title = new Label("Emoji");
        title.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #3a3d44;");

        TextField searchField = new TextField();
        searchField.setPromptText("Tìm emoji...");
        searchField.setStyle("-fx-background-radius: 10; -fx-padding: 8 10;");

        HBox recentRow = new HBox(6);
        recentRow.setAlignment(Pos.CENTER_LEFT);
        recentRow.setStyle("-fx-padding: 2 0 2 0;");
        Label recentLabel = new Label("Gần đây:");
        recentLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #7a7f89;");
        recentRow.getChildren().add(recentLabel);
        updateRecentRow(recentRow);

        ToggleGroup categoryGroup = new ToggleGroup();
        HBox categoryTabs = new HBox(6);
        categoryTabs.setAlignment(Pos.CENTER_LEFT);

        FlowPane iconGrid = new FlowPane();
        iconGrid.setHgap(8);
        iconGrid.setVgap(8);
        iconGrid.setPrefWrapLength(288);

        ScrollPane gridScroll = new ScrollPane(iconGrid);
        gridScroll.setFitToWidth(true);
        gridScroll.setPrefViewportHeight(230);
        gridScroll.setStyle("-fx-background-color: transparent;");

        for (String cat : ICON_CATEGORIES.keySet()) {
            ToggleButton tab = new ToggleButton(cat);
            tab.setToggleGroup(categoryGroup);
            tab.setStyle("-fx-background-radius: 16; -fx-background-color: #f1f3f7; -fx-font-size: 14;");
            tab.setPrefWidth(44);
            categoryTabs.getChildren().add(tab);
        }

        categoryGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle instanceof ToggleButton selected) {
                renderIconGrid(iconGrid, ICON_CATEGORIES.get(selected.getText()), searchField.getText(), recentRow);
            }
        });

        searchField.textProperty().addListener((obs, oldText, newText) -> {
            Toggle selected = categoryGroup.getSelectedToggle();
            if (selected instanceof ToggleButton selectedBtn) {
                renderIconGrid(iconGrid, ICON_CATEGORIES.get(selectedBtn.getText()), newText, recentRow);
            }
        });

        root.getChildren().addAll(title, searchField, recentRow, categoryTabs, gridScroll);

        iconPopup = new javafx.stage.Popup();
        iconPopup.getContent().add(root);
        iconPopup.setAutoHide(true);

        Toggle first = categoryGroup.getToggles().isEmpty() ? null : categoryGroup.getToggles().get(0);
        if (first != null) {
            categoryGroup.selectToggle(first);
        }

        if (btnIcon.getScene() != null) {
            javafx.geometry.Bounds bounds = btnIcon.localToScreen(btnIcon.getBoundsInLocal());
            double x = Math.max(8, bounds.getMinX() - 250);
            double y = bounds.getMinY() - 320;
            iconPopup.show(btnIcon.getScene().getWindow(), x, y);
        }
    }

    private void renderIconGrid(FlowPane grid, List<EmojiIcon> icons, String keyword, HBox recentRow) {
        grid.getChildren().clear();
        if (icons == null) return;

        String filter = keyword == null ? "" : keyword.trim().toLowerCase();
        for (EmojiIcon icon : icons) {
            if (!filter.isEmpty() && !icon.keywords().contains(filter) && !icon.emoji().contains(filter)) {
                continue;
            }

            Button emojiButton = new Button();
            emojiButton.setGraphic(createEmojiImageView(icon.emoji(), 24));
            emojiButton.setStyle("-fx-font-size: 20; -fx-min-width: 44; -fx-min-height: 44; -fx-background-radius: 22; -fx-background-color: #f4f6fa;");
            emojiButton.setOnMouseEntered(e -> {
                emojiButton.setScaleX(1.08);
                emojiButton.setScaleY(1.08);
                emojiButton.setStyle("-fx-font-size: 20; -fx-min-width: 44; -fx-min-height: 44; -fx-background-radius: 22; -fx-background-color: #e8ecf7;");
            });
            emojiButton.setOnMouseExited(e -> {
                emojiButton.setScaleX(1.0);
                emojiButton.setScaleY(1.0);
                emojiButton.setStyle("-fx-font-size: 20; -fx-min-width: 44; -fx-min-height: 44; -fx-background-radius: 22; -fx-background-color: #f4f6fa;");
            });

            emojiButton.setOnAction(e -> {
                if (chatClient != null) {
                    chatClient.sendMessage("ICON::" + icon.emoji());
                }
                rememberRecent(icon.emoji());
                updateRecentRow(recentRow);
                iconPopup.hide();
            });

            grid.getChildren().add(emojiButton);
        }
    }

    private void insertEmojiAtCaret(String emoji) {
        int caret = Math.max(0, txtMessage.getCaretPosition());
        txtMessage.insertText(caret, emoji);
        txtMessage.requestFocus();
        txtMessage.positionCaret(caret + emoji.length());
    }

    private void rememberRecent(String emoji) {
        recentIcons.remove(emoji);
        recentIcons.add(0, emoji);
        if (recentIcons.size() > 8) {
            recentIcons.remove(recentIcons.size() - 1);
        }
    }

    private void updateRecentRow(HBox recentRow) {
        while (recentRow.getChildren().size() > 1) {
            recentRow.getChildren().remove(1);
        }

        if (recentIcons.isEmpty()) {
            Label empty = new Label("chưa có");
            empty.setStyle("-fx-font-size: 11; -fx-text-fill: #a1a5ad;");
            recentRow.getChildren().add(empty);
            return;
        }

        for (String recent : recentIcons) {
            Button quick = new Button();
            quick.setGraphic(createEmojiImageView(recent, 18));
            quick.setStyle("-fx-min-width: 30; -fx-min-height: 30; -fx-background-radius: 15; -fx-background-color: #f4f6fa;");
            quick.setOnAction(e -> {
                if (chatClient != null) {
                    chatClient.sendMessage("ICON::" + recent);
                }
                iconPopup.hide();
            });
            recentRow.getChildren().add(quick);
        }
    }

    private ImageView createEmojiImageView(String emoji, double size) {
        String imageUrl = toTwemojiUrl(emoji);
        Image image = new Image(imageUrl, size, size, true, true, true);
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        return imageView;
    }

    private String toTwemojiUrl(String emoji) {
        StringBuilder hex = new StringBuilder();
        int[] cps = emoji.codePoints().toArray();
        for (int i = 0; i < cps.length; i++) {
            if (i > 0) {
                hex.append("-");
            }
            hex.append(Integer.toHexString(cps[i]));
        }
        return "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/" + hex + ".png";
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
                        setConnectStatus("✅ Kết nối thành công", Color.GREEN);
                        setChatStatus("✅ Kết nối thành công", Color.GREEN);
                    });
                }
            } catch (IOException ex) {
                Platform.runLater(() -> {
                    showAlert("Lỗi", "Không thể kết nối đến server: " + ex.getMessage());
                    setConnectStatus("❌ Kết nối thất bại", Color.RED);
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

        // Icon message
        if (message.getContent() != null && message.getContent().startsWith("ICON::")) {
            String[] parts = message.getContent().split("::", 2);
            if (parts.length == 2) {
                String emoji = parts[1];

                VBox container = new VBox();
                container.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                HBox messageRow = new HBox(5);
                messageRow.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Text senderText = new Text(message.getSender());
                senderText.setStyle(isOwn ? "-fx-font-weight: bold; -fx-fill: #0066cc;" : "-fx-font-weight: bold; -fx-fill: #00aa00;");
                Text timeText = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
                timeText.setStyle("-fx-font-size: 10; -fx-fill: #999999;");
                messageRow.getChildren().addAll(senderText, timeText);

                Node emojiNode;
                try {
                    emojiNode = createEmojiImageView(emoji, 40);
                } catch (Exception ex) {
                    Label fallback = new Label(emoji);
                    fallback.setStyle("-fx-font-size: 36;");
                    emojiNode = fallback;
                }

                TextFlow contentFlow = new TextFlow(emojiNode);
                contentFlow.setStyle(isOwn ? ownBg : otherBg);

                container.getChildren().addAll(messageRow, contentFlow);
                textFlow.getChildren().add(container);
                return textFlow;
            }
        }

        // File message
        if (message.getContent() != null && message.getContent().startsWith("FILE::")) {
            String[] parts = message.getContent().split("::", 4);
            if (parts.length == 4) {
                String filename = parts[1];
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
        // Voice message
        if (message.getContent() != null && message.getContent().startsWith("VOICE::")) {
            String[] parts = message.getContent().split("::", 4);
            if (parts.length == 4) {
                String filename = parts[1];
                String b64 = parts[3];

                VBox containerVoice = new VBox();
                containerVoice.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

                HBox messageRowVoice = new HBox(8);
                messageRowVoice.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Text senderTextVoice = new Text(message.getSender());
                senderTextVoice.setStyle(isOwn ? "-fx-font-weight: bold; -fx-fill: #0066cc;" : "-fx-font-weight: bold; -fx-fill: #00aa00;");
                Text timeTextVoice = new Text(" [" + timeFormat.format(new Date(message.getTimestamp())) + "]");
                timeTextVoice.setStyle("-fx-font-size: 10; -fx-fill: #999999;");
                messageRowVoice.getChildren().addAll(senderTextVoice, timeTextVoice);

                HBox voiceBox = new HBox(6);
                voiceBox.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                Label lbl = new Label(filename);
                lbl.setStyle("-fx-border-color: #cccccc; -fx-padding: 6; -fx-background-radius: 4; -fx-border-radius: 4;");
                Button btnPlay = new Button("▶ Phát");
                btnPlay.setOnAction(e -> {
                    byte[] audioBytes;
                    try {
                        audioBytes = Base64.getDecoder().decode(b64);
                    } catch (IllegalArgumentException ex) {
                        showAlert("Lỗi", "Dữ liệu âm thanh không hợp lệ");
                        return;
                    }
                    // play in background thread
                    new Thread(() -> {
                        try {
                            voiceHandler.play(audioBytes);
                        } catch (Exception ex) {
                            Platform.runLater(() -> showAlert("Lỗi", "Không thể phát âm thanh: " + ex.getMessage()));
                        }
                    }, "Voice-Play-Thread").start();
                });

                voiceBox.getChildren().addAll(lbl, btnPlay);
                TextFlow contentFlow = new TextFlow(voiceBox);
                contentFlow.setStyle(isOwn ? ownBg : otherBg);

                containerVoice.getChildren().addAll(messageRowVoice, contentFlow);
                textFlow.getChildren().add(containerVoice);
                return textFlow;
            }
        }
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
                setConnectStatus("❌ Đã ngắt kết nối", Color.RED);
                setChatStatus("❌ Mất kết nối", Color.RED);
            }
        });
    }

    private void setConnectStatus(String text, Color color) {
        if (lblConnectStatus != null) {
            lblConnectStatus.setText(text);
            lblConnectStatus.setTextFill(color);
        }
    }

    private void setChatStatus(String text, Color color) {
        if (lblChatStatus != null) {
            lblChatStatus.setText(text);
            lblChatStatus.setTextFill(color);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
