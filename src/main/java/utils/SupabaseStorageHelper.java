package utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.nio.file.Files;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SupabaseStorageHelper {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String SUPABASE_URL = dotenv.get("SUPABASE_URL");
    private static final String SUPABASE_KEY = dotenv.get("SUPABASE_SERVICE_KEY");

    public static String uploadImage(File file, String prefix) {
        // Architectural Safeguard: 5MB File Size Limit
        if (file.length() > 5242880) {
            System.err.println("Upload aborted: File exceeds 5MB limit.");
            return prefix.equals("STU") ? "default_student.png" : "default_device.png";
        }

        try {
            String extension = ".png";
            int i = file.getName().lastIndexOf('.');
            if (i > 0) extension = file.getName().substring(i);

            String fileName = prefix + "_" + System.currentTimeMillis() + extension;
            byte[] fileBytes = Files.readAllBytes(file.toPath());

            // Points directly to the 'byod-images' bucket you created!
            String uploadEndpoint = SUPABASE_URL + "/storage/v1/object/byod-images/" + fileName;

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadEndpoint))
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .header("apikey", SUPABASE_KEY)
                    .header("Content-Type", getMimeType(extension))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                // Return the public web link to save in your SQL database!
                return SUPABASE_URL + "/storage/v1/object/public/byod-images/" + fileName;
            } else {
                System.err.println("Cloud Upload Failed: " + response.body());
                return prefix.equals("STU") ? "default_student.png" : "default_device.png";
            }

        } catch (Exception e) {
            System.err.println("Network error during cloud upload.");
            e.printStackTrace();
            return prefix.equals("STU") ? "default_student.png" : "default_device.png";
        }
    }

    private static String getMimeType(String extension) {
        if (extension.equalsIgnoreCase(".jpg") || extension.equalsIgnoreCase(".jpeg")) return "image/jpeg";
        if (extension.equalsIgnoreCase(".webp")) return "image/webp";
        return "image/png";
    }
}