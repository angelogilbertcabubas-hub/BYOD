package utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class QRCodeGenerator {

    // Define where the QR codes will be saved locally inside your project
    private static final String QR_DIRECTORY = "src/main/resources/qrcodes/";

    /**
     * Generates a QR code image file containing the student number.
     * * @param studentNumber The unique ID to encode (e.g., "2026-0001") - This is what the scanner reads.
     * @param studentName   Used just for naming the output PNG file cleanly so the Admin can find it.
     */
    public static void generateStudentQRCode(String studentNumber, String studentName) {
        try {
            // 1. Ensure the qrcodes directory exists, if not, create it
            File directory = new File(QR_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. Create a safe file name (removes spaces from the student's name)
            // Example output: "2026-0001_Juan_Dela_Cruz.png"
            String safeName = studentName.replaceAll("\\s+", "_");
            String fileName = studentNumber + "_" + safeName + ".png";
            String filePath = QR_DIRECTORY + fileName;

            // 3. Generate the QR matrix
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            // 300x300 is a standard, crisp size for printing on ID cards or emailing
            BitMatrix bitMatrix = qrCodeWriter.encode(studentNumber, BarcodeFormat.QR_CODE, 300, 300);

            // 4. Save the matrix as a PNG image to the specified path
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

            System.out.println("Success: QR Code generated and saved at -> " + filePath);

        } catch (WriterException | IOException e) {
            System.err.println("Critical Error generating QR Code for " + studentNumber);
            e.printStackTrace();
        }
    }
}