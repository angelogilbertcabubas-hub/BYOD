package utils;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.activation.*;
import jakarta.mail.util.ByteArrayDataSource;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Properties;

public class EmailHelper {
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String SENDER_EMAIL = "pup.endpoint.byod@gmail.com";
    private static final String SENDER_PASS = "gvna zass qwce rskc";

    // Validates if credentials are set so DatabaseHelper doesn't crash
    public static boolean isConfigured() {
        return SENDER_EMAIL != null && !SENDER_EMAIL.isEmpty();
    }

    // Generic method to send warning emails
    public static void sendEmail(String toEmail, String subject, String bodyText) {
        // Safety check to prevent crashing if the email is invalid
        if (toEmail == null || toEmail.isEmpty() || !toEmail.contains("@")) {
            System.err.println("[EMAIL ERROR] Invalid recipient email address: " + toEmail);
            return;
        }

        new Thread(() -> {
            try {
                Properties properties = new Properties();
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.host", SMTP_HOST);
                properties.put("mail.smtp.port", SMTP_PORT);

                Session session = Session.getInstance(properties, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(SENDER_EMAIL));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject(subject);
                msg.setText(bodyText);

                Transport.send(msg);
                System.out.println("[EMAIL] Notification sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println("[EMAIL ERROR] Failed to send notification to " + toEmail);
                e.printStackTrace();
            }
        }).start();
    }

    public static void sendQRCode(String toEmail, String studentName, String studentId, Image qrImage) {
        new Thread(() -> {
            try {
                BufferedImage buffered = SwingFXUtils.fromFXImage(qrImage, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(buffered, "png", baos);
                byte[] imageBytes = baos.toByteArray();

                Properties properties = new Properties();
                properties.put("mail.smtp.auth", "true");
                properties.put("mail.smtp.starttls.enable", "true");
                properties.put("mail.smtp.host", SMTP_HOST);
                properties.put("mail.smtp.port", SMTP_PORT);

                Session session = Session.getInstance(properties, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASS);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(SENDER_EMAIL));
                msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject("BYOD System - Your Campus Access QR Code");

                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText("Dear, " + studentName + ", \n\n" +
                        "Your BYOD campus access QR code is attached to this email.\n" +
                        "Student ID: " + studentId + "\n\n" +
                        "Present this QR code at the campus gate for device check-in/check-out.\n" +
                        "Keep this confidential.\n\n" +
                        "- Endpoint BYOD System Administration");

                MimeBodyPart imagePart = new MimeBodyPart();
                imagePart.setDataHandler(new DataHandler(
                        new ByteArrayDataSource(imageBytes, "image/png")
                ));
                imagePart.setFileName("QR_" + studentId + ".png");
                imagePart.setDisposition(MimeBodyPart.ATTACHMENT);

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(textPart);
                multipart.addBodyPart(imagePart);
                msg.setContent(multipart);

                Transport.send(msg);
                System.out.println("[EMAIL] QR sent to: " + toEmail);
            } catch (Exception e) {
                System.err.println("[EMAIL ERROR] Failed to send QR to " + toEmail);
                e.printStackTrace();
            }
        }).start();
    }
}