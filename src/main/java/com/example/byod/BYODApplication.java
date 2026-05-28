package com.example.byod;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class BYODApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        URL fxmlLocation = getClass().getResource("/com/example/byod/login.fxml");

        if (fxmlLocation == null) {
            throw new RuntimeException("Critical Error: login.fxml layout could not be found in the resource path!");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Scene scene = new Scene(loader.load());

        stage.setTitle("BYOD System");
        stage.setScene(scene);
        stage.setWidth(1200);
        stage.setHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}