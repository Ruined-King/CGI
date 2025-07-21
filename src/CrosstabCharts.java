import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.GridPane;
import java.util.LinkedHashMap;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Circle;


import java.util.*;


public class CrosstabCharts {
    private CrosstabData crosstabData;
    private String currentXColumn;
    private String currentYColumn;
    private boolean useMonthlyConversion;
    private boolean includeAllMonths;
    public CrosstabCharts(CrosstabData crosstabData) {
        this.crosstabData = crosstabData;
    }

    private ScrollPane createBarChartPane(boolean totalOnly) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(currentXColumn);
        yAxis.setLabel("Count");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(currentXColumn + " vs " + currentYColumn + " Distribution");
        barChart.setLegendVisible(true);

        if (totalOnly) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Total Count");

            Map<String, Integer> totals = crosstabData.getRowTotals();

            List<String> sortedKeys = new ArrayList<>(totals.keySet());
            if (useMonthlyConversion) {
                sortedKeys = crosstabData.sortMonthYearValues(sortedKeys);
            } else {
                Collections.sort(sortedKeys);
            }

            for (String key : sortedKeys) {
                series.getData().add(new XYChart.Data<>(key, totals.get(key)));
            }
            barChart.getData().add(series);
        } else {
            Map<String, Map<String, Integer>> data = crosstabData.getCrosstabData();

            List<String> sortedXValues = new ArrayList<>(data.keySet());
            if (useMonthlyConversion) {
                sortedXValues = crosstabData.sortMonthYearValues(sortedXValues);
            } else {
                Collections.sort(sortedXValues);
            }

            Set<String> allYValues = new TreeSet<>();
            data.values().forEach(row -> allYValues.addAll(row.keySet()));

            for (String yValue : allYValues) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(yValue);

                for (String xValue : sortedXValues) {
                    Integer count = data.get(xValue).getOrDefault(yValue, 0);
                    series.getData().add(new XYChart.Data<>(xValue, count));
                }
                barChart.getData().add(series);
            }
        }

        barChart.setStyle("-fx-background-color: white;");

        ScrollPane scrollPane = new ScrollPane(barChart);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }
    private ScrollPane createPieChartPane(boolean totalOnly) {
        Map<String, Integer> totals = new HashMap<>();

        if (totalOnly) {
            totals = crosstabData.getColumnTotals();
        } else {
            Map<String, Map<String, Integer>> data = crosstabData.getCrosstabData();
            for (Map<String, Integer> xRow : data.values()) {
                for (Map.Entry<String, Integer> entry : xRow.entrySet()) {
                    totals.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }
        }

        Map<String, Integer> filteredTotals = totals.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20) // Limit to top 20 values to avoid memory issues
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : filteredTotals.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
        }

        PieChart pieChart = new PieChart(pieChartData);
        pieChart.setTitle("Distribution of " + currentYColumn +
                (filteredTotals.size() < totals.size() ? " (Top " + filteredTotals.size() + " values)" : ""));
        pieChart.setLabelLineLength(15); // Increased from 10
        pieChart.setLegendVisible(false); // We'll create custom legend
        pieChart.setPrefSize(800, 600); // Much bigger chart
        pieChart.setMinSize(600, 450);

        String[] enhancedColors = {
                "#e74c3c", "#3498db", "#f39c12", "#2ecc71", "#9b59b6", "#1abc9c", "#e67e22", "#34495e",
                "#f1c40f", "#95a5a6", "#c0392b", "#2980b9", "#d35400", "#27ae60", "#8e44ad", "#16a085",
                "#d68910", "#5d6d7e", "#f7dc6f", "#bb8fce", "#85c1e9", "#f8c471", "#82e0aa", "#a569bd",
                "#5dade2", "#f4d03f", "#d7dbdd", "#ec7063", "#5499c7", "#f6b26b", "#58d68d", "#bb8fce",
                "#7fb3d3", "#f9e79f", "#d2b4de", "#85c1e9", "#f8c471", "#82e0aa", "#a569bd", "#5dade2",
                "#ff6b6b", "#4ecdc4", "#45b7d1", "#f9ca24", "#f0932b", "#eb4d4b", "#6c5ce7", "#a29bfe",
                "#fd79a8", "#e84393", "#00b894", "#00cec9", "#0984e3", "#6c5ce7", "#a29bfe", "#fd79a8",
                "#fdcb6e", "#e17055", "#74b9ff", "#0984e3", "#00b894", "#00cec9", "#a29bfe", "#fd79a8"
        };

        int totalSum = filteredTotals.values().stream().mapToInt(Integer::intValue).sum();

        for (int i = 0; i < pieChart.getData().size(); i++) {
            PieChart.Data data = pieChart.getData().get(i);
            double percentage = (data.getPieValue() / totalSum) * 100;

            String color = enhancedColors[i % enhancedColors.length];
            data.getNode().setStyle("-fx-pie-color: " + color + ";");

            data.setName(data.getName() + " (" + String.format("%.1f%%", percentage) + ")");
        }

        VBox customLegend = createEnhancedPieLegend(pieChart.getData(), enhancedColors);

        HBox mainLayout = new HBox(30);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(20));

        VBox pieContainer = new VBox(10);
        pieContainer.setAlignment(Pos.CENTER);
        pieContainer.getChildren().add(pieChart);

        mainLayout.getChildren().addAll(pieContainer, customLegend);
        mainLayout.setStyle("-fx-background-color: white;");

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background-color: white;");
        return scrollPane;
    }

    private VBox createEnhancedPieLegend(ObservableList<PieChart.Data> data, String[] colors) {
        VBox legendContainer = new VBox(15);
        legendContainer.setAlignment(Pos.TOP_LEFT);
        legendContainer.setPadding(new Insets(20));
        legendContainer.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                "-fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label legendTitle = new Label("Legend");
        legendTitle.setFont(Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 16));
        legendTitle.setStyle("-fx-text-fill: #2c3e50;");
        legendContainer.getChildren().add(legendTitle);

        GridPane legendGrid = new GridPane();
        legendGrid.setHgap(15);
        legendGrid.setVgap(8);
        legendGrid.setAlignment(Pos.TOP_LEFT);

        int maxItemsPerColumn = Math.min(10, data.size()); // Limit items per column
        int columns = (int) Math.ceil((double) data.size() / maxItemsPerColumn);

        for (int i = 0; i < Math.min(data.size(), 50); i++) { // Limit to 50 items max
            PieChart.Data item = data.get(i);

            int row = i % maxItemsPerColumn;
            int col = i / maxItemsPerColumn;

            HBox legendItem = new HBox(8);
            legendItem.setAlignment(Pos.CENTER_LEFT);

            Rectangle colorRect = new Rectangle(18, 18);
            colorRect.setFill(Color.web(colors[i % colors.length]));
            colorRect.setStroke(Color.web(colors[i % colors.length]).darker());
            colorRect.setStrokeWidth(1);

            String labelText = item.getName();
            if (labelText.length() > 25) {
                labelText = labelText.substring(0, 22) + "..."; // Truncate long labels
            }

            Label itemLabel = new Label(labelText);
            itemLabel.setFont(Font.font("Arial", 11));
            itemLabel.setWrapText(true);
            itemLabel.setMaxWidth(180);

            legendItem.getChildren().addAll(colorRect, itemLabel);
            legendGrid.add(legendItem, col, row);
        }

        if (data.size() > 50) {
            Label moreLabel = new Label("... and " + (data.size() - 50) + " more items");
            moreLabel.setFont(Font.font("Arial", 10));
            moreLabel.setStyle("-fx-text-fill: #7f8c8d;");
            legendContainer.getChildren().add(moreLabel);
        }

        legendContainer.getChildren().add(legendGrid);
        legendContainer.setMaxWidth(400);
        return legendContainer;
    }
    private HBox createTiltedStackedBarLegend(Set<String> yValues, String[] colors) {
        ScrollPane legendScrollPane = new ScrollPane();
        legendScrollPane.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                "-fx-border-width: 2; -fx-border-radius: 10;");
        legendScrollPane.setFitToHeight(true);
        legendScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        legendScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        legendScrollPane.setPrefHeight(120);
        legendScrollPane.setMaxHeight(120);

        HBox legendBox = new HBox(10);
        legendBox.setAlignment(Pos.CENTER_LEFT);
        legendBox.setPadding(new Insets(15));

        int colorIndex = 0;
        for (String yValue : yValues) {
            VBox legendItem = new VBox(5);
            legendItem.setAlignment(Pos.CENTER);
            legendItem.setMinWidth(60);

            Rectangle colorRect = new Rectangle(20, 20);
            colorRect.setFill(Color.web(colors[colorIndex % colors.length]));
            colorRect.setStroke(Color.web(colors[colorIndex % colors.length]).darker());
            colorRect.setStrokeWidth(1);

            Label itemLabel = new Label(yValue.length() > 15 ? yValue.substring(0, 12) + "..." : yValue);
            itemLabel.setFont(Font.font("Arial", 10));
            itemLabel.setRotate(-30);
            itemLabel.setStyle("-fx-text-fill: #2c3e50;");
            itemLabel.setWrapText(true);
            itemLabel.setMaxWidth(80);

            legendItem.getChildren().addAll(colorRect, itemLabel);
            legendBox.getChildren().add(legendItem);

            colorIndex++;
        }

        legendScrollPane.setContent(legendBox);

        HBox container = new HBox();
        container.getChildren().add(legendScrollPane);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(10));

        return container;
    }

    private ScrollPane createLineChartPane(boolean totalOnly) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(currentXColumn);
        yAxis.setLabel("Count");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Evolution of " + currentYColumn + " over " + currentXColumn);
        lineChart.setCreateSymbols(true);
        lineChart.setLegendVisible(true);

        if (totalOnly) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Total Count");

            Map<String, Integer> totals = crosstabData.getRowTotals();
            List<String> sortedKeys = new ArrayList<>(totals.keySet());

            if (useMonthlyConversion) {
                sortedKeys = crosstabData.sortMonthYearValues(sortedKeys);
            } else {
                Collections.sort(sortedKeys);
            }

            for (String key : sortedKeys) {
                series.getData().add(new XYChart.Data<>(key, totals.get(key)));
            }
            lineChart.getData().add(series);
        } else {
            Map<String, Map<String, Integer>> data = crosstabData.getCrosstabData();

            List<String> sortedXValues = new ArrayList<>(data.keySet());
            if (useMonthlyConversion) {
                sortedXValues = crosstabData.sortMonthYearValues(sortedXValues);
            } else {
                Collections.sort(sortedXValues);
            }

            Set<String> allYValues = new TreeSet<>();
            data.values().forEach(row -> allYValues.addAll(row.keySet()));

            for (String yValue : allYValues) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(yValue);

                for (String xValue : sortedXValues) {
                    Integer count = data.get(xValue).getOrDefault(yValue, 0);
                    series.getData().add(new XYChart.Data<>(xValue, count));
                }
                lineChart.getData().add(series);
            }
        }
        lineChart.setStyle("-fx-background-color: white;");

        ScrollPane scrollPane = new ScrollPane(lineChart);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }

    private ScrollPane createStackedBarChartPane(boolean totalOnly) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel(currentXColumn);
        yAxis.setLabel("Count");

        StackedBarChart<String, Number> stackedBarChart = new StackedBarChart<>(xAxis, yAxis);
        stackedBarChart.setTitle("Stacked Distribution: " + currentXColumn + " by " + currentYColumn);
        stackedBarChart.setLegendVisible(true);

        stackedBarChart.setAnimated(false);

        String[] colorPalette = {
                "#e74c3c", "#3498db", "#f39c12", "#2ecc71", "#9b59b6", "#1abc9c", "#e67e22", "#34495e",
                "#f1c40f", "#95a5a6", "#c0392b", "#2980b9", "#d35400", "#27ae60", "#8e44ad", "#16a085",
                "#d68910", "#5d6d7e", "#f7dc6f", "#bb8fce", "#85c1e9", "#f8c471", "#82e0aa", "#a569bd",
                "#5dade2", "#f4d03f", "#d7dbdd", "#ec7063", "#5499c7", "#f6b26b", "#58d68d", "#bb8fce",
                "#7fb3d3", "#f9e79f", "#d2b4de", "#85c1e9", "#f8c471", "#82e0aa", "#a569bd", "#5dade2",
                "#ff6b6b", "#4ecdc4", "#45b7d1", "#f9ca24", "#f0932b", "#eb4d4b", "#6c5ce7", "#a29bfe",
                "#fd79a8", "#e84393", "#00b894", "#00cec9", "#0984e3", "#6c5ce7", "#a29bfe", "#fd79a8",
                "#fdcb6e", "#e17055", "#74b9ff", "#0984e3", "#00b894", "#00cec9", "#a29bfe", "#fd79a8",
                "#fab1a0", "#ff7675", "#00cec9", "#0984e3", "#6c5ce7", "#a29bfe", "#fd79a8", "#e84393",
                "#ff6348", "#7bed9f", "#70a1ff", "#5352ed", "#ff4757", "#2ed573", "#1e90ff", "#3742fa",
                "#ff3838", "#2ed573", "#ff6348", "#7bed9f", "#70a1ff", "#5352ed", "#ff4757", "#2ed573",
                "#ffa502", "#ff6b81", "#7bed9f", "#70a1ff", "#5352ed", "#ff4757", "#2ed573", "#1e90ff",
                "#3742fa", "#ff3838", "#2ed573", "#ff6348", "#7bed9f", "#70a1ff", "#5352ed", "#ff4757"
        };

        Set<String> allYValues = new TreeSet<>();

        if (totalOnly) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Total Count");

            Map<String, Integer> totals = crosstabData.getRowTotals();
            List<String> sortedKeys = new ArrayList<>(totals.keySet());
            if (useMonthlyConversion) {
                sortedKeys = crosstabData.sortMonthYearValues(sortedKeys);
            } else {
                Collections.sort(sortedKeys);
            }

            for (String key : sortedKeys) {
                series.getData().add(new XYChart.Data<>(key, totals.get(key)));
            }
            stackedBarChart.getData().add(series);

            allYValues.add("Total Count");
        } else {
            Map<String, Map<String, Integer>> data = crosstabData.getCrosstabData();

            List<String> sortedXValues = new ArrayList<>(data.keySet());
            if (useMonthlyConversion) {
                sortedXValues = crosstabData.sortMonthYearValues(sortedXValues);
            } else {
                Collections.sort(sortedXValues);
            }

            for (Map<String, Integer> row : data.values()) {
                allYValues.addAll(row.keySet());
            }

            ObservableList<String> categories = FXCollections.observableArrayList(sortedXValues);
            xAxis.setCategories(categories);

            int colorIndex = 0;
            for (String yValue : allYValues) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(yValue); // Legend shows Y values

                for (String xValue : sortedXValues) {
                    Map<String, Integer> rowData = data.get(xValue);
                    Integer count = rowData != null ? rowData.getOrDefault(yValue, 0) : 0;

                    // Each data point contributes to the stack for this X category
                    XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(xValue, count);
                    series.getData().add(dataPoint);
                }

                stackedBarChart.getData().add(series);

                // Apply colors to each series with proper stacking colors
                final String color = colorPalette[colorIndex % colorPalette.length];
                final int seriesIndex = colorIndex;

                // Style the series with custom colors for proper stacking
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Apply unique color styling for each series segment
                        String cssSelector = ".default-color" + seriesIndex + ".chart-bar";
                        stackedBarChart.lookupAll(cssSelector).forEach(node -> {
                            node.setStyle(
                                    "-fx-background-color: " + color + ";" +
                                            "-fx-border-color: white;" +
                                            "-fx-border-width: 1px;" +
                                            "-fx-background-radius: 0;"
                            );
                        });

                        // Also style individual data points when they become available
                        for (XYChart.Data<String, Number> dataPoint : series.getData()) {
                            if (dataPoint.getNode() != null) {
                                dataPoint.getNode().setStyle(
                                        "-fx-background-color: " + color + ";" +
                                                "-fx-border-color: white;" +
                                                "-fx-border-width: 1px;" +
                                                "-fx-background-radius: 0;"
                                );
                            }
                        }

                    } catch (Exception e) {
                        System.err.println("Styling error (non-critical): " + e.getMessage());
                    }
                });

                colorIndex++;
            }
            // Chart styling
            stackedBarChart.setCategoryGap(20.0); // Space between bars
            stackedBarChart.setLegendVisible(true);
        }

        // Overall chart styling
        stackedBarChart.setStyle("-fx-background-color: white;");

        // Ensure proper rendering
        javafx.application.Platform.runLater(() -> {
            try {
                stackedBarChart.applyCss();
                stackedBarChart.autosize();
                stackedBarChart.requestLayout();

                if (stackedBarChart.lookup(".chart-content") != null) {
                    stackedBarChart.lookup(".chart-content").setStyle("-fx-padding: 10px;");
                }
            } catch (Exception e) {
                System.err.println("Layout error (non-critical): " + e.getMessage());
            }
        });

        stackedBarChart.setLegendVisible(false); // Hide default legend
        VBox chartWithCustomLegend = new VBox(10);
        chartWithCustomLegend.setAlignment(Pos.CENTER);
        chartWithCustomLegend.getChildren().addAll(stackedBarChart, createTiltedStackedBarLegend(allYValues, colorPalette));
        chartWithCustomLegend.setStyle("-fx-background-color: white;");

        ScrollPane scrollPane = new ScrollPane(chartWithCustomLegend);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }   private ScrollPane createStackedComboChartPane(boolean totalOnly, boolean useCumulativeEffective) {
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setStyle("-fx-background-color: white;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox container = new VBox(20);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white;");

        if (totalOnly) {
            // Create combo chart using totals
            Map<String, Integer> rowTotals = crosstabData.getRowTotals();
            Map<String, Integer> colTotals = crosstabData.getColumnTotals();
            Map<String, Integer> cumulativeEffective = crosstabData.getCumulativeEffective();

            // Get sorted x-axis values
            List<String> xValues;
            if (useMonthlyConversion && crosstabData.isDateColumn(currentXColumn)) {
                Set<String> completeMonthRange = crosstabData.createCompleteMonthRange(currentXColumn, currentYColumn, true);
                xValues = new ArrayList<>(completeMonthRange);
            } else {
                xValues = new ArrayList<>(rowTotals.keySet());
                Collections.sort(xValues);
            }

            // Create combo chart with cumulative effective option
            ComboChart comboChart = new ComboChart(
                    rowTotals,
                    rowTotals,
                    xValues,
                    currentXColumn,
                    currentYColumn,
                    cumulativeEffective,
                    useCumulativeEffective
            );

            VBox chartContainer = comboChart.getChartContainer();
            container.getChildren().add(chartContainer);

            // Add data summary
            Label summaryLabel = new Label(String.format(
                    "📊 Summary: %d %s categories, %d %s categories, Grand Total: %d",
                    rowTotals.size(), currentXColumn,
                    colTotals.size(), currentYColumn,
                    crosstabData.getGrandTotal()
            ));
            summaryLabel.setFont(Font.font("Arial", 12));
            summaryLabel.setStyle("-fx-text-fill: #666666; -fx-padding: 10;");
            container.getChildren().add(summaryLabel);

            if (useCumulativeEffective) {
                Label cumulativeLabel = new Label("📈 Line shows Cumulative Effective values");
                cumulativeLabel.setFont(Font.font("Arial", 12));
                cumulativeLabel.setStyle("-fx-text-fill: #e74c3c; -fx-padding: 5;");
                container.getChildren().add(cumulativeLabel);
            }

        } else {
            Label errorLabel = new Label("❌ Combo Chart is only available in Total Only mode");
            errorLabel.setFont(Font.font("Arial", 14));
            errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-padding: 20;");
            container.getChildren().add(errorLabel);
        }

        scrollPane.setContent(container);
        return scrollPane;
    }
    private static class ComboChart {
        private final Map<String, Integer> rowTotals;
        private final Map<String, Integer> colTotals;
        private final Map<String, Integer> cumulativeEffective;
        private final List<String> xValues;
        private final String xColumnName;
        private final String yColumnName;
        private final boolean useCumulativeForLine;

        private final double CHART_WIDTH = 1200;
        private final double CHART_HEIGHT = 700;
        private final double MARGIN_LEFT = 80;
        private final double MARGIN_RIGHT = 80; // Increased for right axis
        private final double MARGIN_TOP = 50;
        private final double MARGIN_BOTTOM = 200;
        private final double PLOT_WIDTH = CHART_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
        private final double PLOT_HEIGHT = CHART_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM;

        private final String[] COLORS = {"#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6", "#1abc9c", "#e67e22", "#34495e"};

        // Updated constructor to include cumulative data
        public ComboChart(Map<String, Integer> rowTotals, Map<String, Integer> colTotals,
                          List<String> xValues, String xColumnName, String yColumnName,
                          Map<String, Integer> cumulativeEffective, boolean useCumulativeForLine) {
            this.rowTotals = rowTotals;
            this.colTotals = colTotals;
            this.cumulativeEffective = cumulativeEffective != null ? cumulativeEffective : new HashMap<>();
            this.xValues = xValues;
            this.xColumnName = xColumnName;
            this.yColumnName = yColumnName;
            this.useCumulativeForLine = useCumulativeForLine;
        }

        // Backward compatibility constructor
        public ComboChart(Map<String, Integer> rowTotals, Map<String, Integer> colTotals,
                          List<String> xValues, String xColumnName, String yColumnName) {
            this(rowTotals, colTotals, xValues, xColumnName, yColumnName, null, false);
        }

        public VBox getChartContainer() {
            Canvas canvas = new Canvas(CHART_WIDTH, CHART_HEIGHT);
            GraphicsContext gc = canvas.getGraphicsContext2D();

            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, CHART_WIDTH, CHART_HEIGHT);

            // Calculate separate max values for dual scale
            int maxBarValue = rowTotals.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            int maxLineValue;

            if (useCumulativeForLine && !cumulativeEffective.isEmpty()) {
                maxLineValue = cumulativeEffective.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            } else {
                maxLineValue = colTotals.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            }

            drawTitle(gc);
            drawAxes(gc);
            drawAxisLabels(gc);
            drawGridLines(gc, maxBarValue, maxLineValue);
            drawBars(gc, maxBarValue);
            drawLines(gc, maxLineValue);

            HBox legend = createLegend();

            VBox container = new VBox(10);
            container.getChildren().addAll(canvas, legend);
            container.setAlignment(Pos.CENTER);
            container.setPadding(new Insets(20));
            container.setStyle("-fx-background-color: white;");

            return container;
        }

        private int calculateMaxValue() {
            int maxRow = rowTotals.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            int maxCol;

            // Use cumulative effective values for line if enabled, otherwise use column totals
            if (useCumulativeForLine && !cumulativeEffective.isEmpty()) {
                maxCol = cumulativeEffective.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            } else {
                maxCol = colTotals.values().stream().mapToInt(Integer::intValue).max().orElse(0);
            }

            int max = Math.max(maxRow, maxCol);
            return max == 0 ? 1 : max;
        }

        private void drawTitle(GraphicsContext gc) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 16));
            gc.setTextAlign(TextAlignment.CENTER);

            String lineDataSource = useCumulativeForLine ? "Cumulative Effective" : yColumnName + " Totals";
            String titleText = xColumnName + " Totals (Bars) vs " + lineDataSource + " (Line)";
            if (useCumulativeForLine) {
                titleText += " - Dual Scale";
            }
            gc.fillText(titleText, CHART_WIDTH / 2, 25);
        }
        private void drawAxes(GraphicsContext gc) {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);

            gc.strokeLine(MARGIN_LEFT, MARGIN_TOP, MARGIN_LEFT, CHART_HEIGHT - MARGIN_BOTTOM);
            gc.strokeLine(MARGIN_LEFT, CHART_HEIGHT - MARGIN_BOTTOM,
                    CHART_WIDTH - MARGIN_RIGHT, CHART_HEIGHT - MARGIN_BOTTOM);
        }

        private void drawAxisLabels(GraphicsContext gc) {
            gc.setFill(Color.BLACK);
            gc.setFont(Font.font("Arial", 12));

            gc.setTextAlign(TextAlignment.LEFT);
            double barWidth = PLOT_WIDTH / xValues.size();
            for (int i = 0; i < xValues.size(); i++) {
                double x = MARGIN_LEFT + (i + 0.5) * barWidth;
                String label = xValues.get(i);

                // Save the current transform
                gc.save();

                // Move to the label position
                gc.translate(x, CHART_HEIGHT - MARGIN_BOTTOM + 10);
                gc.rotate(-45); // 45 degrees inclined

                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextAlign(TextAlignment.RIGHT);

                // Draw the rotated text
                gc.fillText(label, 0, 0);

                // Restore the transform
                gc.restore();
            }

            // Y-axis title
            gc.save();
            gc.translate(20, CHART_HEIGHT / 2);
            gc.rotate(-Math.PI / 2);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("Count", 0, 0);
            gc.restore();

            // X-axis title
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(xColumnName, CHART_WIDTH / 2, CHART_HEIGHT - 10);
        }

        private void drawGridLines(GraphicsContext gc, int maxBarValue, int maxLineValue) {
            gc.setStroke(Color.LIGHTGRAY);
            gc.setLineWidth(0.5);

            // Horizontal grid lines
            int gridLines = 5;
            for (int i = 0; i <= gridLines; i++) {
                double y = MARGIN_TOP + (i * PLOT_HEIGHT / gridLines);
                gc.strokeLine(MARGIN_LEFT, y, CHART_WIDTH - MARGIN_RIGHT, y);

                // Left Y-axis values (for bars)
                gc.setFill(Color.web(COLORS[0])); // Bar color
                gc.setTextAlign(TextAlignment.RIGHT);
                int barValue = maxBarValue - (i * maxBarValue / gridLines);
                gc.fillText(String.valueOf(barValue), MARGIN_LEFT - 10, y + 5);

                // Right Y-axis values (for line) - only if using cumulative and different scale
                if (useCumulativeForLine && maxLineValue != maxBarValue) {
                    gc.setFill(Color.web(COLORS[1])); // Line color
                    gc.setTextAlign(TextAlignment.LEFT);
                    int lineValue = maxLineValue - (i * maxLineValue / gridLines);
                    gc.fillText(String.valueOf(lineValue), CHART_WIDTH - MARGIN_RIGHT + 10, y + 5);
                }
            }
        }

        private void drawBars(GraphicsContext gc, int maxValue) {
            double barWidth = PLOT_WIDTH / xValues.size() * 0.6; // Individual bar width
            double spacing = PLOT_WIDTH / xValues.size(); // Space between bars

            // Draw individual bars for each X value
            for (int i = 0; i < xValues.size(); i++) {
                String xValue = xValues.get(i);
                int count = rowTotals.getOrDefault(xValue, 0);

                if (count > 0) {
                    // Calculate bar position and height
                    double barX = MARGIN_LEFT + i * spacing + (spacing - barWidth) / 2; // Center bar in its space
                    double barHeight = (double) count / maxValue * PLOT_HEIGHT;
                    double barY = CHART_HEIGHT - MARGIN_BOTTOM - barHeight;

                    // Set color for this bar (use first color consistently)
                    gc.setFill(Color.web(COLORS[0]));

                    // Draw the bar
                    gc.fillRect(barX, barY, barWidth, barHeight);

                    // Add border
                    gc.setStroke(Color.WHITE);
                    gc.setLineWidth(1);
                    gc.strokeRect(barX, barY, barWidth, barHeight);

                    // Add value label on top of bar
                    gc.setFill(Color.BLACK);
                    gc.setFont(Font.font("Arial", 10));
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.fillText(String.valueOf(count),
                            barX + barWidth/2,
                            barY - 5);
                }
            }
        }
        private void drawLines(GraphicsContext gc, int maxValue) {
            // Draw line for column totals or cumulative effective
            Color lineColor = Color.web(COLORS[1]); // Use second color for line

            gc.setStroke(lineColor);
            gc.setLineWidth(3);

            // Calculate points based on the selected data source
            List<Double> xPoints = new ArrayList<>();
            List<Double> yPoints = new ArrayList<>();

            Map<String, Integer> lineDataSource;
            if (useCumulativeForLine && !cumulativeEffective.isEmpty()) {
                lineDataSource = cumulativeEffective;
            } else {
                lineDataSource = colTotals;
            }
            for (int i = 0; i < xValues.size(); i++) {
                String xValue = xValues.get(i);
                int count = lineDataSource.getOrDefault(xValue, 0);

                double x = MARGIN_LEFT + (i + 0.5) * PLOT_WIDTH / xValues.size();
                double y = CHART_HEIGHT - MARGIN_BOTTOM - ((double) count / maxValue * PLOT_HEIGHT);

                xPoints.add(x);
                yPoints.add(y);
            }

            // Draw line segments
            for (int i = 0; i < xPoints.size() - 1; i++) {
                gc.strokeLine(xPoints.get(i), yPoints.get(i),
                        xPoints.get(i + 1), yPoints.get(i + 1));
            }

            // Draw points
            gc.setFill(lineColor);
            for (int i = 0; i < xPoints.size(); i++) {
                gc.fillOval(xPoints.get(i) - 4, yPoints.get(i) - 4, 8, 8);

                // Add white border to points
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2);
                gc.strokeOval(xPoints.get(i) - 4, yPoints.get(i) - 4, 8, 8);
                gc.setStroke(lineColor);
                gc.setLineWidth(3);
            }
        }

        private HBox createLegend() {
            HBox legend = new HBox(20);
            legend.setAlignment(Pos.CENTER);
            legend.setPadding(new Insets(10));
            legend.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 5;");

            // Bar legend
            HBox barItem = new HBox(8);
            barItem.setAlignment(Pos.CENTER_LEFT);

            Rectangle barSymbol = new Rectangle(20, 15);
            barSymbol.setFill(Color.web(COLORS[0]));
            barSymbol.setStroke(Color.web(COLORS[0]).darker());

            Label barLabel = new Label("Row Totals (Bars) - Left Scale");
            barLabel.setFont(Font.font("Arial", 12));

            barItem.getChildren().addAll(barSymbol, barLabel);
            legend.getChildren().add(barItem);

            // Line legend
            HBox lineItem = new HBox(8);
            lineItem.setAlignment(Pos.CENTER_LEFT);

            StackPane lineSymbol = new StackPane();
            lineSymbol.setPrefSize(20, 15);

            Line line = new Line(0, 7.5, 20, 7.5);
            line.setStroke(Color.web(COLORS[1]));
            line.setStrokeWidth(3);

            Circle point = new Circle(10, 7.5, 4);
            point.setFill(Color.web(COLORS[1]));
            point.setStroke(Color.WHITE);
            point.setStrokeWidth(1);

            lineSymbol.getChildren().addAll(line, point);

            String lineDescription = useCumulativeForLine ?
                    "Cumulative Effective (Line) - Right Scale" :
                    "Column Totals (Line) - Left Scale";
            Label lineLabel = new Label(lineDescription);
            lineLabel.setFont(Font.font("Arial", 12));

            lineItem.getChildren().addAll(lineSymbol, lineLabel);
            legend.getChildren().add(lineItem);

            return legend;
        }
    }

    private void makeLineChartTransparent(LineChart<String, Number> lineChart) {
        // Make the line chart mouse transparent so clicks go through to bar chart
        lineChart.setMouseTransparent(true);

        // Additional transparency settings
        javafx.application.Platform.runLater(() -> {
            try {
                // Make chart content transparent
                if (lineChart.lookup(".chart-content") != null) {
                    lineChart.lookup(".chart-content").setStyle("-fx-background-color: transparent;");
                }

                // Make chart plot background transparent
                if (lineChart.lookup(".chart-plot-background") != null) {
                    lineChart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
                }

                // Hide axes
                if (lineChart.lookup(".axis") != null) {
                    lineChart.lookup(".axis").setVisible(false);
                }

            } catch (Exception e) {
                // Ignore styling errors
            }
        });
    }

    // Helper method to connect line points
    private HBox createCustomLegend(List<String> yValues, String[] colors) {
        HBox legendBox = new HBox(15);
        legendBox.setAlignment(Pos.CENTER);
        legendBox.setPadding(new Insets(10));
        legendBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1;");

        // Bar series legend (first item)
        if (!yValues.isEmpty()) {
            HBox barLegend = new HBox(5);
            barLegend.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.shape.Rectangle barRect = new javafx.scene.shape.Rectangle(15, 15);
            barRect.setFill(javafx.scene.paint.Color.web("#3498db")); // Default bar color

            Label barLabel = new Label(yValues.get(0) + " (Bars)");
            barLabel.setFont(Font.font("Arial", 12));

            barLegend.getChildren().addAll(barRect, barLabel);
            legendBox.getChildren().add(barLegend);
        }

        // Line series legends
        for (int i = 1; i < Math.min(yValues.size(), 10); i++) {
            HBox lineLegend = new HBox(5);
            lineLegend.setAlignment(Pos.CENTER_LEFT);

            javafx.scene.shape.Line line = new javafx.scene.shape.Line(0, 7.5, 15, 7.5);
            line.setStroke(javafx.scene.paint.Color.web(colors[(i - 1) % colors.length]));
            line.setStrokeWidth(3);

            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(7.5, 7.5, 3);
            circle.setFill(javafx.scene.paint.Color.web(colors[(i - 1) % colors.length]));

            StackPane lineSymbol = new StackPane();
            lineSymbol.getChildren().addAll(line, circle);
            lineSymbol.setPrefSize(15, 15);

            Label lineLabel = new Label(yValues.get(i) + " (Line)");
            lineLabel.setFont(Font.font("Arial", 12));

            lineLegend.getChildren().addAll(lineSymbol, lineLabel);
            legendBox.getChildren().add(lineLegend);
        }

        return legendBox;
    }
    public void showGraphTypeDialog(String xColumn, String yColumn, boolean monthlyConversion, boolean totalOnly, boolean includeAllMonths) {        this.currentXColumn = xColumn;
        this.currentYColumn = yColumn;
        this.useMonthlyConversion = monthlyConversion;

        if (crosstabData.isEmpty()) {
            throw new IllegalStateException("Please generate a crosstab first!");
        }

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Select Chart Types");
        dialog.setWidth(500);
        dialog.setHeight(500);

        VBox layout = new VBox(15);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("Choose Chart Types (Select Multiple):");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Verdana", 16));

        // Create checkboxes for chart types
        CheckBox barChartCB = new CheckBox("📊 Bar Chart");
        CheckBox pieChartCB = new CheckBox("🥧 Pie Chart");
        CheckBox lineChartCB = new CheckBox("📈 Line Chart (Evolution)");
        CheckBox stackedBarCB = new CheckBox("📚 Stacked Bar Chart");
        CheckBox comboChartCB = new CheckBox("📚📈 Combo Chart");
        CheckBox cumulativeChartCB = new CheckBox("📈📊 Cumulative Effective Chart");

        // Style checkboxes
        styleGraphCheckBox(barChartCB);
        styleGraphCheckBox(pieChartCB);
        styleGraphCheckBox(lineChartCB);
        styleGraphCheckBox(stackedBarCB);
        styleGraphCheckBox(comboChartCB);
        styleGraphCheckBox(cumulativeChartCB);

        // IMPORTANT: Only enable combo chart and cumulative chart in total-only mode
        if (!totalOnly) {
            comboChartCB.setDisable(true);
            comboChartCB.setText("📚📈 Combo Chart (Only available in Total Mode)");
            comboChartCB.setStyle(comboChartCB.getStyle() + "-fx-opacity: 0.5;");

            // FIX: Also disable cumulative chart when not in total mode
            cumulativeChartCB.setDisable(true);
            cumulativeChartCB.setText("📈📊 Cumulative Effective Chart (Only available in Total Mode)");
            cumulativeChartCB.setStyle(cumulativeChartCB.getStyle() + "-fx-opacity: 0.5;");
        }
        if (totalOnly) {
            stackedBarCB.setDisable(true);
            stackedBarCB.setText("📚 Stacked Bar Chart (not available in Total Mode)");
            stackedBarCB.setStyle(comboChartCB.getStyle() + "-fx-opacity: 0.5;");

        }

        // Total only info label
        Label totalInfoLabel = new Label();
        if (totalOnly) {
            String infoText = "📋 Total Only Mode: Charts will show aggregated totals only\n✅ Combo Chart Available!";
            if (!crosstabData.getCumulativeEffective().isEmpty()) {
                infoText += "\n📈 Cumulative Effective data detected!";
            }
            totalInfoLabel.setText(infoText);
            totalInfoLabel.setTextFill(Color.rgb(144, 238, 144)); // Light green
            totalInfoLabel.setFont(Font.font("Verdana", 11));
            totalInfoLabel.setWrapText(true);
        } else {
            totalInfoLabel.setText("📋 Regular Mode: Use 'Total Only' to enable Combo Charts");
            totalInfoLabel.setTextFill(Color.rgb(255, 193, 7)); // Yellow
            totalInfoLabel.setFont(Font.font("Verdana", 11));
            totalInfoLabel.setWrapText(true);
        }

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button generateButton = new Button("📊 Generate Selected Charts");
        Button cancelButton = new Button("❌ Cancel");

        styleDialogButton(generateButton, "#27ae60");
        styleDialogButton(cancelButton, "#e74c3c");

        generateButton.setOnAction(e -> {
            List<String> selectedCharts = new ArrayList<>();
            boolean useCumulativeEffective = false;

            if (barChartCB.isSelected()) selectedCharts.add("BAR");
            if (pieChartCB.isSelected()) selectedCharts.add("PIE");
            if (lineChartCB.isSelected()) selectedCharts.add("LINE");
            if (stackedBarCB.isSelected()) selectedCharts.add("STACKED");
            if (comboChartCB.isSelected() && totalOnly) {
                selectedCharts.add("COMBO");
            }
            if (cumulativeChartCB.isSelected() && totalOnly) {
                selectedCharts.add("CUMULATIVE");
            }

            if (selectedCharts.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No Selection");
                alert.setHeaderText("Please select at least one chart type");
                alert.showAndWait();
                return;
            }

            dialog.close();
            generateSelectedCharts(selectedCharts, totalOnly, useCumulativeEffective);
        });

        cancelButton.setOnAction(e -> dialog.close());

        buttonBox.getChildren().addAll(generateButton, cancelButton);

        VBox checkboxContainer = new VBox(10);
        checkboxContainer.setAlignment(Pos.CENTER_LEFT);
        checkboxContainer.getChildren().addAll(barChartCB, pieChartCB, lineChartCB, stackedBarCB, comboChartCB, cumulativeChartCB);

        layout.getChildren().addAll(titleLabel, checkboxContainer, totalInfoLabel, buttonBox);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // Add these helper methods if they don't already exist in your CrosstabCharts class:
    private void styleGraphCheckBox(CheckBox checkBox) {
        checkBox.setTextFill(Color.WHITE);
        checkBox.setFont(Font.font("Verdana", 14));
        checkBox.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-mark-color: #3498db; " +
                        "-fx-mark-highlight-color: #3498db;"
        );
    }

    private void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle(
                "-fx-background-color: #2c3e50; " +
                        "-fx-text-fill: white; " +
                        "-fx-prompt-text-fill: white; " +
                        "-fx-background-radius: 5;"
        );

        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(item);
                setTextFill(Color.WHITE);
            }
        });
    }

    private void styleDialogButton(Button button, String color) {
        button.setPrefWidth(150);
        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 5; " +
                        "-fx-padding: 8 16 8 16;"
        );
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.8;", "")));
    }

    private void generateSelectedCharts(List<String> chartTypes, boolean totalOnly) {
        generateSelectedCharts(chartTypes, totalOnly, false);
    }

    private void generateSelectedCharts(List<String> chartTypes, boolean totalOnly, boolean useCumulativeEffective) {
        try {
            if (chartTypes.size() == 1) {
                String chartType = chartTypes.get(0);
                switch (chartType) {
                    case "BAR":
                        showBarChart(totalOnly);
                        break;
                    case "PIE":
                        showPieChart(totalOnly);
                        break;
                    case "LINE":
                        showLineChart(totalOnly);
                        break;
                    case "STACKED":
                        showStackedBarChart(totalOnly);
                        break;
                    case "COMBO":
                        showComboChart(totalOnly, useCumulativeEffective);
                        break;
                    case "CUMULATIVE":
                        showComboChart(totalOnly, true);
                        break;
                }
            } else {
                showMultipleCharts(chartTypes, totalOnly, useCumulativeEffective);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Chart Generation Error");
            alert.setHeaderText("Error generating charts");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void showMultipleCharts(List<String> chartTypes, boolean totalOnly) {
        showMultipleCharts(chartTypes, totalOnly, false);
    }

    private void showMultipleCharts(List<String> chartTypes, boolean totalOnly, boolean useCumulativeEffective) {
        Stage multiChartStage = new Stage();
        multiChartStage.setTitle("Multiple Charts - " + currentXColumn + " vs " + currentYColumn);
        multiChartStage.setWidth(1200);
        multiChartStage.setHeight(700);

        TabPane tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: white;");

        for (String chartType : chartTypes) {
            Tab tab = new Tab();

            switch (chartType) {
                case "BAR":
                    tab.setText("📊 Bar Chart");
                    tab.setContent(createBarChartPane(totalOnly));
                    break;
                case "PIE":
                    tab.setText("🥧 Pie Chart");
                    tab.setContent(createPieChartPane(totalOnly));
                    break;
                case "LINE":
                    tab.setText("📈 Line Chart");
                    tab.setContent(createLineChartPane(totalOnly));
                    break;
                case "STACKED":
                    tab.setText("📚 Stacked Bar");
                    tab.setContent(createStackedBarChartPane(totalOnly));
                    break;
                case "COMBO":
                    String tabText = "📚📈 Combo Chart";
                    if (useCumulativeEffective) {
                        tabText += " (Cumulative)";
                    }
                    tab.setText(tabText);
                    tab.setContent(createStackedComboChartPane(totalOnly, useCumulativeEffective));
                    break;
                case "CUMULATIVE":
                    tab.setText("📈📊 Cumulative Effective");
                    tab.setContent(createStackedComboChartPane(totalOnly, true));
                    break;
            }

            tabPane.getTabs().add(tab);
        }

        Scene scene = new Scene(tabPane);
        multiChartStage.setScene(scene);
        multiChartStage.show();
    }
   public void showComboChart(boolean totalOnly) {
        showComboChart(totalOnly, false);
    }

    public void showComboChart(boolean totalOnly, boolean useCumulativeEffective) {
        Stage comboStage = new Stage();
        String title = "Combo Chart - " + currentXColumn + " vs " + currentYColumn;
        if (useCumulativeEffective) {
            title += " (Cumulative Effective)";
        }
        comboStage.setTitle(title);
        comboStage.setWidth(1000);
        comboStage.setHeight(600);

        ScrollPane scrollPane = createStackedComboChartPane(totalOnly, useCumulativeEffective);
        Scene scene = new Scene(scrollPane);
        comboStage.setScene(scene);
        comboStage.show();
    }
    public void showBarChart(boolean totalOnly) {
        Stage chartStage = new Stage();
        chartStage.setTitle("Bar Chart - " + currentXColumn + " vs " + currentYColumn);
        chartStage.setWidth(900);
        chartStage.setHeight(600);

        ScrollPane scrollPane = createBarChartPane(totalOnly);
        Scene scene = new Scene(scrollPane);
        chartStage.setScene(scene);
        chartStage.show();
    }

    public void showPieChart(boolean totalOnly) {
        Stage chartStage = new Stage();
        chartStage.setTitle("Pie Chart - " + currentYColumn + " Distribution");
        chartStage.setWidth(700);
        chartStage.setHeight(600);

        ScrollPane scrollPane = createPieChartPane(totalOnly);
        Scene scene = new Scene(scrollPane);
        chartStage.setScene(scene);
        chartStage.show();
    }

    public void showLineChart(boolean totalOnly) {
        Stage chartStage = new Stage();
        chartStage.setTitle("Combo Chart - " + currentXColumn + " vs " + currentYColumn + " Evolution");
        chartStage.setWidth(1000);
        chartStage.setHeight(600);

        ScrollPane scrollPane = createLineChartPane(totalOnly);
        Scene scene = new Scene(scrollPane);
        chartStage.setScene(scene);
        chartStage.show();
    }

    public void showStackedBarChart(boolean totalOnly) {
        Stage chartStage = new Stage();
        chartStage.setTitle("Stacked Bar Chart - " + currentXColumn + " vs " + currentYColumn);
        chartStage.setWidth(1000);
        chartStage.setHeight(600);


        ScrollPane scrollPane = createStackedBarChartPane(totalOnly);
        Scene scene = new Scene(scrollPane);
        chartStage.setScene(scene);
        chartStage.show();
    }


    private void styleGraphButton(Button button) {
        button.setPrefWidth(200);
        button.setStyle(
                "-fx-background-color: #3498db; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 5; " +
                        "-fx-padding: 10 20 10 20;"
        );
        button.setOnMouseEntered(e -> button.setStyle(button.getStyle() + "-fx-opacity: 0.8;"));
        button.setOnMouseExited(e -> button.setStyle(button.getStyle().replace("-fx-opacity: 0.8;", "")));
    }
    public void verifyDataStructure() {
        System.out.println("=== DATA STRUCTURE VERIFICATION ===");
        System.out.println("Current X Column: " + currentXColumn);
        System.out.println("Current Y Column: " + currentYColumn);

        Map<String, Map<String, Integer>> data = crosstabData.getCrosstabData();
        System.out.println("Row keys (should match " + currentXColumn + "): " + data.keySet());

        if (!data.isEmpty()) {
            String firstRow = data.keySet().iterator().next();
            System.out.println("Column keys in first row (should match " + currentYColumn + " values): " +
                    data.get(firstRow).keySet());
        }

        System.out.println("Row totals: " + crosstabData.getRowTotals());
        System.out.println("Column totals: " + crosstabData.getColumnTotals());
        System.out.println("================================");
    }
}