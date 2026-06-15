package utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.ImageView;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class QRScannerThread extends Thread {
    private Webcam webcam;
    private final ImageView viewfinder;
    private volatile boolean isScanning = true;
    private final Consumer<String> onQrDecoded;

    public QRScannerThread(ImageView viewfinder, Consumer<String> onQrDecoded) {
        this.viewfinder = viewfinder;
        this.onQrDecoded = onQrDecoded;
        this.webcam = Webcam.getDefault();

        if (this.webcam != null) {
            if (this.webcam.isOpen()) {
                this.webcam.close();
            }
            this.webcam.setViewSize(WebcamResolution.VGA.getSize());
            this.webcam.open();
        }
    }

    public void stopScanner() {
        // Phase 2 Fix: Signal the loop to stop.
        // The actual closure is now safely handled in the 'finally' block below.
        isScanning = false;
    }

    @Override
    public void run() {
        if (webcam == null) return;

        MultiFormatReader reader = new MultiFormatReader();

        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        reader.setHints(hints);

        int frameSkipCounter = 0;
        System.out.println("[SCANNER START] QR Video stream capturing active. Awaiting matrix target...");

        try {
            while (isScanning && webcam.isOpen()) {
                BufferedImage image = webcam.getImage();
                if (image == null) continue;

                Platform.runLater(() -> viewfinder.setImage(SwingFXUtils.toFXImage(image, null)));

                if (frameSkipCounter++ % 3 == 0) {
                    try {
                        LuminanceSource source = new BufferedImageLuminanceSource(image);
                        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

                        Result result = reader.decode(bitmap);
                        String rawQrContent = result.getText();

                        System.out.println("[SCANNER CAPTURE] Decoded data stream payload: " + rawQrContent);
                        java.awt.Toolkit.getDefaultToolkit().beep();

                        final String cleanStudentNumber;
                        if (rawQrContent.contains("\n")) {
                            cleanStudentNumber = rawQrContent.split("\n")[0].trim();
                        } else if (rawQrContent.contains(",")) {
                            cleanStudentNumber = rawQrContent.split(",")[0].trim();
                        } else {
                            cleanStudentNumber = rawQrContent.trim();
                        }

                        Platform.runLater(() -> {
                            onQrDecoded.accept(cleanStudentNumber);
                        });

                        isScanning = false;

                    } catch (NotFoundException ignored) {
                        // QR Code matrix profile not detected on this specific video frame slice
                    } catch (Exception e) {
                        System.err.println("[SCANNER CRITICAL ERROR] Failure processing hardware decode routing: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            // Phase 2 Fix: GUARANTEED hardware resource release to prevent memory leaks and camera lockups
            if (webcam != null && webcam.isOpen()) {
                System.out.println("[SCANNER STOP] Releasing webcam hardware resources safely...");
                webcam.close();
            }
        }
    }
}