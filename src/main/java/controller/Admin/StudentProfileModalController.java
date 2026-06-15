package controller.Admin;

import com.example.byod.model.Device;
import com.example.byod.model.Student;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import utils.DataStore;
import utils.DatabaseHelper;
import utils.QRCodeGenerator;
import utils.SupabaseStorageHelper;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.regex.Pattern;

public class StudentProfileModalController {

    @FXML private Label lblFullNameHeader;
    @FXML private Label lblStudentNumberSub;
    @FXML private Label txtProfileId;
    @FXML private Label txtProfileCourse;
    @FXML private Label txtProfileEmail;
    @FXML private Label txtProfileMobile;
    @FXML private ImageView studentCloudPhotoView;
    @FXML private ImageView qrCodeImageView;
    @FXML private ImageView devicePhotoImageView;
    @FXML private TableView<Device> deviceMatrixTable;
    @FXML private TableColumn<Device, String> colType;
    @FXML private TableColumn<Device, String> colModel;
    @FXML private TableColumn<Device, String> colMac;
    @FXML private TableColumn<Device, String> colToken;
    @FXML private ComboBox<String> quickTypeBox;
    @FXML private TextField quickModelField;
    @FXML private TextField quickMacField;
    @FXML private Button btnEditToggle;
    @FXML private Button btnSaveEdit;
    @FXML private Button btnCancelEdit;
    @FXML private TextField editCourseField;
    @FXML private TextField editEmailField;
    @FXML private TextField editMobileField;
    @FXML private GridPane viewModeGrid;
    @FXML private GridPane editModeGrid;
    @FXML private TextField editStudentIdField;

    private Student focusedStudent;
    private UUID databaseStudentUuid;
    private ObservableList<Device> isolatedDevicesList = FXCollections.observableArrayList();
    private Map<String, String> devicePhotoMap = new HashMap<>();
    private String quickDevicePhotoPath = "default_device.png";
    private final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    @FXML
    public void initialize() {
        colType.setCellValueFactory(new PropertyValueFactory<>("deviceType"));
        colModel.setCellValueFactory(new PropertyValueFactory<>("brandModel"));
        colMac.setCellValueFactory(new PropertyValueFactory<>("macAddress"));
        colToken.setCellValueFactory(new PropertyValueFactory<>("accessCode"));
        deviceMatrixTable.setItems(isolatedDevicesList);
        quickTypeBox.getItems().addAll("Smartphone", "Laptop", "Tablet", "Speaker", "Projector", "Smart Watch", "Others");
    }

    public void initData(Student targetStudent) {
        this.focusedStudent = targetStudent;
        lblFullNameHeader.setText(targetStudent.getFullName());
        lblStudentNumberSub.setText("Dossier Node File Record Reference ID: " + targetStudent.getStudentId());
        txtProfileId.setText(targetStudent.getStudentId());
        txtProfileCourse.setText(targetStudent.getCourse());
        txtProfileEmail.setText(targetStudent.getEmail());
        txtProfileMobile.setText(targetStudent.getMobile());
        if (editStudentIdField != null) editStudentIdField.setText(targetStudent.getStudentId());
        fetchStudentCloudPhoto();
        renderZeroDiskQRCode();
        fetchInternalUuidAndDevices();
    }

    @FXML
    private void handleRemoveStudent(ActionEvent event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(((Node) event.getSource()).getScene().getWindow());
        dialog.initStyle(StageStyle.TRANSPARENT);

        // Header: Updated Title
        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill: #500A0E; -fx-font-size: 32px;");
        Label title = new Label("Confirm Record Deletion"); // Changed from "Security Override"
        title.setStyle("-fx-text-fill: #500A0E; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox header = new VBox(5, icon, title);
        header.setAlignment(Pos.CENTER);

        // Message
        Label message = new Label("Are you sure you want to remove " + focusedStudent.getFullName() + "?\nThis action will permanently delete their profile and logs.");
        message.setWrapText(true);
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        message.setStyle("-fx-text-fill: #555555; -fx-font-size: 14px;");

        // Action Buttons
        Button btnConfirm = new Button("YES, DELETE RECORD");
        btnConfirm.setPrefWidth(220);
        btnConfirm.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");

        Button btnCancel = new Button("NO, KEEP RECORD");
        btnCancel.setPrefWidth(220);
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #500A0E; -fx-font-weight: bold; -fx-border-color: #500A0E; -fx-border-radius: 4; -fx-cursor: hand;");

        VBox btnContainer = new VBox(10, btnConfirm, btnCancel);
        btnContainer.setAlignment(Pos.CENTER);

        // Root Container
        VBox root = new VBox(25, header, message, btnContainer);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Logic
        btnCancel.setOnAction(e -> dialog.close());
        btnConfirm.setOnAction(e -> {
            dialog.close();
            performStudentDeletion(event);
        });

        Scene scene = new Scene(root, 420, 320);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    private void performStudentDeletion(ActionEvent event) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String purgeSql = "DELETE FROM students WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(purgeSql)) {
                ps.setObject(1, databaseStudentUuid);
                ps.executeUpdate();
            }
            DataStore.getInstance().getStudentsList().remove(focusedStudent);
            DataStore.getInstance().refreshStudents();
            DataStore.getInstance().refreshDevices();
            DataStore.getInstance().refreshLogs();
            handleCloseModal(event);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Purge Failed", "Could not remove record: " + e.getMessage());
        }
    }

    // --- RETAINED METHODS ---
    @FXML private void handleCloseModal(ActionEvent event) { ((Stage) ((Node) event.getSource()).getScene().getWindow()).close(); }
    @FXML private void handleEditToggle(ActionEvent event){ /* Existing Logic */ }
    @FXML private void handleSaveEdit(ActionEvent event){ /* Existing Logic */ }
    @FXML private void handleCancelEdit(ActionEvent event){ switchToViewMode(); }
    @FXML private void handleQuickAddDevice(ActionEvent event){ /* Existing Logic */ }
    @FXML private void handleRemoveSelectedDevice(ActionEvent event){ /* Existing Logic */ }
    @FXML private void handleQuickUploadPhoto(ActionEvent event){ /* Existing Logic */ }

    private void fetchStudentCloudPhoto() { /* Existing Logic */ }
    private void loadDevicePhoto(String path) { /* Existing Logic */ }
    private void renderZeroDiskQRCode() { /* Existing Logic */ }
    private void fetchInternalUuidAndDevices() { /* Existing Logic */ }
    private void switchToViewMode() { /* Existing Logic */ }
    private void showAlert(Alert.AlertType t, String h, String b) { Alert a = new Alert(t); a.setHeaderText(h); a.setContentText(b); a.showAndWait(); }
}