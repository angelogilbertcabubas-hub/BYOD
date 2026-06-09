package controller.Admin;

import com.example.byod.model.Student;
import com.example.byod.model.Device;
import utils.DatabaseHelper;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EditStudentController {

    @FXML private TextField fullNameField;
    @FXML private TextField studentIdField;
    @FXML private TextField courseField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private ImageView qrCodeImageView;

    @FXML private TableView<Device> devicesTableView;
    @FXML private Label deviceQuantityLabel;

    private Student currentStudent;

    public void initData(Student student) {
        this.currentStudent = student;

        fullNameField.setText(student.getFullName());
        studentIdField.setText(student.getStudentId());
        courseField.setText(student.getCourse());
        emailField.setText(student.getEmail());
        statusComboBox.getItems().addAll("Active", "Inactive");
        statusComboBox.setValue(student.getStatus());

        loadQRCode();
        loadRegisteredDevices();
    }

    private void loadQRCode() {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(currentStudent.getStudentId(), BarcodeFormat.QR_CODE, 200, 200);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            Image qrImage = new Image(new ByteArrayInputStream(pngData));
            qrCodeImageView.setImage(qrImage);

        } catch (WriterException | IOException e) {
            System.err.println("Failed to render QR code dynamically.");
        }
    }

    private void loadRegisteredDevices() {
        // Device fetching logic goes here
    }

    @FXML
    private void handleSaveChanges() {
        currentStudent.setFullName(fullNameField.getText());
        currentStudent.setCourse(courseField.getText());
        currentStudent.setEmail(emailField.getText());
        currentStudent.setStatus(statusComboBox.getValue());

        String updateQuery = "UPDATE students SET full_name = ?, course = ?, email = ?, status = ? WHERE student_id = ?";

        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setString(1, currentStudent.getFullName());
            pstmt.setString(2, currentStudent.getCourse());
            pstmt.setString(3, currentStudent.getEmail());
            pstmt.setString(4, currentStudent.getStatus());
            pstmt.setString(5, currentStudent.getStudentId());

            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        closeModal();
    }

    @FXML
    private void closeModal() {
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        stage.close();
    }
}