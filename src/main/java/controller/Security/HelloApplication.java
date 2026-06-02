package controller.Security;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // FIXED PATH: Added the missing /Security/ folder to match your resources directory
        URL url = getClass().getResource("/com/example/byod/Security/SecurityDashboard.fxml");
        System.out.println("FXML URL: " + url);

        if (url == null) {
            throw new RuntimeException("Cannot find SecurityDashboard.fxml! Check your resources path.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(url);
        Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
        stage.setTitle("BYOD - Security Dashboard");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}