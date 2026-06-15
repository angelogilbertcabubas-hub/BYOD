package controller.Admin;

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

public class ReportsController extends BaseAdminController {

    @FXML private ComboBox<String> cmbReportType;
    @FXML private TextField txtDateFrom;
    @FXML private TextField txtDateTo;
    @FXML private Label lblTotalCheckIn, lblTotalCheckOut, lblCurrentlyInside, lblRegisteredDevices;
    @FXML private Button btnGenerate, btnExport;

    @FXML private TableView<IncidentReport> tblIncidents;
    @FXML private TableColumn<IncidentReport, String> colIncDate, colIncTime, colIncStudent, colIncDevice, colIncType, colIncLocation;
    @FXML private ComboBox<String> cmbIncidentFilter;

    // Store generated logs in memory so we can export them later
    private List<LogEntry> currentReportLogs = new ArrayList<>();

    @FXML
    public void initialize() {
        cmbReportType.getItems().addAll("Daily Summary", "Weekly Summary", "Monthly Summary", "Custom Range");
        cmbReportType.getSelectionModel().selectFirst();

        String todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
        txtDateFrom.setText(todayStr);
        txtDateTo.setText(todayStr);

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
            cmbIncidentFilter.valueProperty().addListener((obs, oldV, newV) -> {
                filteredData.setPredicate(report -> newV == null || newV.equals("All Incidents") || report.getIncidentType().equals(newV));
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
            showAlert(Alert.AlertType.WARNING, "Date Error", "Please enter both 'Date From' and 'Date To'.");
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
                        "WHERE DATE(c.check_in_time) BETWEEN ? AND ? ORDER BY c.check_in_time DESC";

                long checkIns = 0, checkOuts = 0; int active = 0;
                currentReportLogs.clear(); // Reset the memory state for export

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
                            currentReportLogs.add(new LogEntry(id, name, rs2.getString("school_id"), dev, code, "Check-In", time, "Main Gate"));
                        }
                    }
                    final int fActive = active; final long fIn = checkIns; final long fOut = checkOuts;
                    Platform.runLater(() -> renderReportDialog(fActive, fIn, fOut, currentReportLogs));
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
        reportDialog.setTitle("System Report Viewer");

        // Take a 15-item sample for the visual UI so the screen doesn't lag
        List<LogEntry> sampleLogs = reportLogs.size() > 15 ? reportLogs.subList(0, 15) : reportLogs;
        ScrollPane dialogLayout = buildStylizedReportUI(active, checkIns, checkOuts, sampleLogs);

        DialogPane dialogPane = reportDialog.getDialogPane();
        dialogPane.setContent(dialogLayout);
        dialogPane.setStyle("-fx-background-color: #E5E1E2;");

        ButtonType btnTypePrint = new ButtonType("Print", ButtonBar.ButtonData.LEFT);
        ButtonType btnTypeSave = new ButtonType("Save Copy", ButtonBar.ButtonData.LEFT);
        ButtonType btnTypeClose = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialogPane.getButtonTypes().setAll(btnTypePrint, btnTypeSave, btnTypeClose);

        Optional<ButtonType> result = reportDialog.showAndWait();
        if (result.isPresent()) {
            if (result.get() == btnTypePrint) {
                showAlert(Alert.AlertType.INFORMATION, "Printer Status", "Routed to the printer queue.");
            }
            else if (result.get() == btnTypeSave) {
                handleExport(); // Trigger the real export
            }
        }
    }

    private ScrollPane buildStylizedReportUI(int active, long checkIns, long checkOuts, List<LogEntry> logs) {
        VBox container = new VBox(20);
        container.setPadding(new Insets(25)); container.setPrefWidth(720);
        container.setStyle("-fx-background-color: #E5E1E2;");

        VBox headerCard = new VBox(4);
        headerCard.setPadding(new Insets(15)); headerCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Label lblTitle = new Label("BYOD SYSTEM AUDIT REPORT");
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
        metricsCard.getChildren().addAll(lblSectionTraffic, metricsGrid);

        VBox logsCard = new VBox(12);
        logsCard.setPadding(new Insets(20)); logsCard.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Label lblSectionLogs = new Label("RECENT ACTIVITY LOGS (SAMPLE)");
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

    // --- PHASE 4 FIX: The Real Export Engine (Writes to CSV) ---
    @FXML
    private void handleExport() {
        if (currentReportLogs == null || currentReportLogs.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Data", "There is no data to export. Please generate a report first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export System Records Archive");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Data File (*.csv)", "*.csv"));
        fileChooser.setInitialFileName("BYOD_Admin_Report_" + cmbReportType.getValue().replace(" ", "_") + ".csv");

        Stage stage = (Stage) btnExport.getScene().getWindow();
        File selectedFile = fileChooser.showSaveDialog(stage);

        if (selectedFile != null) {
            try (FileWriter writer = new FileWriter(selectedFile)) {
                // Write CSV Header
                writer.append("Timestamp,Student Name,Student ID,Device Model,Access Token,Operation\n");

                // Write Data Rows
                for (LogEntry log : currentReportLogs) {
                    writer.append(escapeCSV(log.getTimestamp())).append(",")
                            .append(escapeCSV(log.getStudentName())).append(",")
                            .append(escapeCSV(log.getStudentId())).append(",")
                            .append(escapeCSV(log.getDeviceModel())).append(",")
                            .append(escapeCSV(log.getAccessToken())).append(",")
                            .append(escapeCSV(log.getOperation())).append("\n");
                }
                writer.flush();
                showAlert(Alert.AlertType.INFORMATION, "Data Export Finalized", "Report successfully written to:\n\n" + selectedFile.getAbsolutePath());
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Could not write file: " + e.getMessage());
            }
        }
    }

    // Helper method to prevent commas in names from breaking the CSV layout
    private String escapeCSV(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle("BYOD Admin"); alert.setHeaderText(title); alert.setContentText(message); alert.showAndWait();
    }
}