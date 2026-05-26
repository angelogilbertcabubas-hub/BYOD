module com.example.byod {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.byod to javafx.fxml;
    exports com.example.byod;
    opens controller to javafx.fxml;
    exports controller;
    opens controller.Admin to javafx.fxml;
    exports controller.Admin;
    opens controller.Security to javafx.fxml;
    exports controller.Security;
}