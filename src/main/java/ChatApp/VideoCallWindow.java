package ChatApp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * A separate JavaFX window for an active video call.
 * Displays the remote user's video feed (large) and the local webcam preview (small, overlay).
 * Includes a "Kết thúc cuộc gọi" button.
 */
public class VideoCallWindow {

    public interface VideoCallWindowListener {
        void onEndCallClicked();
    }

    private final Stage stage;
    private final ImageView remoteView;
    private final ImageView localView;
    private final Label lblStatus;
    private final Label lblCallerInfo;
    private VideoCallWindowListener windowListener;

    public VideoCallWindow(String localUsername, String remoteUsername) {
        stage = new Stage();
        stage.setTitle("📹 Video Call - " + localUsername + " ↔ " + remoteUsername);
        stage.setResizable(true);
        stage.setMinWidth(520);
        stage.setMinHeight(440);

        // Root layout
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setAlignment(Pos.CENTER);
        root.setSpacing(0);

        // ═══════ TOP BAR ═══════
        HBox topBar = new HBox(10);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 15, 10, 15));
        topBar.setStyle("-fx-background-color: linear-gradient(to right, #16213e, #0f3460);");

        Label callIcon = new Label("📹");
        callIcon.setStyle("-fx-font-size: 20;");

        lblCallerInfo = new Label(localUsername + "  ↔  " + remoteUsername);
        lblCallerInfo.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #e0e0e0;");

        lblStatus = new Label("● Đang kết nối...");
        lblStatus.setStyle("-fx-font-size: 12; -fx-text-fill: #ffc107;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(callIcon, lblCallerInfo, spacer, lblStatus);

        // ═══════ VIDEO AREA ═══════
        StackPane videoContainer = new StackPane();
        videoContainer.setStyle("-fx-background-color: #0a0a0a;");
        VBox.setVgrow(videoContainer, Priority.ALWAYS);

        // Remote video (large, centered)
        remoteView = new ImageView();
        remoteView.setPreserveRatio(true);
        remoteView.fitWidthProperty().bind(videoContainer.widthProperty().subtract(20));
        remoteView.fitHeightProperty().bind(videoContainer.heightProperty().subtract(20));

        // Placeholder for remote video
        VBox remotePlaceholder = new VBox(10);
        remotePlaceholder.setAlignment(Pos.CENTER);
        Label waitIcon = new Label("📹");
        waitIcon.setStyle("-fx-font-size: 48;");
        Label waitLabel = new Label("Đang chờ video từ " + remoteUsername + "...");
        waitLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #666666;");
        remotePlaceholder.getChildren().addAll(waitIcon, waitLabel);

        // Local video (small, bottom-right corner)
        localView = new ImageView();
        localView.setPreserveRatio(true);
        localView.setFitWidth(160);
        localView.setFitHeight(120);

        // Local view border container
        StackPane localContainer = new StackPane(localView);
        localContainer.setStyle("-fx-background-color: #333333; -fx-border-color: #0f3460; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        localContainer.setMaxWidth(164);
        localContainer.setMaxHeight(124);
        localContainer.setPadding(new Insets(2));
        StackPane.setAlignment(localContainer, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(localContainer, new Insets(0, 15, 15, 0));

        videoContainer.getChildren().addAll(remotePlaceholder, remoteView, localContainer);

        // ═══════ BOTTOM BAR (CONTROLS) ═══════
        HBox bottomBar = new HBox(15);
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(12, 15, 12, 15));
        bottomBar.setStyle("-fx-background-color: linear-gradient(to right, #16213e, #0f3460);");

        Button btnEndCall = new Button("📞 Kết thúc cuộc gọi");
        btnEndCall.setStyle(
                "-fx-padding: 10 30; -fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                        "-fx-background-radius: 25; -fx-cursor: hand;"
        );
        btnEndCall.setOnMouseEntered(e -> btnEndCall.setStyle(
                "-fx-padding: 10 30; -fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-background-color: #c0392b; -fx-text-fill: white; " +
                        "-fx-background-radius: 25; -fx-cursor: hand;"
        ));
        btnEndCall.setOnMouseExited(e -> btnEndCall.setStyle(
                "-fx-padding: 10 30; -fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                        "-fx-background-radius: 25; -fx-cursor: hand;"
        ));
        btnEndCall.setOnAction(e -> {
            if (windowListener != null) {
                windowListener.onEndCallClicked();
            }
        });

        bottomBar.getChildren().add(btnEndCall);

        root.getChildren().addAll(topBar, videoContainer, bottomBar);

        Scene scene = new Scene(root, 500, 420);
        stage.setScene(scene);

        // When user closes window directly, treat as ending the call
        stage.setOnCloseRequest(e -> {
            if (windowListener != null) {
                windowListener.onEndCallClicked();
            }
        });
    }

    public void setWindowListener(VideoCallWindowListener listener) {
        this.windowListener = listener;
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    public void close() {
        stage.close();
    }

    public void setStatus(String status, Color color) {
        lblStatus.setText(status);
        lblStatus.setTextFill(color);
    }

    public void updateLocalFrame(Image frame) {
        localView.setImage(frame);
    }

    public void updateRemoteFrame(Image frame) {
        remoteView.setImage(frame);
    }

    public boolean isShowing() {
        return stage.isShowing();
    }
}
