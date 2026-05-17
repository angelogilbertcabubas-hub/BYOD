module com.example.byod {
    requires javafx.controls;
    requires javafx.fxml;

    // Allows JavaFX to see your main application classes
    opens com.example.byod to javafx.fxml;
    exports com.example.byod;

    // CRITICAL: Allows JavaFX to see and interact with your Controller classes
    opens controller to javafx.fxml;
    exports controller;
}