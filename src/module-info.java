module com.example.byod {

    requires javafx.controls;

    requires javafx.fxml;

    requires javafx.graphics;



    requires java.sql;



    opens controller to javafx.fxml;

    opens controller.Admin to javafx.fxml;

    opens controller.Security to javafx.fxml;



    opens com.example.byod.model to javafx.base, javafx.fxml;



    exports com.example.byod;

}
