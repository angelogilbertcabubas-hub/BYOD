package controller.Security;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class MonitoringLogsController {

    @FXML private TextField txtSearch;
    @FXML private Button btnFilter;
    @FXML private Label lblPagination;
    @FXML private Button btnPage1;
    @FXML private Button btnPage2;
    @FXML private Button btnPage3;

    private int currentPage = 1;
    private static final int TOTAL_PAGES = 3;

    @FXML public void initialize() {
        txtSearch.setOnKeyReleased(e -> handleSearch());
    }

    @FXML private void handleSearch() { System.out.println("Searching: " + txtSearch.getText()); }
    @FXML private void handleFilter() { showAlert("Filter coming soon."); }

    @FXML private void handlePrevPage() { if (currentPage > 1) { currentPage--; updateActivePage(); } }
    @FXML private void handleNextPage() { if (currentPage < TOTAL_PAGES) { currentPage++; updateActivePage(); } }
    @FXML private void handlePage1() { currentPage = 1; updateActivePage(); }
    @FXML private void handlePage2() { currentPage = 2; updateActivePage(); }
    @FXML private void handlePage3() { currentPage = 3; updateActivePage(); }

    private void updateActivePage() {
        String a = "-fx-background-color: #7B0D0D; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;";
        String i = "-fx-background-color: white; -fx-text-fill: #555555; -fx-font-size: 13px; -fx-background-radius: 6; -fx-border-color: #dddddd; -fx-border-radius: 6; -fx-border-width: 1.2; -fx-padding: 4 10; -fx-cursor: hand;";
        btnPage1.setStyle(currentPage == 1 ? a : i);
        btnPage2.setStyle(currentPage == 2 ? a : i);
        btnPage3.setStyle(currentPage == 3 ? a : i);
    }

    @FXML private void goToDashboard()      { navigateTo("SecurityDashboard.fxml"); }
    @FXML private void goToCheckInOut()     { navigateTo("CheckInOut.fxml"); }
    @FXML private void goToActiveDevices()  { navigateTo("ActiveDevices.fxml"); }
    @FXML private void goToReports()        { navigateTo("Reports.fxml"); }
    @FXML private void handleLogout()       { System.out.println("Logout"); }

    private void navigateTo(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/byod/" + fxml));
            Stage stage = (Stage) txtSearch.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1024, 768));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("BYOD"); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }
}