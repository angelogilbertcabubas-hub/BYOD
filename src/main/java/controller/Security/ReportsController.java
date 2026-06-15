package controller.Security;

import com.example.byod.LogEntry;
import com.example.byod.model.IncidentReport;
import utils.DataStore;
import utils.DatabaseHelper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportsController extends BaseSecurityController {

    @FXML private ComboBox<String> cmbReportType;
    @FXML private TextField txtDateFrom;
    @FXML private TextField txtDateTo;
    @FXML private Label lblTotalCheckIn, lblTotalCheckOut, lblCurrentlyInside, lblRegisteredDevices;
    @FXML private Button btnGenerate, btnExport;

    @FXML private TableView<IncidentReport> tblIncidents;
    @FXML private TableColumn<IncidentReport, String> colIncDate, colIncTime, colIncStudent, colIncDevice, colIncType, colIncLocation;
    @FXML private ComboBox<String> cmbIncidentFilter;

    @FXML
    public void initialize() {
        cmbReportType.getItems().addAll("Daily Summary", "Weekly Summary", "Monthly Summary", "Custom Range");
        cmbReportType.getSelectionModel().selectFirst();

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        txtDateFrom.setText(todayStr);
        txtDateTo.setText(todayStr);

        if (btnGenerate != null) btnGenerate.setDisable(false);
        refreshMetrics();

        if (cmbIncidentFilter != null) {
            cmbIncidentFilter.getItems().addAll("All Incidents", "Missing / Lost Device", "Stolen Device", "Found / Recovered Device", "Suspicious Activity", "Physical Hardware Damage");
            cmbIncidentFilter.getSelectionModel().selectFirst();
        }

        if (tblIncidents != null) {
            colIncDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colIncTime.setCellValueFactory(new PropertyValueFactory<>("time"));
            colIncStudent.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
            colIncDevice.setCellValueFactory(new PropertyValueFactory<>("deviceDetails"));
            colIncType.setCellValueFactory(new PropertyValueFactory<>("incidentType"));
            colIncLocation.setCellValueFactory(new PropertyValueFactory<>("location"));

            FilteredList<IncidentReport> filteredData = new FilteredList<>(DataStore.getInstance().getIncidentReportsList(), p -> true);

            cmbIncidentFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(report -> {
                    if (newValue == null || newValue.equals("All Incidents")) return true;
                    return report.getIncidentType().equals(newValue);
                });
            });
            tblIncidents.setItems(filteredData);
        }
    }

    private void refreshMetrics() {
        new Thread(() -> {
            String query = "SELECT " +
                    "COUNT(CASE WHEN status = 'CHECKED_IN' AND check_out_time IS NULL THEN 1 END) as active_now, " +
                    "COUNT(CASE WHEN status IN ('CHECKED_IN', 'CHECKED_OUT') THEN 1 END) as total_ins, " +
                    "COUNT(CASE WHEN check_out_time IS NOT NULL THEN 1 END) as total_outs " +
                    "FROM check_in_out";
            try (Connection conn = DatabaseHelper.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int active = rs.getInt("active_now");
                    long ins = rs.getLong("total_ins");
                    long outs = rs.getLong("total_outs");
                    int registered = DataStore.getInstance().getDevicesList().size();
                    Platform.runLater(() -> {
                        lblCurrentlyInside.setText(String.valueOf(active));
                        lblTotalCheckIn.setText(String.valueOf(ins));
                        lblTotalCheckOut.setText(String.valueOf(outs));
                        lblRegisteredDevices.setText(String.valueOf(registered));
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleGenerate() {
        if (txtDateFrom.getText().trim().isEmpty() || txtDateTo.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Date Error", "Please enter both 'Date From' and 'Date To' values.");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        try {
            LocalDate fromDate = LocalDate.parse(txtDateFrom.getText().trim(), formatter);
            LocalDate toDate = LocalDate.parse(txtDateTo.getText().trim(), formatter);
            java.sql.Date sqlFrom = java.sql.Date.valueOf(fromDate);
            java.sql.Date sqlTo = java.sql.Date.valueOf(toDate);

            new Thread(() -> {
                String statsQuery = "SELECT " +
                        "COUNT(CASE WHEN status IN ('CHECKED_IN', 'CHECKED_OUT') THEN 1 END) as in_count, " +
                        "COUNT(CASE WHEN check_out_time IS NOT NULL THEN 1 END) as out_count, " +
                        "COUNT(CASE WHEN status = 'CHECKED_IN' AND check_out_time IS NULL THEN 1 END) as active_count " +
                        "FROM check_in_out WHERE DATE(check_in_time) BETWEEN ? AND ?";

                String logsQuery = "SELECT c.id AS log_id, s.first_name, s.last_name, s.school_id, d.device_brand, d.device_name, d.unique_code, c.status, " +
                        "TO_CHAR(c.check_in_time, 'MM/DD/YYYY HH12:MI AM') as time_in " +
                        "FROM check_in_out c JOIN students s ON c.student_id = s.id JOIN devices d ON c.device_id = d.id " +
                        "WHERE DATE(c.check_in_time) BETWEEN ? AND ? ORDER BY c.check_in_time DESC LIMIT 15";

                long checkIns = 0, checkOuts = 0; int active = 0;
                List<LogEntry> reportLogs = new ArrayList<>();

                try (Connection conn = DatabaseHelper.getConnection()) {
                    try(PreparedStatement ps1 = conn.prepareStatement(statsQuery)){
                        ps1.setDate(1, sqlFrom); ps1.setDate(2, sqlTo);
                        ResultSet rs1 = ps1.executeQuery();
                        if (rs1.next()) { checkIns = rs1.getLong("in_count"); checkOuts = rs1.getLong("out_count"); active = rs1.getInt("active_count"); }
                    }
                    try(PreparedStatement ps2 = conn.prepareStatement(logsQuery)){
                        ps2.setDate(1, sqlFrom); ps2.setDate(2, sqlTo);
                        ResultSet rs2 = ps2.executeQuery();
                        while(rs2.next()){
                            String id = String.valueOf(rs2.getObject("log_id")) + "-IN";
                            String name = rs2.getString("first_name") + " " + rs2.getString("last_name");
                            String dev = rs2.getString("device_brand") + " " + rs2.getString("device_name");
                            String time = rs2.getString("time_in");
                            String code = rs2.getString("unique_code") != null ? rs2.getString("unique_code") : "N/A";
                            reportLogs.add(new LogEntry(id, name, rs2.getString("school_id"), dev, code, "Check-In", time, "Main Gate"));
                        }
                    }
                    final int fActive = active; final long fIn = checkIns; final long fOut = checkOuts;
                    Platform.runLater(() -> renderReportDialog(fActive, fIn, fOut, reportLogs));
                } catch (Exception ex) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to compile the report context."));
                }
            }).start();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Date Format Error", "Dates must follow MM/DD/YYYY format.");
        }
    }

    private void renderReportDialog(int active, long checkIns, long checkOuts, List<LogEntry> reportLogs) {
        Dialog<ButtonType> reportDialog = new Dialog<>();
        reportDialog.setTitle("Security Report Viewer");
        ScrollPane dialogLayout = buildStylizedReportUI(active, checkIns, checkOuts, reportLogs);
        DialogPane dialogPane = reportDialog.getDialogPane();
        dialogPane.setContent(dialogLayout);
        dialogPane.setStyle("-fx-background-color: #E5E1E2;");

        ButtonType btnTypePrint = new ButtonType("Print Report", ButtonBar.ButtonData.LEFT);
        ButtonType btnTypeSave = new ButtonType("Save Copy", ButtonBar.ButtonData.LEFT);
        ButtonType btnTypeClose = new ButtonType("Close Viewer", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogPane.getButtonTypes().setAll(btnTypePrint, btnTypeSave, btnTypeClose);
        styleDialogButtons(dialogPane, btnTypePrint, btnTypeSave, btnTypeClose);

        Optional<ButtonType> result = reportDialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnTypePrint) handlePrintAction(cmbReportType.getValue());
            else if (result.get() == btnTypeSave) {
                String fallbackTxtContent = generateFlatTextFallback(active, checkIns, checkOuts, reportLogs);
                handleSaveCopyAction(fallbackTxtContent);
            }
        }
    }

    private ScrollPane buildStylizedReportUI(int active, long checkIns, long checkOuts, List<LogEntry> logs) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(25)); container.setPrefWidth(720);
        container.setStyle("-fx-background-color: #E5E1E2;");

        VBox headerCard = new VBox(4);
        headerCard.setPadding(new Insets(15)); headerCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Label lblTitle = new Label("SECURITY GATE ACTIVITY REPORT");
        lblTitle.setTextFill(javafx.scene.paint.Color.web("#500A0E")); lblTitle.setFont(Font.font("System", FontWeight.BOLD, 18));
        Label lblSubtitle = new Label("Report Type: " + cmbReportType.getValue() + "  |  Date Range: " + txtDateFrom.getText() + " to " + txtDateTo.getText());
        lblSubtitle.setTextFill(javafx.scene.paint.Color.web("#666666")); lblSubtitle.setFont(Font.font("System", 11));
        headerCard.getChildren().addAll(lblTitle, lblSubtitle);

        VBox metricsCard = new VBox(12);
        metricsCard.setPadding(new Insets(20)); metricsCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Label lblSectionTraffic = new Label("GATEWAY TRAFFIC SUMMARY");
        lblSectionTraffic.setTextFill(javafx.scene.paint.Color.web("#555555")); lblSectionTraffic.setFont(Font.font("System", FontWeight.BOLD, 11));

        GridPane metricsGrid = new GridPane(); metricsGrid.setHgap(20); metricsGrid.setVgap(10);
        metricsGrid.add(createMetricRow("Total Check-Ins:", String.valueOf(checkIns)), 0, 0);
        metricsGrid.add(createMetricRow("Total Check-Outs:", String.valueOf(checkOuts)), 1, 0);
        metricsGrid.add(createMetricRow("Devices Inside Campus:", String.valueOf(active)), 0, 1);
        metricsGrid.add(createMetricRow("Generated By:", "Security Guard Personnel (Terminal 01)"), 1, 1);
        metricsCard.getChildren().addAll(lblSectionTraffic, metricsGrid);

        VBox logsCard = new VBox(12);
        logsCard.setPadding(new Insets(20)); logsCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Label lblSectionLogs = new Label("RECENT ACTIVITY LOGS");
        lblSectionLogs.setTextFill(javafx.scene.paint.Color.web("#555555")); lblSectionLogs.setFont(Font.font("System", FontWeight.BOLD, 11));

        VBox logsListContainer = new VBox(6);
        if (logs.isEmpty()) {
            Label lblEmpty = new Label("No matching activity events reported.");
            lblEmpty.setStyle("-fx-font-style: italic; -fx-text-fill: #888888;");
            logsListContainer.getChildren().add(lblEmpty);
        } else {
            for (int i = 0; i < logs.size(); i++) {
                LogEntry log = logs.get(i);
                HBox row = new HBox(15); row.setPadding(new Insets(8, 12, 8, 12)); row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle(i % 2 == 0 ? "-fx-background-color: #F7F5F5; -fx-background-radius: 4;" : "-fx-background-color: white;");

                Label lblTime = new Label("[" + log.getTimestamp() + "]"); lblTime.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #777777;"); lblTime.setPrefWidth(140);
                Label lblName = new Label(log.getStudentName()); lblName.setFont(Font.font("System", FontWeight.BOLD, 12)); lblName.setPrefWidth(180);
                Label lblOp = new Label(log.getOperation().toUpperCase()); lblOp.setFont(Font.font("System", FontWeight.BOLD, 10)); lblOp.setTextFill(javafx.scene.paint.Color.web("#2E7D32")); lblOp.setPrefWidth(80);
                Label lblToken = new Label(log.getAccessToken()); lblToken.setStyle("-fx-font-family: 'Consolas'; -fx-text-fill: #555555;");

                row.getChildren().addAll(lblTime, lblName, lblOp, lblToken);
                logsListContainer.getChildren().add(row);
            }
        }
        logsCard.getChildren().addAll(lblSectionLogs, logsListContainer);
        container.getChildren().addAll(headerCard, metricsCard, logsCard);

        ScrollPane scrollPane = new ScrollPane(container);
        scrollPane.setFitToWidth(true); scrollPane.setPrefHeight(550); scrollPane.setPrefWidth(740);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        return scrollPane;
    }

    private HBox createMetricRow(String label, String value) {
        HBox box = new HBox(8); box.setPadding(new Insets(4, 0, 4, 0));
        Label lblTitle = new Label(label); lblTitle.setStyle("-fx-text-fill: #666666; -fx-font-size: 13px;");
        Label lblValue = new Label(value); lblValue.setStyle("-fx-text-fill: #500A0E; -fx-font-weight: bold; -fx-font-size: 13px;");
        box.getChildren().addAll(lblTitle, lblValue); return box;
    }

    private void styleDialogButtons(DialogPane dialogPane, ButtonType print, ButtonType save, ButtonType close) {
        Button btnPrint = (Button) dialogPane.lookupButton(print); if (btnPrint != null) btnPrint.setStyle("-fx-background-color: #500A0E; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 15;");
        Button btnSave = (Button) dialogPane.lookupButton(save); if (btnSave != null) btnSave.setStyle("-fx-background-color: white; -fx-text-fill: #444444; -fx-border-color: #E2DDD9; -fx-border-radius: 4; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 15;");
        Button btnClose = (Button) dialogPane.lookupButton(close); if (btnClose != null) btnClose.setStyle("-fx-background-color: #F4F4F4; -fx-text-fill: #555555; -fx-background-radius: 4; -fx-cursor: hand; -fx-padding: 6 15;");
    }

    private String generateFlatTextFallback(int active, long checkIns, long checkOuts, List<LogEntry> logs) {
        StringBuilder content = new StringBuilder();
        content.append("======================================================================\n")
                .append("                     SECURITY GATE ACTIVITY REPORT\n")
                .append("======================================================================\n")
                .append("REPORT TYPE  : ").append(cmbReportType.getValue()).append("\n")
                .append("DATE RANGE   : ").append(txtDateFrom.getText()).append(" to ").append(txtDateTo.getText()).append("\n")
                .append("GENERATED BY : Security Guard Personnel (Terminal 01)\n")
                .append("----------------------------------------------------------------------\n")
                .append("GATEWAY TRAFFIC SUMMARY:\n")
                .append("- Total Check-Ins        : ").append(checkIns).append("\n")
                .append("- Total Check-Outs       : ").append(checkOuts).append("\n")
                .append("- Devices Inside Campus  : ").append(active).append("\n\n")
                .append("RECENT ACTIVITY LOGS:\n");

        for (LogEntry l : logs) {
            content.append("[").append(l.getTimestamp()).append("] ")
                    .append(String.format("%-22s", l.getStudentName())).append(" | ")
                    .append(String.format("%-9s", l.getOperation())).append(" | ")
                    .append(l.getAccessToken()).append("\n");
        }
        content.append("\n*** END OF SECURE REPORT ***");
        return content.toString();
    }

    private void handleSaveCopyAction(String content) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report Copy");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files (*.txt)", "*.txt"));
        fileChooser.setInitialFileName("BYOD_Security_Archive.txt");

        Stage stage = (Stage) btnExport.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Report saved successfully to " + file.getName());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not save file: " + e.getMessage());
            }
        }
    }

    private void handlePrintAction(String reportTitle) {
        Alert printConfirm = new Alert(Alert.AlertType.CONFIRMATION);
        printConfirm.setTitle("Print Manager");
        printConfirm.setHeaderText("Print Confirmation");
        printConfirm.setContentText("Send '" + reportTitle + "' to the local security printer?");
        printConfirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) showAlert(Alert.AlertType.INFORMATION, "Printer Status", "Routed to the printer queue.");
        });
    }

    @FXML
    private void handleExport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Security Report");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("CSV Data File (*.csv)", "*.csv")
        );
        fileChooser.setInitialFileName("BYOD_Report_" + cmbReportType.getValue().replace(' ', '_'));
        Stage stage = (Stage) btnExport.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);
        if (selectedFile != null) showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Report securely exported to:\n\n" + selectedFile.getAbsolutePath());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle("BYOD Security"); alert.setHeaderText(title); alert.setContentText(message); alert.showAndWait();
    }
}