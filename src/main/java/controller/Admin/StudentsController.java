package controller.Admin;

import com.example.byod.model.Student;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
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
        // Standard Columns
        colStudentName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colStudentNumber.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colCourse.setCellValueFactory(new PropertyValueFactory<>("course"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        filteredStudents = new FilteredList<>(DataStore.getInstance().getStudentsList(), p -> true);

        // Search Filter Logic
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

        // Phase 3: Injecting "Actions" column programmatically with Edit & Delete buttons
        TableColumn<Student, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(160);
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(10, editBtn, deleteBtn);

            {
                // Styling the buttons for a clean, professional look
                editBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 15;");
                deleteBtn.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 10;");
                pane.setAlignment(Pos.CENTER);

                editBtn.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    openEditStudentModal(student);
                });

                deleteBtn.setOnAction(event -> {
                    Student student = getTableView().getItems().get(getIndex());
                    handleDeleteStudent(student);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });

        // Add the action column to the far right of the table
        studentsTableView.getColumns().add(colActions);

        // Retain existing double-click profile behavior
        studentsTableView.setRowFactory(tv -> {
            TableRow<Student> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {

                    // Safe check to ensure we didn't double-click inside an action button
                    Node target = (Node) event.getTarget();
                    boolean clickedOnButton = false;
                    while (target != null) {
                        if (target instanceof Button) {
                            clickedOnButton = true;
                            break;
                        }
                        target = target.getParent();
                    }

                    if (!clickedOnButton) {
                        Student selectedStudent = row.getItem();
                        openStudentProfileModal(selectedStudent);
                    }
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

            updateLabel(DataStore.getInstance().getStudentsList().size());
            studentsTableView.refresh();

        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Unable to construct the Student Profile interface view frame.");
            e.printStackTrace();
        }
    }

    // Phase 3: Edit Student Modal Launcher
    private void openEditStudentModal(Student student) {
        try {
            // Reusing the Add Student layout for the Edit overlay
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/Admin/AddStudentModal.fxml"));
            Parent root = loader.load();

            AddStudentController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Student Record: " + student.getFullName());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(studentsTableView.getScene().getWindow());

            // Retaining the window sizing fixes from Phase 1
            dialogStage.setScene(new Scene(root, 650, 700));
            dialogStage.setResizable(true);

            dialogStage.showAndWait();

            studentsTableView.refresh();
            updateLabel(DataStore.getInstance().getStudentsList().size());

        } catch (IOException e) {
            System.err.println("CRITICAL ERROR: Could not load the Edit Student Modal.");
            e.printStackTrace();
        }
    }

    // Phase 3: Delete Student Logic with Confirmation Dialog
    private void handleDeleteStudent(Student student) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete Student Record");
        alert.setContentText("Are you sure you want to permanently remove " + student.getFullName() + " (" + student.getStudentId() + ") from the system?\n\nThis action cannot be undone.");

        ButtonType confirmBtn = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

        alert.showAndWait().ifPresent(type -> {
            if (type == confirmBtn) {
                // Remove the student record from the local memory instance
                DataStore.getInstance().getStudentsList().remove(student);

                // Triggers table to redraw without the deleted row
                studentsTableView.refresh();
                updateLabel(filteredStudents.size());
            }
        });
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

            dialogStage.setScene(new javafx.scene.Scene(root, 650, 700));
            dialogStage.setResizable(true);

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