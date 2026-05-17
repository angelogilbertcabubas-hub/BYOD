package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    public TextField usernameField;
    public PasswordField passwordField;

    public void handleLogin(ActionEvent event) {

        String username = usernameField.getText();
        String password = passwordField.getText();

        if(username.equals("admin") && password.equals("admin")) {

            try {

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/dashboard.fxml"));
                Parent root = loader.load();

                Stage stage = (Stage) usernameField
                        .getScene()
                        .getWindow();

                stage.setScene(new Scene(root));
                stage.setTitle("Dashboard");
                stage.show();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            System.out.println("Invalid Login");
        }
    }
}