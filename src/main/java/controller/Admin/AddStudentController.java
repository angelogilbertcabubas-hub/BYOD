package controller.Admin;

import com.example.byod.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddStudentController {

    @FXML private TextField txtStudentId;
    @FXML private TextField txtFullName;
    @FXML private TextField txtCourse;
    @FXML private TextField txtEmail;
    @FXML private TextField txtMobile;

    private Student newStudent = null;

    @FXML
    private void handleSave(ActionEvent event) {
        try {
            newStudent = new Student(
                    txtStudentId.getText(),
                    txtFullName.getText(),
                    txtCourse.getText(),
                    txtEmail.getText(),
                    txtMobile.getText(),
                    "Active"
            );
            closeStage(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        newStudent = null;
        closeStage(event);
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    public Student getNewStudent() {
        return newStudent;
    }
}