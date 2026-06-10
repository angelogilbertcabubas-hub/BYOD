module com.example.byod {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires io.github.cdimascio.dotenv;

    opens com.example.byod to javafx.fxml;
    opens controller to javafx.fxml;
    opens controller.Admin to javafx.fxml;
    opens controller.Security to javafx.fxml;

    exports com.example.byod;
    exports controller;
    exports controller.Admin;
    exports controller.Security;
}