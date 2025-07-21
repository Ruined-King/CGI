import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.*;
import java.util.stream.Collectors;

public class StatisticsManager {
    private final VBox statisticsSection;
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();
    private ComboBox<String> columnSelector;
    private Label statisticsResultLabel;
    private Button calculateButton;
    private FilterManager filterManager; // For filtered data support

    public StatisticsManager(VBox statisticsSection) {
        this.statisticsSection = statisticsSection;
        initializeStatisticsSection();
    }

    private void initializeStatisticsSection() {
        Label statsLabel = new Label("📊 Statistical Analysis:");
        statsLabel.setTextFill(Color.WHITE);
        statsLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));

        // Column selector
        HBox columnSelectorBox = new HBox(10);
        Label columnLabel = new Label("Column:");
        columnLabel.setTextFill(Color.WHITE);
        columnLabel.setFont(Font.font("Verdana", 12));

        columnSelector = new ComboBox<>();
        columnSelector.setPromptText("Select numeric column...");
        columnSelector.setPrefWidth(200);
        String comboBoxStyle = "-fx-background-color: linear-gradient(to right, #ffffff, #ff4444); " +
                "-fx-text-fill: black; -fx-border-color: #cc0000; -fx-border-width: 2px; " +
                "-fx-font-size: 12px;";
        columnSelector.setStyle(comboBoxStyle);

        columnSelectorBox.getChildren().addAll(columnLabel, columnSelector);

        // Calculate button
        calculateButton = new Button("📈 CALCULATE STATISTICS");
        calculateButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-padding: 8 16 8 16; " +
                "-fx-background-radius: 6;");
        calculateButton.setOnAction(e -> calculateStatistics());

        // Results label
        statisticsResultLabel = new Label("");
        statisticsResultLabel.setTextFill(Color.LIGHTBLUE);
        statisticsResultLabel.setFont(Font.font("Verdana", 12));
        statisticsResultLabel.setWrapText(true);

        statisticsSection.getChildren().addAll(
                statsLabel,
                columnSelectorBox,
                calculateButton,
                statisticsResultLabel
        );

        statisticsSection.setStyle("-fx-padding: 15; -fx-background-color: #2c3e50; " +
                "-fx-background-radius: 8; -fx-spacing: 10;");
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = new ArrayList<>(headers);
        this.allRows = new ArrayList<>(allRows);

        // Populate column selector with all columns
        Platform.runLater(() -> {
            columnSelector.setItems(FXCollections.observableArrayList(headers));
        });
    }

    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    private void calculateStatistics() {
        String selectedColumn = columnSelector.getValue();
        if (selectedColumn == null || selectedColumn.trim().isEmpty()) {
            statisticsResultLabel.setText("⚠️ Please select a column first!");
            statisticsResultLabel.setTextFill(Color.ORANGE);
            return;
        }

        int columnIndex = headers.indexOf(selectedColumn);
        if (columnIndex == -1) {
            statisticsResultLabel.setText("❌ Column not found!");
            statisticsResultLabel.setTextFill(Color.RED);
            return;
        }

        // Get data (filtered if filter manager is available)
        List<ObservableList<String>> dataToAnalyze = getDataToAnalyze();

        // Extract numeric values from the selected column
        List<Double> numericValues = extractNumericValues(dataToAnalyze, columnIndex);

        if (numericValues.isEmpty()) {
            statisticsResultLabel.setText("❌ No numeric values found in column '" + selectedColumn + "'");
            statisticsResultLabel.setTextFill(Color.RED);
            return;
        }

        // Calculate statistics
        StatisticsResult stats = calculateAllStatistics(numericValues);

        // Display results
        displayStatistics(stats, selectedColumn, numericValues.size(), dataToAnalyze.size());
    }

    private List<ObservableList<String>> getDataToAnalyze() {
        // If filter manager is available and has active filters, use filtered data
        if (filterManager != null && filterManager.hasActiveFilters()) {
            return filterManager.getFilteredRows();
        }
        // Otherwise use all data
        return allRows;
    }

    private List<Double> extractNumericValues(List<ObservableList<String>> data, int columnIndex) {
        List<Double> numericValues = new ArrayList<>();

        for (ObservableList<String> row : data) {
            if (columnIndex < row.size()) {
                String value = row.get(columnIndex);
                if (value != null && !value.trim().isEmpty()) {
                    try {
                        // Handle different number formats
                        String cleanValue = value.trim()
                                .replace(",", ".")  // Handle comma as decimal separator
                                .replaceAll("[^0-9.-]", ""); // Remove non-numeric characters except . and -

                        if (!cleanValue.isEmpty()) {
                            double numValue = Double.parseDouble(cleanValue);
                            if (!Double.isNaN(numValue) && !Double.isInfinite(numValue)) {
                                numericValues.add(numValue);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numeric values
                    }
                }
            }
        }

        return numericValues;
    }

    private StatisticsResult calculateAllStatistics(List<Double> values) {
        StatisticsResult result = new StatisticsResult();

        if (values.isEmpty()) return result;

        // Sort values for percentile calculations
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);

        // Basic statistics
        result.count = values.size();
        result.min = sortedValues.get(0);
        result.max = sortedValues.get(sortedValues.size() - 1);
        result.sum = values.stream().mapToDouble(Double::doubleValue).sum();
        result.mean = result.sum / result.count;

        // Median
        if (result.count % 2 == 0) {
            result.median = (sortedValues.get(result.count / 2 - 1) + sortedValues.get(result.count / 2)) / 2.0;
        } else {
            result.median = sortedValues.get(result.count / 2);
        }

        // Standard deviation and variance
        double sumSquaredDiffs = values.stream()
                .mapToDouble(v -> Math.pow(v - result.mean, 2))
                .sum();
        result.variance = sumSquaredDiffs / result.count;
        result.standardDeviation = Math.sqrt(result.variance);

        // Range
        result.range = result.max - result.min;

        // Quartiles
        result.q1 = calculatePercentile(sortedValues, 25);
        result.q3 = calculatePercentile(sortedValues, 75);
        result.iqr = result.q3 - result.q1;

        return result;
    }

    private double calculatePercentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) return 0.0;

        double index = (percentile / 100.0) * (sortedValues.size() - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedValues.get(lowerIndex);
        }

        double lowerValue = sortedValues.get(lowerIndex);
        double upperValue = sortedValues.get(upperIndex);
        double weight = index - lowerIndex;

        return lowerValue + weight * (upperValue - lowerValue);
    }

    private void displayStatistics(StatisticsResult stats, String columnName, int numericCount, int totalRows) {
        StringBuilder result = new StringBuilder();

        result.append("📊 Statistics for '").append(columnName).append("':\n\n");

        // Data info
        result.append("📈 Data Overview:\n");
        result.append(String.format("  • Total rows analyzed: %d\n", totalRows));
        result.append(String.format("  • Numeric values found: %d\n", numericCount));
        result.append(String.format("  • Non-numeric/empty: %d\n\n", totalRows - numericCount));

        // Central tendency
        result.append("🎯 Central Tendency:\n");
        result.append(String.format("  • Mean (Average): %.2f\n", stats.mean));
        result.append(String.format("  • Median: %.2f\n", stats.median));
        result.append(String.format("  • Sum: %.2f\n\n", stats.sum));

        // Range and spread
        result.append("📏 Range & Spread:\n");
        result.append(String.format("  • Minimum: %.2f\n", stats.min));
        result.append(String.format("  • Maximum: %.2f\n", stats.max));
        result.append(String.format("  • Range: %.2f\n", stats.range));
        result.append(String.format("  • Standard Deviation: %.2f\n", stats.standardDeviation));
        result.append(String.format("  • Variance: %.2f\n\n", stats.variance));

        // Quartiles
        result.append("📊 Quartiles:\n");
        result.append(String.format("  • Q1 (25th percentile): %.2f\n", stats.q1));
        result.append(String.format("  • Q3 (75th percentile): %.2f\n", stats.q3));
        result.append(String.format("  • IQR (Interquartile Range): %.2f\n", stats.iqr));

        // Check if filters are applied
        if (filterManager != null && filterManager.hasActiveFilters()) {
            result.append("\n🔍 Note: Statistics calculated on filtered data");
        }

        statisticsResultLabel.setText(result.toString());
        statisticsResultLabel.setTextFill(Color.LIGHTGREEN);
    }

    // Getter methods
    public VBox getStatisticsSection() {
        return statisticsSection;
    }

    public Button getCalculateButton() {
        return calculateButton;
    }

    // Helper class to store statistics results
    private static class StatisticsResult {
        int count;
        double min, max, sum, mean, median;
        double variance, standardDeviation, range;
        double q1, q3, iqr;
    }
}