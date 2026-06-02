package controller.Admin;

import com.example.byod.model.Student;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import utils.DatabaseHelper;
import utils.QRCodeGenerator;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class AddStudentController {

    @FXML private TextField firstNameField;
    @FXML private TextField middleNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> cmbCourse;
    @FXML private TextField sectionField;
    @FXML private TextField mobileField;
    @FXML private VBox deviceListContainer;

    private Student newStudent = null;
    private boolean saveClicked = false;
    private List<DeviceRowComponents> deviceRowsList = new ArrayList<>();

    private final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private final Pattern MOBILE_PATTERN = Pattern.compile("^(09|\\+639)\\d{9}$");
    private final Pattern SECTION_PATTERN = Pattern.compile("^[1-4]-[1-9]$");
    private final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    // Inner class to keep track of dynamically generated UI elements
    private static class DeviceRowComponents {
        VBox cardContainer;
        ComboBox<String> typeBox;
        TextField modelField;
        TextField macField;

        DeviceRowComponents(VBox cardContainer, ComboBox<String> typeBox, TextField modelField, TextField macField) {
            this.cardContainer = cardContainer;
            this.typeBox = typeBox;
            this.modelField = modelField;
            this.macField = macField;
        }
    }

    @FXML
    public void initialize() {
        cmbCourse.getItems().addAll("BSIT", "BSA", "BSECE", "BSBA", "BSCS", "BSCpE", "BSIE");

        // Generate 3 empty device forms by default on startup
        for (int i = 0; i < 3; i++) {
            generateNewDeviceRowField();
        }
    }

    // Creates the visual "Card" for a single device and adds it to the list
    private void generateNewDeviceRowField() {
        int continuousIndex = deviceRowsList.size() + 1;

        VBox rowCard = new VBox(12);
        rowCard.setPadding(new Insets(15));
        rowCard.setStyle("-fx-background-color: #F7F5F5; -fx-border-color: #E2DDD9; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Use pure CSS to ensure the label renders properly
        Label rowTitle = new Label("Device " + continuousIndex);
        rowTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #500A0E;");
        rowCard.getChildren().add(rowTitle);

        GridPane layoutGrid = new GridPane();
        layoutGrid.setHgap(15);
        layoutGrid.setVgap(15);

        // Type
        VBox typeContainer = new VBox(5);
        Label typeLabel = new Label("DEVICE TYPE");
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555555;");
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Smartphone", "Laptop", "Tablet", "Speaker", "Projector", "Smart Watch", "Others");
        typeBox.setPromptText("Select Category");
        typeBox.setPrefHeight(38);
        typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD9; -fx-border-radius: 6;");
        typeContainer.getChildren().addAll(typeLabel, typeBox);
        layoutGrid.add(typeContainer, 0, 0);

        // Brand & Model
        VBox modelContainer = new VBox(5);
        Label modelLabel = new Label("BRAND & MODEL DESCRIPTION");
        modelLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555555;");
        TextField modelField = new TextField();
        modelField.setPromptText("e.g., Asus ROG G14");
        modelField.setPrefHeight(38);
        modelField.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD9; -fx-border-radius: 6;");
        modelContainer.getChildren().addAll(modelLabel, modelField);
        layoutGrid.add(modelContainer, 0, 1);

        // MAC Address with N/A Button
        VBox macContainer = new VBox(5);
        Label macLabel = new Label("PHYSICAL MAC ADDRESS");
        macLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #555555;");

        HBox macInputGroup = new HBox(8);
        TextField macField = new TextField();
        macField.setPromptText("e.g., 00:1B:44:11:3A:B7");
        macField.setPrefHeight(38);
        macField.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #E2DDD9; -fx-border-radius: 6;");
        HBox.setHgrow(macField, Priority.ALWAYS);

        Button btnNA = new Button("N/A");
        btnNA.setPrefHeight(38);
        btnNA.setStyle("-fx-background-color: #E5E1E2; -fx-text-fill: #555555; -fx-font-weight: bold; -fx-border-color: #CCCCCC; -fx-border-radius: 6; -fx-cursor: hand;");
        btnNA.setOnAction(e -> macField.setText("N/A")); // Securely sets the N/A format

        macInputGroup.getChildren().addAll(macField, btnNA);
        macContainer.getChildren().addAll(macLabel, macInputGroup);
        layoutGrid.add(macContainer, 1, 1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        layoutGrid.getColumnConstraints().addAll(col1, col2);
        GridPane.setColumnSpan(typeContainer, 2);

        rowCard.getChildren().add(layoutGrid);
        deviceListContainer.getChildren().add(rowCard);

        deviceRowsList.add(new DeviceRowComponents(rowCard, typeBox, modelField, macField));
    }

    @FXML
    private void handleIncrementDeviceRow(ActionEvent event) {
        generateNewDeviceRowField();
    }

    private boolean isValidMac(String macStr) {
        if (macStr.equalsIgnoreCase("N/A")) {
            return true;
        }
        return MAC_PATTERN.matcher(macStr).matches();
    }

    @FXML
    private void handleSaveStudentAction(ActionEvent event) {
        if (isInputValid()) {
            try {
                String fName = firstNameField.getText().trim();
                String mName = middleNameField.getText().trim();
                String lName = lastNameField.getText().trim();
                String cleanCourse = cmbCourse.getValue();
                String cleanSection = sectionField.getText().trim();
                String cleanEmail = emailField.getText().trim();
                String cleanMobile = mobileField.getText().trim();

                String fullCombinedName = lName + ", " + fName + (mName.isEmpty() ? "" : " " + mName);
                String generatedId = "2026-" + (int)(Math.random() * 90000 + 10000);

                try (Connection conn = DatabaseHelper.getConnection()) {
                    conn.setAutoCommit(false);
                    int dbStudentId = -1;

                    String studentQuery = "INSERT INTO students (student_number, first_name, last_name, course, section, status) VALUES (?, ?, ?, ?, ?, 'Active')";
                    try (PreparedStatement pstmt = conn.prepareStatement(studentQuery, Statement.RETURN_GENERATED_KEYS)) {
                        pstmt.setString(1, generatedId);
                        pstmt.setString(2, fName);
                        pstmt.setString(3, lName + (mName.isEmpty() ? "" : " " + mName));
                        pstmt.setString(4, cleanCourse);
                        pstmt.setString(5, cleanSection);
                        pstmt.executeUpdate();

                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                dbStudentId = generatedKeys.getInt(1);
                            }
                        }
                    }

                    String deviceQuery = "INSERT INTO devices (student_id, device_type, brand, model, serial_number, access_code) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(deviceQuery)) {
                        for (DeviceRowComponents row : deviceRowsList) {
                            String type = row.typeBox.getValue();
                            String rawModel = row.modelField.getText().trim();
                            String mac = row.macField.getText().trim().toUpperCase();

                            if (type != null && !rawModel.isEmpty() && !mac.isEmpty()) {
                                String[] modelSplit = rawModel.split(" ", 2);
                                String brand = modelSplit[0];
                                String modelStr = modelSplit.length > 1 ? modelSplit[1] : "Unknown";

                                int randomTokenNum = 1000 + new Random().nextInt(9000);
                                String generatedToken = "TKN-" + randomTokenNum;

                                insertStmt.setInt(1, dbStudentId);
                                insertStmt.setString(2, type);
                                insertStmt.setString(3, brand);
                                insertStmt.setString(4, modelStr);
                                insertStmt.setString(5, mac);
                                insertStmt.setString(6, generatedToken);
                                insertStmt.addBatch();
                            }
                        }
                        insertStmt.executeBatch();
                    }
                    conn.commit();
                }

                newStudent = new Student(generatedId, fullCombinedName, cleanCourse, cleanSection, cleanMobile, "Active");

                QRCodeGenerator.generateStudentQRCode(generatedId, fullCombinedName);
                String safeName = fullCombinedName.replaceAll("\\s+", "_");
                String fileName = generatedId + "_" + safeName + ".png";
                String filePath = "src/main/resources/qrcodes/" + fileName;
                File qrFile = new File(filePath);

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Registration Success");
                alert.setHeaderText("Student Registered: " + fullCombinedName);
                alert.setContentText("The student and all filled devices have been successfully registered.\nQR generated at: " + filePath);

                if (qrFile.exists()) {
                    Image qrImage = new Image(qrFile.toURI().toString());
                    ImageView imageView = new ImageView(qrImage);
                    imageView.setFitWidth(250);
                    imageView.setFitHeight(250);
                    imageView.setPreserveRatio(true);
                    alert.setGraphic(imageView);
                }

                alert.showAndWait();
                saveClicked = true;
                closeStage(event);

            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Database Transaction Failure", "Failed to save data: " + e.getMessage());
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
            errorBuilder.append("- First Name is required.\n");
        }
        if (lastNameField.getText() == null || lastNameField.getText().trim().isEmpty()) {
            errorBuilder.append("- Last Name is required.\n");
        }
        String email = emailField.getText();
        if (email == null || email.trim().isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            errorBuilder.append("- Valid Email Address is required (e.g., student@pup.edu.ph).\n");
        }
        if (cmbCourse.getValue() == null) {
            errorBuilder.append("- Course selection is required.\n");
        }
        String section = sectionField.getText();
        if (section == null || section.trim().isEmpty() || !SECTION_PATTERN.matcher(section).matches()) {
            errorBuilder.append("- Section format must be [Year]-[Block] (e.g., 2-1, 4-3).\n");
        }
        String mobile = mobileField.getText();
        if (mobile == null || mobile.trim().isEmpty() || !MOBILE_PATTERN.matcher(mobile).matches()) {
            errorBuilder.append("- Valid Philippine Mobile Number is required (e.g., 09123456789).\n");
        }

        int index = 1;
        for (DeviceRowComponents row : deviceRowsList) {
            String type = row.typeBox.getValue();
            String model = row.modelField.getText().trim();
            String mac = row.macField.getText().trim();

            boolean partlyFilled = (type != null) || !model.isEmpty() || !mac.isEmpty();
            boolean completelyFilled = (type != null) && !model.isEmpty() && !mac.isEmpty();

            if (partlyFilled && !completelyFilled) {
                if (type == null) {
                    errorBuilder.append("- Device Type selection missing on Device ").append(index).append(".\n");
                }
                if (model.isEmpty()) {
                    errorBuilder.append("- Brand/Model description missing on Device ").append(index).append(".\n");
                }
                if (mac.isEmpty() || !isValidMac(mac)) {
                    errorBuilder.append("- MAC validation failed on Device ").append(index).append(" (Format: 00:1B:44:11:3A:B7 or N/A).\n");
                }
            } else if (completelyFilled && !isValidMac(mac)) {
                errorBuilder.append("- MAC validation failed on Device ").append(index).append(" (Format: 00:1B:44:11:3A:B7 or N/A).\n");
            }
            index++;
        }

        if (errorBuilder.length() == 0) {
            return true;
        } else {
            showAlert(Alert.AlertType.WARNING, "Form Verification Failures", "Please resolve the following before saving:\n\n" + errorBuilder.toString());
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