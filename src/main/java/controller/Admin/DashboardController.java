package controller.Admin;

import com.example.byod.LogEntry;
import javafx.scene.chart.*;
import utils.DataStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import utils.DatabaseHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DashboardController extends BaseAdminController {

    @FXML private Label lblTotalStudents;
    @FXML private Label lblRegisteredDevices;
    @FXML private Label lblDevicesInside;
    @FXML private Label lblTodayLogs;

    @FXML private BarChart<String, Number> ingressEgressChart;
    @FXML private CategoryAxis xAxisChart;
    @FXML private NumberAxis yAxisChart;
    @FXML private VBox chartPlaceholder;

    @FXML private TableView<LogEntry> miniLogsTable;
    @FXML private TableColumn<LogEntry, String> colMiniName;
    @FXML private TableColumn<LogEntry, String> colMiniAction;
    @FXML private TableColumn<LogEntry, String> colMiniTime;

    @FXML
    public void initialize() {
        lblTotalStudents.setText("Loading...");
        lblRegisteredDevices.setText("Loading...");
        lblDevicesInside.setText("Loading...");
        lblTodayLogs.setText("Loading...");

        chartPlaceholder.setVisible(true);
        ingressEgressChart.setAnimated(false);

        colMiniName.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        colMiniAction.setCellValueFactory(new PropertyValueFactory<>("operation"));
        colMiniTime.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // --- UI/UX UPGRADE: OVERRIDING COLUMN WIDTHS ---
        colMiniTime.setMinWidth(95);
        colMiniTime.setPrefWidth(100);
        colMiniAction.setMinWidth(65);
        colMiniAction.setPrefWidth(70);

        // --- UI/UX UPGRADE: COMPACT DATA STACKING ---

        // 1. Name Column: Stacks Student Name over the Device Model
        colMiniName.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    LogEntry log = getTableRow().getItem();

                    Label lblName = new Label(item);
                    lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #222222;");
                    // FIX: Removed setWrapText(true) so the row heights stay compact!

                    Label lblDevice = new Label(log.getDeviceModel() != null ? log.getDeviceModel() : "Unknown Device");
                    lblDevice.setStyle("-fx-font-size: 9px; -fx-text-fill: #888888;");
                    // FIX: Removed setWrapText(true) here as well

                    VBox vbox = new VBox(2, lblName, lblDevice);
                    vbox.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(vbox);
                    setText(null);
                }
            }
        });

        // 2. Action Column: Converts text into colored CSS Badges (IN/OUT)
        colMiniAction.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label();
                    if (item.equalsIgnoreCase("Check-In")) {
                        badge.setText("IN");
                        badge.setStyle("-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px;");
                    } else if (item.equalsIgnoreCase("Check-Out")) {
                        badge.setText("OUT");
                        badge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 10px;");
                    } else { // Handles Security Alerts
                        badge.setText(item.toUpperCase());
                        badge.setStyle("-fx-background-color: #FFEBEE; -fx-text-fill: #C62828; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6; -fx-font-size: 9px;");
                    }
                    setGraphic(badge);
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                }
            }
        });

        // 3. Time Column: Stacks Date (small) on top of Time (Bold) with extra padding
        colMiniTime.setCellFactory(column -> new TableCell<LogEntry, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String[] parts = item.split(" ", 2);
                    if (parts.length == 2) {
                        Label lblDate = new Label(parts[0]);
                        lblDate.setStyle("-fx-font-size: 9px; -fx-text-fill: #999999;");
                        Label lblTime = new Label(parts[1]);
                        lblTime.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #444444;");

                        VBox vbox = new VBox(2, lblDate, lblTime);
                        vbox.setAlignment(Pos.CENTER);
                        vbox.setPadding(new Insets(0, 5, 0, 5));

                        setGraphic(vbox);
                        setText(null);
                    } else {
                        setText(item);
                        setGraphic(null);
                        setStyle("-fx-alignment: CENTER; -fx-font-size: 11px;");
                    }
                }
            }
        });

        // ----------------------------------------------------

        Thread dataLoadThread = new Thread(() -> {
            DataStore store = DataStore.getInstance();

            Platform.runLater(() -> {
                lblTotalStudents.setText(String.valueOf(store.getStudentsList().size()));
                lblRegisteredDevices.setText(String.valueOf(store.getDevicesList().size()));
                lblDevicesInside.setText(String.valueOf(store.getActiveDevicesList().size()));
                lblTodayLogs.setText(String.valueOf(store.getMonitoringLogsList().size()));
                miniLogsTable.setItems(store.getMonitoringLogsList());
            });
        });

        dataLoadThread.setDaemon(true);
        dataLoadThread.start();

        setupSampleData();
        startChartAutoRefresh();
    }

    private void setupSampleData() {
        String query = "SELECT TO_CHAR(check_in_time, 'HH12 AM') as hour_slot, " +
                "       COUNT(CASE WHEN check_out_time IS NULL THEN 1 END) as ingress_count, " +
                "       COUNT(CASE WHEN check_out_time IS NOT NULL THEN 1 END) as egress_count " +
                "FROM check_in_out " +
                "WHERE DATE(check_in_time) = CURRENT_DATE " +
                "GROUP BY hour_slot " +
                "ORDER BY MIN(check_in_time)";

        XYChart.Series<String, Number> ingressSeries = new XYChart.Series<>();
        ingressSeries.setName("Check-In (Ingress)");

        XYChart.Series<String, Number> egressSeries = new XYChart.Series<>();
        egressSeries.setName("Check-Out (Egress)");

        new Thread(() -> {
            try(Connection connection = DatabaseHelper.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                ResultSet rs = preparedStatement.executeQuery()){
                boolean hasData = false;

                while (rs.next()){
                    String hour = rs.getString("hour_slot");
                    int ingress = rs.getInt("ingress_count");
                    int egress = rs.getInt("egress_count");
                    hasData = true;

                    Platform.runLater(() -> {
                        ingressSeries.getData().add(new XYChart.Data<>(hour, ingress));
                        egressSeries.getData().add(new XYChart.Data<>(hour, egress));
                    });
                }

                final boolean chartHasData = hasData;
                Platform.runLater(() -> {
                    if(chartHasData){
                        ingressEgressChart.getData().clear();
                        ingressEgressChart.getData().addAll(ingressSeries, egressSeries);
                        chartPlaceholder.setVisible(false);
                        ingressEgressChart.setVisible(true);

                        ingressSeries.getData().forEach(d -> d.getNode().setStyle("-fx-bar-fill: #500A0E"));
                        egressSeries.getData().forEach(d -> d.getNode().setStyle("-fx-bar-fill: #C5A059"));

                        int maxVal = 0;
                        for(XYChart.Data<String, Number> d : ingressSeries.getData())
                            maxVal = Math.max(maxVal, d.getYValue().intValue());
                        for(XYChart.Data<String, Number> d: egressSeries.getData())
                            maxVal = Math.max(maxVal, d.getYValue().intValue());

                        yAxisChart.setAutoRanging(false);
                        yAxisChart.setLowerBound(0);
                        yAxisChart.setUpperBound(maxVal + Math.max(5, maxVal / 2));
                        yAxisChart.setTickUnit(Math.max(1, maxVal / 5));
                        yAxisChart.setForceZeroInRange(true);
                    } else{
                        chartPlaceholder.setVisible(true);
                    }
                });
            } catch (Exception e){
                e.printStackTrace();
            }
        }).start();
    }

    private void startChartAutoRefresh(){
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(
                () -> Platform.runLater(this::setupSampleData),
                60, 60, TimeUnit.SECONDS
        );
    }
}