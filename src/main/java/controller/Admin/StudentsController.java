package controller.Admin;

import com.example.byod.model.Student;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.event.ActionEvent;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class StudentsController extends BaseAdminController {

    @FXML private TextField searchBarField;
    @FXML private Label statusSummaryLabel;
    @FXML private TableView<Student> studentsTableView;

    @FXML private TableColumn<Student, String> colStudentName;
    @FXML private TableColumn<Student, String> colStudentNumber;
    @FXML private TableColumn<Student, String> colCourse;
    @FXML private TableColumn<Student, String> colEmail;
    @FXML private TableColumn<Student, String> colStatus;

    private FilteredList<Student> filteredStudents;

    @FXML
    public void initialize() {
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colStudentNumber.setCellValueFactory(new PropertyValueFactory<>("studentId"));
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

        // FIX: Configure Table Rows to be double-clickable to open the profile manager modal
        studentsTableView.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Student selectedStudent = row.getItem();
                    openStudentProfileModal(selectedStudent);
                }
            });
            row.setStyle("-fx-cursor: hand;");
            return row;
        });

        SortedList<Student> sortedStudent = new SortedList<>(filteredStudents);
        sortedStudent.comparatorProperty().bind(studentsTableView.comparatorProperty());
        studentsTableView.setItems(sortedStudent);

        updateLabel(sortedStudent.size());
    }

    private void openStudentProfileModal(Student student) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/Admin/StudentProfileModal.fxml"));
            Parent root = loader.load();

            StudentProfileModalController controller = loader.getController();
            controller.initData(student);

            Stage stage = new Stage();
            stage.setTitle("Student Profile & Asset Matrix Manager");
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(studentsTableView.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // Refresh view lists after the modal interacts with storage changes
            updateLabel(DataStore.getInstance().getStudentsList().size());
            studentsTableView.refresh();

        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Unable to construct the Student Profile interface view frame.");
            e.printStackTrace();
        }
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

            int count = utils.DataStore.getInstance().getStudentsList().size();
            statusSummaryLabel.setText("Showing 1 to " + count + " of " + count + " entries");

        } catch (java.io.IOException e) {
            System.err.println("CRITICAL ERROR: Could not load the Add Student Modal.");
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