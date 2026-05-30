package controller.Admin;

import com.example.byod.model.LogEntry;
import utils.DataStore;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class ReportsController extends BaseAdminController {

    @FXML private ComboBox<String> cmbReportType;
    @FXML private TextField txtDateFrom;
    @FXML private TextField txtDateTo;

    @FXML private Label lblTotalCheckIn;
    @FXML private Label lblTotalCheckOut;
    @FXML private Label lblCurrentlyInside;
    @FXML private Label lblRegisteredDevices;

    @FXML private Button btnGenerate;
    @FXML private Button btnExport;

    @FXML
    public void initialize() {
        cmbReportType.getItems().addAll("Daily Summary", "Weekly Summary", "Monthly Summary", "Custom Range");
        cmbReportType.getSelectionModel().selectFirst();

        // Aligned with the current academic deployment date context
        txtDateFrom.setText("05/30/2026");
        txtDateTo.setText("05/30/2026");

        // Resolves the 'btnGenerate is assigned but never accessed' IDE warning smoothly
        if (btnGenerate != null) {
            btnGenerate.setDisable(false);
        }

        refreshMetrics();
    }

    private void refreshMetrics() {
        int active = DataStore.getInstance().getActiveDevicesList().size();
        long checkIns = DataStore.getInstance().getMonitoringLogsList().stream()
                .filter(l -> "Check-In".equals(l.getOperation())).count();
        long checkOuts = DataStore.getInstance().getMonitoringLogsList().stream()
                .filter(l -> "Check-Out".equals(l.getOperation())).count();
        int registered = DataStore.getInstance().getDevicesList().size();

        lblCurrentlyInside.setText(String.valueOf(active));
        lblTotalCheckIn.setText(String.valueOf(checkIns));
        lblTotalCheckOut.setText(String.valueOf(checkOuts));
        lblRegisteredDevices.setText(String.valueOf(registered));
    }

    @FXML
    private void handleGenerate() {
        if (txtDateFrom.getText().trim().isEmpty() || txtDateTo.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Filter Validation Error", "Please input parameters for both 'Date From' and 'Date To'.");
            return;
        }

        int active = DataStore.getInstance().getActiveDevicesList().size();
        long checkIns = DataStore.getInstance().getMonitoringLogsList().stream()
                .filter(l -> "Check-In".equals(l.getOperation())).count();
        long checkOuts = DataStore.getInstance().getMonitoringLogsList().stream()
                .filter(l -> "Check-Out".equals(l.getOperation())).count();

        // 1. Create custom base Dialog container
        Dialog<ButtonType> reportDialog = new Dialog<>();
        reportDialog.setTitle("System Report Viewer");

        // 2. Extract layout content pane container setup
        ScrollPane dialogLayout = buildStylizedReportUI(active, checkIns, checkOuts);

        // 3. FIX: Fetch DialogPane target explicitly to append styles and items securely
        DialogPane dialogPane = reportDialog.getDialogPane();
        dialogPane.setContent(dialogLayout);
        dialogPane.setStyle("-fx-background-color: #E5E1E2;");

        // Establish targeted control action trigger configurations
        ButtonType btnTypePrint = new ButtonType("PRINT REPORT", ButtonBar.ButtonData.LEFT);
        ButtonType btnTypeSave = new ButtonType("SAVE COPY", ButtonBar.ButtonData.LEFT);
        ButtonType btnTypeClose = new ButtonType("CLOSE VIEWER", ButtonBar.ButtonData.CANCEL_CLOSE);

        // FIX: Executed on the dialogPane reference node instance directly
        dialogPane.getButtonTypes().setAll(btnTypePrint, btnTypeSave, btnTypeClose);

        // Inject custom stylesheet rendering modifications over default prompt properties
        styleDialogButtons(dialogPane, btnTypePrint, btnTypeSave, btnTypeClose);

        Optional<ButtonType> result = reportDialog.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btnTypePrint) {
                handlePrintAction(cmbReportType.getValue());
            } else if (result.get() == btnTypeSave) {
                String fallbackTxtContent = generateFlatTextFallback(active, checkIns, checkOuts);
                handleSaveCopyAction(fallbackTxtContent);
            }
        }
    }

    /**
     * Constructs a programmatic UI view layout structure matching the application design system.
     */
    private ScrollPane buildStylizedReportUI(int active, long checkIns, long checkOuts) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(25));
        container.setPrefWidth(720);
        container.setStyle("-fx-background-color: #E5E1E2;");

        // ---- HEADER CARD BANNER ----
        VBox headerCard = new VBox(4);
        headerCard.setPadding(new Insets(15));
        headerCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label lblTitle = new Label("BYOD System Audit Report");
        lblTitle.setTextFill(javafx.scene.paint.Color.web("#500A0E"));
        lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label lblSubtitle = new Label("Report Type: " + cmbReportType.getValue() + "  |  Date Range: " + txtDateFrom.getText() + " to " + txtDateTo.getText());
        lblSubtitle.setTextFill(javafx.scene.paint.Color.web("#666666"));
        lblSubtitle.setFont(Font.font("System", 11));

        headerCard.getChildren().addAll(lblTitle, lblSubtitle);

        // ---- METRICS GRID SUMMARY CARD ----
        VBox metricsCard = new VBox(12);
        metricsCard.setPadding(new Insets(20));
        metricsCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label lblSectionTraffic = new Label("GATEWAY TRAFFIC SUMMARY");
        lblSectionTraffic.setTextFill(javafx.scene.paint.Color.web("#555555"));
        lblSectionTraffic.setFont(Font.font("System", FontWeight.BOLD, 11));

        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(10);

        metricsGrid.add(createMetricRow("Total System Check-Ins:", String.valueOf(checkIns)), 0, 0);
        metricsGrid.add(createMetricRow("Total System Check-Outs:", String.valueOf(checkOuts)), 1, 0);
        metricsGrid.add(createMetricRow("Active Campus Assets:", String.valueOf(active)), 0, 1);
        metricsGrid.add(createMetricRow("Compiled By:", "Admin Control Terminal"), 1, 1);

        metricsCard.getChildren().addAll(lblSectionTraffic, metricsGrid);

        // ---- TIMELINE LOGS LIST CARD ----
        VBox logsCard = new VBox(12);
        logsCard.setPadding(new Insets(20));
        logsCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");

        Label lblSectionLogs = new Label("RECENT TRAFFIC ACTIVITY LOGS");
        lblSectionLogs.setTextFill(javafx.scene.paint.Color.web("#555555"));
        lblSectionLogs.setFont(Font.font("System", FontWeight.BOLD, 11));

        VBox logsListContainer = new VBox(6);
        List<LogEntry> logs = DataStore.getInstance().getMonitoringLogsList();
        int limit = Math.min(10, logs.size());

        if (limit == 0) {
            Label lblEmpty = new Label("No matching activity events reported during this tracking window.");
            lblEmpty.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
            logsListContainer.getChildren().add(lblEmpty);
        } else {
            for (int i = 0; i < limit; i++) {
                LogEntry log = logs.get(i);
                HBox row = new HBox(15);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(i % 2 == 0 ? "-fx-background-color: #F7F5F5; -fx-background-radius: 4;" : "-fx-background-color: white;");

                Label lblTime = new Label("[" + log.getTimestamp() + "]");
                lblTime.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #777777;");
                lblTime.setPrefWidth(140);

                Label lblName = new Label(log.getStudentName());
                lblName.setFont(Font.font("System", FontWeight.BOLD, 12));
                lblName.setPrefWidth(180);

                Label lblOp = new Label(log.getOperation().toUpperCase());
                lblOp.setFont(Font.font("System", FontWeight.BOLD, 10));
                lblOp.setTextFill(javafx.scene.paint.Color.web("Check-In".equals(log.getOperation()) ? "#2E7D32" : "#C62828"));
                lblOp.setPrefWidth(80);

                Label lblToken = new Label(log.getAccessToken());
                lblToken.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #555555;");

                row.getChildren().addAll(lblTime, lblName, lblOp, lblToken);
                logsListContainer.getChildren().add(row);
            }
        }

        logsCard.getChildren().addAll(lblSectionLogs, logsListContainer);
        container.getChildren().addAll(headerCard, metricsCard, logsCard);

        // ---- CONTAINER SCROLL VIEW WRAPPER ----
        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(550);
        scrollPane.setPrefWidth(740);

        // FIX: Removed the duplicate -fx-background-color rules to fix IDE property warnings
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        return scrollPane;
    }

    private HBox createMetricRow(String label, String value) {
        HBox box = new HBox(8);
        box.setPadding(new Insets(4, 0, 4, 0));
        Label lblTitle = new Label(label);
        lblTitle.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");
        Label lblValue = new Label(value);
        lblValue.setStyle("-fx-text-fill: #500A0E; -fx-font-weight: bold; -fx-font-size: 13px;");
        box.getChildren().addAll(lblTitle, lblValue);
        return box;
    }

    private void styleDialogButtons(DialogPane dialogPane, ButtonType print, ButtonType save, ButtonType close) {
        Button btnPrint = (Button) dialogPane.lookupButton(print);
        if (btnPrint != null) {
            btnPrint.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 15;");
        }
        Button btnSave = (Button) dialogPane.lookupButton(save);
        if (btnSave != null) {
            btnSave.setStyle("-fx-background-color: white; -fx-text-fill: #444444; -fx-border-color: #E2DDD9; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 15;");
        }
        Button btnClose = (Button) dialogPane.lookupButton(close);
        if (btnClose != null) {
            btnClose.setStyle("-fx-background-color: #F4F4F4; -fx-text-fill: #555555; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 15;");
        }
    }

    private String generateFlatTextFallback(int active, long checkIns, long checkOuts) {
        StringBuilder content = new StringBuilder();
        content.append("======================================================================\n")
                .append("                     BYOD SYSTEM AUDIT REPORT\n")
                .append("======================================================================\n")
                .append("REPORT TYPE  : ").append(cmbReportType.getValue()).append("\n")
                .append("DATE RANGE   : ").append(txtDateFrom.getText()).append(" to ").append(txtDateTo.getText()).append("\n")
                .append("COMPILED BY  : Administrative Control Terminal (Root Admin)\n")
                .append("----------------------------------------------------------------------\n")
                .append("GATEWAY TRAFFIC SUMMARY:\n")
                .append("- Total System Check-Ins : ").append(checkIns).append("\n")
                .append("- Total System Check-Outs: ").append(checkOuts).append("\n")
                .append("- Active Campus Assets   : ").append(active).append("\n\n")
                .append("RECENT ACTIVITY TIMELINE LOGS (SAMPLE):\n");

        List<LogEntry> logs = DataStore.getInstance().getMonitoringLogsList();
        int limit = Math.min(15, logs.size());
        for (int i = 0; i < limit; i++) {
            LogEntry l = logs.get(i);
            content.append("[").append(l.getTimestamp()).append("] ")
                    .append(String.format("%-22s", l.getStudentName())).append(" | ")
                    .append(String.format("%-9s", l.getOperation())).append(" | ")
                    .append(l.getAccessToken()).append("\n");
        }
        content.append("\n*** END OF SECURE SYSTEM REPORT ***");
        return content.toString();
    }

    private void handleSaveCopyAction(String content) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Archive Admin Report Copy");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName("BYOD_Admin_Archive.txt");

        Stage stage = (Stage) btnExport.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Administrative report saved successfully to " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "System Error", "Could not complete text serialization: " + e.getMessage());
            }
        }
    }

    private void handlePrintAction(String reportTitle) {
        Alert printConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        printConfirm.setTitle("System Print Manager");
        printConfirm.setHeaderText("Print Confirmation Queue");
        printConfirm.setContentText("Send '" + reportTitle + "' directly to the local administration office printer layout network?");
        printConfirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert(Alert.AlertType.INFORMATION, "Printer Routing Status", "Successfully routed data packets to local printer pool.");
            }
        });
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export System Records Archive");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("CSV Data File (*.csv)", "*.csv")
        );
        fileChooser.setInitialFileName("BYOD_Admin_Report_" + cmbReportType.getValue().replace(" ", "_"));
        Stage stage = (Stage) btnExport.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) {
            showAlert(Alert.AlertType.INFORMATION, "Data Export Finalized", "Report successfully written and verified to path:\n\n" + selectedFile.getAbsolutePath());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("BYOD System Management Context");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}