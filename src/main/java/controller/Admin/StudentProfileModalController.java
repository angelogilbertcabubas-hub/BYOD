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

        deviceMatrixTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                String path = devicePhotoMap.get(newSelection.getAccessCode());
                loadDevicePhoto(path);
            } else {
                devicePhotoImageView.setImage(null);
            }
        });

        editModeGrid.setVisible(false);
        editModeGrid.setManaged(false);
        btnSaveEdit.setVisible(false);
        btnSaveEdit.setManaged(false);
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);
    }

    public void initData(Student targetStudent) {
        this.focusedStudent = targetStudent;

        lblFullNameHeader.setText(targetStudent.getFullName());
        lblStudentNumberSub.setText("Dossier Node File Record Reference ID: " + targetStudent.getStudentId());
        txtProfileId.setText(targetStudent.getStudentId());
        txtProfileCourse.setText(targetStudent.getCourse());
        txtProfileEmail.setText(targetStudent.getEmail());
        txtProfileMobile.setText(targetStudent.getMobile());

        if (editStudentIdField != null) {
            editStudentIdField.setText(targetStudent.getStudentId());
        }

        fetchStudentCloudPhoto();
        renderZeroDiskQRCode();
        fetchInternalUuidAndDevices();
    }

    // THE FIX: Correctly parses "http" so Supabase cloud photos actually render
    private void fetchStudentCloudPhoto() {
        String query = "SELECT photo_path FROM students WHERE school_id = ?";
        new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, focusedStudent.getStudentId());
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    String photoUrl = rs.getString("photo_path");
                    if (photoUrl != null && photoUrl.startsWith("http")) {
                        Platform.runLater(() -> studentCloudPhotoView.setImage(new Image(photoUrl, true)));
                    } else if (photoUrl != null) {
                        File imgFile = new File("src/main/resources/" + photoUrl);
                        if (imgFile.exists()) {
                            Platform.runLater(() -> studentCloudPhotoView.setImage(new Image(imgFile.toURI().toString())));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // THE FIX: Correctly parses "http" so Supabase device photos actually render
    private void loadDevicePhoto(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            devicePhotoImageView.setImage(null);
            return;
        }
        try {
            if (photoPath.startsWith("http")) {
                devicePhotoImageView.setImage(new Image(photoPath, true));
            } else {
                File imgFile = new File("src/main/resources/" + photoPath);
                if (imgFile.exists()) {
                    devicePhotoImageView.setImage(new Image(imgFile.toURI().toString()));
                } else {
                    devicePhotoImageView.setImage(null);
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load device photo: " + photoPath);
        }
    }

    private void renderZeroDiskQRCode() {
        try {
            Image qrImage = QRCodeGenerator.generateQRCodeInMemory(focusedStudent.getStudentId(), 130, 130);
            if (qrImage != null && qrCodeImageView != null) {
                qrCodeImageView.setImage(qrImage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchInternalUuidAndDevices() {
        isolatedDevicesList.clear();
        devicePhotoMap.clear();

        String fetchUuidSql = "SELECT id FROM students WHERE school_id = ?";
        String fetchDevicesSql = "SELECT device_type, device_brand, device_name, mac_address, unique_code, photo_path FROM devices WHERE student_id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement stmtUuid = conn.prepareStatement(fetchUuidSql)) {

            stmtUuid.setString(1, focusedStudent.getStudentId());
            try (ResultSet rsUuid = stmtUuid.executeQuery()) {
                if (rsUuid.next()) {
                    databaseStudentUuid = (UUID) rsUuid.getObject("id");
                }
            }

            if (databaseStudentUuid != null) {
                try (PreparedStatement stmtDev = conn.prepareStatement(fetchDevicesSql)) {
                    stmtDev.setObject(1, databaseStudentUuid);
                    try (ResultSet rsDev = stmtDev.executeQuery()) {
                        while (rsDev.next()) {
                            String type = rsDev.getString("device_type");
                            String brandModel = rsDev.getString("device_brand") + " " + rsDev.getString("device_name");
                            String mac = rsDev.getString("mac_address");
                            String token = rsDev.getString("unique_code");
                            String photo = rsDev.getString("photo_path");

                            isolatedDevicesList.add(new Device(focusedStudent.getFullName(), type, brandModel, mac, token));
                            devicePhotoMap.put(token, photo);
                        }
                    }
                }
            }

            if (!isolatedDevicesList.isEmpty()) {
                deviceMatrixTable.getSelectionModel().select(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleQuickUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Device Photo");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            try {
                quickDevicePhotoPath = SupabaseStorageHelper.uploadImage(file, "DEV");
                showAlert(Alert.AlertType.INFORMATION, "Cloud Photo Attached", "The image was successfully staged for the new device.");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Upload Failed", "Could not reach cloud storage.");
            }
        }
    }

    @FXML
    private void handleQuickAddDevice(ActionEvent event) {
        String type = quickTypeBox.getValue();
        String rawModel = quickModelField.getText().trim();
        String mac = quickMacField.getText().trim().toUpperCase();

        if (type == null || rawModel.isEmpty() || mac.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Empty", "Please fill up all asset attachment parameters.");
            return;
        }

        if (!mac.equalsIgnoreCase("N/A") && !MAC_PATTERN.matcher(mac).matches()) {
            showAlert(Alert.AlertType.WARNING, "Invalid Parameter", "MAC registration layout fail. Standardized layout: 00:1B:44:11:3A:B7 or N/A");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String insertSql = "INSERT INTO devices (student_id, device_type, device_brand, device_name, mac_address, unique_code, status, photo_path) VALUES (?, ?, ?, ?, ?, ?, 'REGISTERED', ?)";

            if (mac.equals("N/A")) mac = "N/A-" + (100 + new Random().nextInt(900));

            String[] modelSplit = rawModel.split(" ", 2);
            String brand = modelSplit[0];
            String modelStr = modelSplit.length > 1 ? modelSplit[1] : "Unknown";
            String generatedToken = "TKN-" + (1000 + new Random().nextInt(9000));

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setObject(1, databaseStudentUuid);
                ps.setString(2, type);
                ps.setString(3, brand);
                ps.setString(4, modelStr);
                ps.setString(5, mac);
                ps.setString(6, generatedToken);
                ps.setString(7, quickDevicePhotoPath);
                ps.executeUpdate();
            }

            DataStore.getInstance().refreshDevices();
            fetchInternalUuidAndDevices();

            quickModelField.clear();
            quickMacField.clear();
            quickTypeBox.setValue(null);
            quickDevicePhotoPath = "default_device.png";

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Linkage Aborted", "Asset mapping error: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveSelectedDevice(ActionEvent event) {
        Device selectedDevice = deviceMatrixTable.getSelectionModel().getSelectedItem();
        if (selectedDevice == null) {
            showAlert(Alert.AlertType.WARNING, "Zero Selection", "Please choose a hardware object partition to drop.");
            return;
        }

        try (Connection conn = DatabaseHelper.getConnection()) {
            String deleteSql = "DELETE FROM devices WHERE unique_code = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setString(1, selectedDevice.getAccessCode());
                ps.executeUpdate();
            }

            DataStore.getInstance().refreshDevices();
            fetchInternalUuidAndDevices();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Drop Interrupted", "Storage update execution dropped: " + e.getMessage());
        }
    }

    @FXML
    private void handleRemoveStudent(ActionEvent event) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(((Node) event.getSource()).getScene().getWindow());
        dialog.initStyle(StageStyle.TRANSPARENT);

        Label icon = new Label("⚠");
        icon.setStyle("-fx-text-fill: #500A0E; -fx-font-size: 32px;");
        Label title = new Label("Confirm Record Deletion");
        title.setStyle("-fx-text-fill: #500A0E; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox header = new VBox(5, icon, title);
        header.setAlignment(Pos.CENTER);

        Label message = new Label("Are you sure you want to remove " + focusedStudent.getFullName() + "?\nThis action will permanently delete their profile and logs.");
        message.setWrapText(true);
        message.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        message.setStyle("-fx-text-fill: #555555; -fx-font-size: 14px;");

        Button btnConfirm = new Button("YES, DELETE RECORD");
        btnConfirm.setPrefWidth(220);
        btnConfirm.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");

        Button btnCancel = new Button("NO, KEEP RECORD");
        btnCancel.setPrefWidth(220);
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #500A0E; -fx-font-weight: bold; -fx-border-color: #500A0E; -fx-border-radius: 4; -fx-cursor: hand;");

        VBox btnContainer = new VBox(10, btnConfirm, btnCancel);
        btnContainer.setAlignment(Pos.CENTER);

        VBox root = new VBox(25, header, message, btnContainer);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #500A0E; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

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

    @FXML
    private void handleCloseModal(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleEditToggle(ActionEvent event){
        editCourseField.setText(txtProfileCourse.getText());
        editEmailField.setText(txtProfileEmail.getText());
        editMobileField.setText(txtProfileMobile.getText());

        viewModeGrid.setVisible(false);
        viewModeGrid.setManaged(false);
        editModeGrid.setManaged(true);
        editModeGrid.setVisible(true);

        btnEditToggle.setVisible(false);
        btnEditToggle.setManaged(false);
        btnSaveEdit.setVisible(true);
        btnSaveEdit.setManaged(true);
        btnCancelEdit.setVisible(true);
        btnCancelEdit.setManaged(true);
    }

    @FXML
    private void handleSaveEdit(ActionEvent event){
        String newCourse = editCourseField.getText().trim();
        String newEmail = editEmailField.getText().trim();
        String newMobile = editMobileField.getText().trim();

        if (newCourse.isEmpty() || newEmail.isEmpty() || newMobile.isEmpty()){
            showAlert(Alert.AlertType.WARNING, "Empty Fields", "All fields must be filled in");
            return;
        }

        try(Connection conn = DatabaseHelper.getConnection()) {
            String updateSql = "UPDATE students SET program_course = ?, email_address = ?, mobile_number = ? WHERE school_id = ?";
            try(PreparedStatement preparedStatement = conn.prepareStatement(updateSql)) {
                preparedStatement.setString(1, newCourse);
                preparedStatement.setString(2, newEmail);
                preparedStatement.setString(3, newMobile);
                preparedStatement.setString(4, focusedStudent.getStudentId());
                preparedStatement.executeUpdate();
            }
            txtProfileCourse.setText(newCourse);
            txtProfileEmail.setText(newEmail);
            txtProfileMobile.setText(newMobile);

            utils.DataStore.getInstance().refreshStudents();

            showAlert(Alert.AlertType.INFORMATION, "Profile Updated", "Student new Information saved successfully");
            switchToViewMode();
        } catch (Exception e){
            showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not save new Informaion");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelEdit(ActionEvent event){
        switchToViewMode();
    }

    private void switchToViewMode() {
        editModeGrid.setVisible(false);
        editModeGrid.setManaged(false);
        viewModeGrid.setVisible(true);
        viewModeGrid.setManaged(true);

        btnSaveEdit.setVisible(false);
        btnSaveEdit.setManaged(false);
        btnCancelEdit.setVisible(false);
        btnCancelEdit.setManaged(false);
        btnEditToggle.setVisible(true);
        btnEditToggle.setManaged(true);
    }

    private void showAlert(Alert.AlertType type, String header, String body) {
        Alert alert = new Alert(type);
        alert.setTitle("Account Provisioning System Core");
        alert.setHeaderText(header);
        alert.setContentText(body);
        alert.showAndWait();
    }
}