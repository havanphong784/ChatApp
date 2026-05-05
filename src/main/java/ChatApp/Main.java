package ChatApp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    static void main(String[] args) {
        System.out.println("🚀 App bắt đầu chạy...\n");
        Application.launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Chat App - Protocol Buffers");
        primaryStage.setResizable(false);
        Scene scene = new Scene(FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/chatapp.fxml"))));
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
