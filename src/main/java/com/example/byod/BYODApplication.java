package com.example.byod;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class BYODApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                BYODApplication.class.getResource("login.fxml")
        );

        Scene scene = new Scene(loader.load());

        stage.setTitle("BYOD System");
        stage.setScene(scene);
        stage.setWidth(1200);
        stage.setHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}