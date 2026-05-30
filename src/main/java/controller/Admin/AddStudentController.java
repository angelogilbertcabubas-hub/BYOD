package controller.Admin;

import com.example.byod.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddStudentController {

    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField courseField;
    @FXML private TextField mobileField;

    private Student newStudent = null;
    private boolean saveClicked = false;

    @FXML
    public void initialize() {
    }

    @FXML
    private void handleSaveStudentAction(ActionEvent event) {
        if (isInputValid()) {
            try {
                String middle = middleNameField.getText().trim();
                String fullCombinedName = lastNameField.getText().trim() + ", " +
                        firstNameField.getText().trim() +
                        (middle.isEmpty() ? "" : " " + middle);

                String cleanCourse = courseField.getText().trim().toUpperCase();
                String cleanEmail = emailField.getText().trim();

                String cleanMobile = (mobileField.getText() == null) ? "" : mobileField.getText().trim();
                String generatedId = "2026-" + (int)(Math.random() * 90000 + 10000);

                newStudent = new Student(
                        generatedId,
                        fullCombinedName,
                        cleanCourse,
                        cleanEmail,
                        cleanMobile,
                        "Active"
                );

                saveClicked = true;
                closeStage(event);
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "System Instance Failure",
                        "Failed to parse and map data fields to the Student object: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleCancelAction(ActionEvent event) {
        newStudent = null;
        saveClicked = false;
        closeStage(event);
    }

    private boolean isInputValid() {
        StringBuilder errorBuilder = new StringBuilder();

        if (firstNameField.getText() == null || firstNameField.getText().trim().isEmpty()) {
            errorBuilder.append("- First Name is a mandatory field.\n");
        }
        if (lastNameField.getText() == null || lastNameField.getText().trim().isEmpty()) {
            errorBuilder.append("- Last Name is a mandatory field.\n");
        }
        if (emailField.getText() == null || emailField.getText().trim().isEmpty()) {
            errorBuilder.append("- Email Address tracking context is required.\n");
        }
        if (courseField.getText() == null || courseField.getText().trim().isEmpty()) {
            errorBuilder.append("- Course designation text cannot be left empty.\n");
        }

        if (errorBuilder.length() == 0) {
            return true;
        } else {
            showAlert(Alert.AlertType.WARNING, "Form Verification Failures",
                    "Please address the following missing system parameters:\n\n" + errorBuilder.toString());
            return false;
        }
    }

    private void closeStage(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Account Provisioning System");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public Student getNewStudent() {
        return newStudent;
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }
}