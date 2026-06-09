package controller.Admin;

import com.example.byod.model.Student;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

public class StudentsController extends BaseAdminController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<Student> studentsTableView;

    @FXML private TableColumn<Student, String> colStudentName;
    @FXML private TableColumn<Student, String> colStudentID;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colStatus;

    private FilteredList<Student> filteredStudents;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colStudentID.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredStudents = new FilteredList<>(DataStore.getInstance().getStudentsList(), p -> true);

        searchBarField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredStudents.setPredicate(student -> {
                if(newValue == null || newValue.isBlank()) return true;

                String keyword = newValue.toLowerCase();

                if(student.getFullName() != null && student.getFullName().toLowerCase().contains(keyword)) return true;
                if(student.getStudentId() != null && student.getStudentId().toLowerCase().contains(keyword)) return true;
                if(student.getCourse() != null && student.getCourse().toLowerCase().contains(keyword)) return true;
                if(student.getEmail() != null && student.getEmail().toLowerCase().contains(keyword)) return true;
                if(student.getStatus() != null && student.getStatus().toLowerCase().contains(keyword)) return true;

                return false;
            });

            updateLabel(filteredStudents.size());
        });

        SortedList<Student> sortedStudent = new SortedList<>(filteredStudents);
        sortedStudent.comparatorProperty().bind(studentsTableView.comparatorProperty());
        studentsTableView.setItems(sortedStudent);

        studentsTableView.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Student selectedStudent = row.getItem();
                    openEditStudentModal(selectedStudent);
                }
            });
            return row;
        });

        updateLabel(sortedStudent.size());
    }

    @FXML
    private void handleAddStudent(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/byod/Admin/AddStudentModal.fxml"));
            javafx.scene.Parent root = loader.load();

            AddStudentController dialogController = loader.getController();

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Add New Student");

            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(((javafx.scene.Node) event.getSource()).getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));

            dialogStage.showAndWait();

            Student createdStudent = dialogController.getNewStudent();
            if (createdStudent != null) {
                utils.DataStore.getInstance().getStudentsList().add(createdStudent);

                int count = utils.DataStore.getInstance().getStudentsList().size();
                statusSummaryLabel.setText("Showing 1 to " + count + " of " + count + " entries");
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void openEditStudentModal(Student student) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/example/byod/Admin/EditStudentModal.fxml"));
            javafx.scene.Parent root = loader.load();

            EditStudentController dialogController = loader.getController();
            dialogController.initData(student);

            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Student Information & Devices");
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(studentsTableView.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));

            dialogStage.showAndWait();

            studentsTableView.refresh();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private void updateLabel(int filtered) {
        int total = DataStore.getInstance().getStudentsList().size();
        if (filtered == total) {
            statusSummaryLabel.setText("Showing 1 to " + total + " of " + total + " entries");
        } else {
            statusSummaryLabel.setText("Showing " + filtered + " of " + total + " entries");
        }
    }
}