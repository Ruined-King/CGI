import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import java.util.*;

public class CrosstabTable {
    private TableView<ObservableList<String>> tableView;
    private CrosstabData crosstabData;

    public CrosstabTable(TableView<ObservableList<String>> tableView, CrosstabData crosstabData) {
        this.tableView = tableView;
        this.crosstabData = crosstabData;
        setupTableStyle();
    }

    private void setupTableStyle() {
        tableView.setStyle(
                "-fx-background-color: white;" +
                        "-fx-control-inner-background: white;" +
                        "-fx-table-cell-border-color: #d3d3d3;" +
                        "-fx-table-header-border-color: #d3d3d3;" +
                        "-fx-selection-bar: #0078d4;" +
                        "-fx-selection-bar-non-focused: #e6f3ff;" +
                        "-fx-text-fill: black;" +
                        "-fx-border-color: #d3d3d3;" +
                        "-fx-border-width: 1px;"
        );
    }

    public void createCrosstabTable(String currentXColumn, String currentYColumn,
                                    boolean useMonthlyConversion, boolean useCumulativeEffective,
                                    boolean useCumulativeForCharts) {
        // Clear existing table
        tableView.getColumns().clear();
        tableView.getItems().clear();

        // Validate inputs
        if (currentXColumn == null || currentYColumn == null ||
                currentXColumn.trim().isEmpty() || currentYColumn.trim().isEmpty()) {
            throw new IllegalArgumentException("X and Y columns cannot be null or empty");
        }

        try {
            // Generate crosstab data with proper filtering and monthly conversion
            crosstabData.generateCrosstabData(currentXColumn, currentYColumn, true, useMonthlyConversion);

            Map<String, Map<String, Integer>> data = crosstabData.getCrosstabData();
            if (data == null || data.isEmpty()) {
                throw new IllegalStateException("No data to display in crosstab!");
            }

            // Debug: Print total count to verify data isn't being lost
            int totalCount = 0;
            for (Map<String, Integer> xRow : data.values()) {
                if (xRow != null) {
                    for (Integer count : xRow.values()) {
                        if (count != null) {
                            totalCount += count;
                        }
                    }
                }
            }
            System.out.println("Total count in crosstab: " + totalCount);

            // Get all unique Y values
            Set<String> allYValues = new TreeSet<>();

            if (useMonthlyConversion && crosstabData.isDateColumn(currentYColumn)) {
                // For date columns, get only months that have actual data
                allYValues = crosstabData.createMonthRangeForY(currentYColumn, true);
                System.out.println("Month range for Y: " + allYValues);
            } else {
                // For non-date columns, get all Y values that have data
                for (Map<String, Integer> xRow : data.values()) {
                    if (xRow != null) {
                        for (Map.Entry<String, Integer> entry : xRow.entrySet()) {
                            if (entry.getValue() != null && entry.getValue() > 0) {
                                allYValues.add(entry.getKey());
                            }
                        }
                    }
                }
            }

            // Convert to sorted list
            List<String> sortedYValues = new ArrayList<>(allYValues);
            if (useMonthlyConversion && crosstabData.isDateColumn(currentYColumn)) {
                sortedYValues = crosstabData.sortMonthYearValues(sortedYValues);
            }

            // Create first column (X values)
            TableColumn<ObservableList<String>, String> firstColumn = new TableColumn<>(currentXColumn);
            firstColumn.setCellValueFactory(param -> {
                if (param.getValue() != null && !param.getValue().isEmpty()) {
                    return new SimpleStringProperty(param.getValue().get(0));
                }
                return new SimpleStringProperty("");
            });
            firstColumn.setPrefWidth(150);
            styleTableColumn(firstColumn);
            tableView.getColumns().add(firstColumn);

            // Add Y value columns
            for (int i = 0; i < sortedYValues.size(); i++) {
                String yValue = sortedYValues.get(i);
                TableColumn<ObservableList<String>, String> column = new TableColumn<>(yValue);
                final int colIndex = i + 1;
                column.setCellValueFactory(param -> {
                    ObservableList<String> row = param.getValue();
                    if (row != null && colIndex < row.size()) {
                        return new SimpleStringProperty(row.get(colIndex));
                    } else {
                        return new SimpleStringProperty("0");
                    }
                });
                column.setPrefWidth(100);
                styleTableColumn(column);
                setCellFactory(column);
                tableView.getColumns().add(column);
            }

            // Add TOTAL column
            TableColumn<ObservableList<String>, String> totalColumn = new TableColumn<>("TOTAL");
            final int totalColIndex = sortedYValues.size() + 1;
            totalColumn.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                if (row != null && totalColIndex < row.size()) {
                    return new SimpleStringProperty(row.get(totalColIndex));
                } else {
                    return new SimpleStringProperty("0");
                }
            });
            totalColumn.setPrefWidth(100);
            styleTotalColumn(totalColumn);
            setCellFactory(totalColumn);
            tableView.getColumns().add(totalColumn);

            // Add cumulative column if needed
            if (useCumulativeEffective) {
                TableColumn<ObservableList<String>, String> xCumulativeColumn = new TableColumn<>("EFFECTIF CUMULÉ X");
                final int xCumColIndex = sortedYValues.size() + 2;
                xCumulativeColumn.setCellValueFactory(param -> {
                    ObservableList<String> row = param.getValue();
                    if (row != null && xCumColIndex < row.size()) {
                        return new SimpleStringProperty(row.get(xCumColIndex));
                    } else {
                        return new SimpleStringProperty("0");
                    }
                });
                xCumulativeColumn.setPrefWidth(150);
                styleCumulativeColumn(xCumulativeColumn);
                setCellFactory(xCumulativeColumn);
                tableView.getColumns().add(xCumulativeColumn);
            }

            // Get X values - only those with actual data
            Set<String> allXValues = new TreeSet<>();

            if (useMonthlyConversion && crosstabData.isDateColumn(currentXColumn)) {
                // For date columns, get only months that have actual data
                allXValues = crosstabData.createMonthRangeForX(currentXColumn, true);
                System.out.println("Month range for X: " + allXValues);
            } else {
                // For non-date columns, get all X values that have data
                for (Map.Entry<String, Map<String, Integer>> entry : data.entrySet()) {
                    if (entry.getValue() != null &&
                            entry.getValue().values().stream().filter(Objects::nonNull).mapToInt(Integer::intValue).sum() > 0) {
                        allXValues.add(entry.getKey());
                    }
                }
            }

            List<String> sortedXValues = new ArrayList<>(allXValues);
            if (useMonthlyConversion && crosstabData.isDateColumn(currentXColumn)) {
                sortedXValues = crosstabData.sortMonthYearValues(sortedXValues);
            } else {
                Collections.sort(sortedXValues);
            }

            // Calculate totals and cumulative values
            Map<String, Integer> rowTotals = new HashMap<>();
            Map<String, Integer> columnTotals = new HashMap<>();
            Map<String, Integer> xCumulativeEffective = new HashMap<>();
            Map<String, Integer> yCumulativeEffective = new HashMap<>();
            int grandTotal = 0;
            int xCumulativeSum = 0;

            // Initialize column totals
            for (String yValue : sortedYValues) {
                columnTotals.put(yValue, 0);
            }

            // Add data rows
            for (String xValue : sortedXValues) {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(xValue);

                Map<String, Integer> xRow = data.get(xValue);
                int rowTotal = 0;

                for (String yValue : sortedYValues) {
                    Integer count = (xRow != null) ? xRow.get(yValue) : null;
                    int countValue = (count != null) ? count : 0;
                    row.add(String.valueOf(countValue));

                    rowTotal += countValue;
                    columnTotals.put(yValue, columnTotals.get(yValue) + countValue);
                }

                // Add row total
                row.add(String.valueOf(rowTotal));
                rowTotals.put(xValue, rowTotal);
                grandTotal += rowTotal;

                // Add X cumulative effective
                if (useCumulativeEffective) {
                    xCumulativeSum += rowTotal;
                    row.add(String.valueOf(xCumulativeSum));
                    xCumulativeEffective.put(xValue, xCumulativeSum);
                }

                tableView.getItems().add(row);
            }

            // Calculate Y cumulative values
            if (useCumulativeEffective) {
                int yCumulativeSum = 0;
                for (String yValue : sortedYValues) {
                    yCumulativeSum += columnTotals.get(yValue);
                    yCumulativeEffective.put(yValue, yCumulativeSum);
                }
            }

            // Add TOTAL row
            ObservableList<String> totalRow = FXCollections.observableArrayList();
            totalRow.add("TOTAL");

            for (String yValue : sortedYValues) {
                totalRow.add(String.valueOf(columnTotals.get(yValue)));
            }

            totalRow.add(String.valueOf(grandTotal));

            if (useCumulativeEffective) {
                totalRow.add(String.valueOf(grandTotal));
            }

            tableView.getItems().add(totalRow);

            // Add Y CUMULATIVE row
            if (useCumulativeEffective) {
                ObservableList<String> yCumulativeRow = FXCollections.observableArrayList();
                yCumulativeRow.add("EFFECTIF CUMULÉ Y");

                for (String yValue : sortedYValues) {
                    yCumulativeRow.add(String.valueOf(yCumulativeEffective.get(yValue)));
                }

                yCumulativeRow.add(String.valueOf(grandTotal));
                yCumulativeRow.add(String.valueOf(grandTotal));

                tableView.getItems().add(yCumulativeRow);
            }

            // Store totals for chart usage
            crosstabData.setTotals(rowTotals, columnTotals, grandTotal);
            if (useCumulativeEffective) {
                crosstabData.setCumulativeEffective(xCumulativeEffective);
            }

            // Final debug output
            System.out.println("Grand total in table: " + grandTotal);
            System.out.println("Number of X values: " + sortedXValues.size());
            System.out.println("Number of Y values: " + sortedYValues.size());

        } catch (Exception e) {
            System.err.println("Error creating crosstab table: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create crosstab table", e);
        }
    }

    private void styleTableColumn(TableColumn<ObservableList<String>, String> column) {
        column.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: black;" +
                        "-fx-border-color: #d3d3d3;" +
                        "-fx-border-width: 1px;"
        );
    }

    private void styleCumulativeColumn(TableColumn<ObservableList<String>, String> column) {
        column.setStyle(
                "-fx-background-color: #fff3cd;" +
                        "-fx-text-fill: black;" +
                        "-fx-border-color: #856404;" +
                        "-fx-border-width: 2px;" +
                        "-fx-font-weight: bold;"
        );
    }

    private void styleTotalColumn(TableColumn<ObservableList<String>, String> column) {
        column.setStyle(
                "-fx-background-color: #fff3cd;" +
                        "-fx-text-fill: black;" +
                        "-fx-border-color: #856404;" +
                        "-fx-border-width: 2px;" +
                        "-fx-font-weight: bold;"
        );
    }

    private void setCellFactory(TableColumn<ObservableList<String>, String> column) {
        column.setCellFactory(col -> {
            TableCell<ObservableList<String>, String> cell = new TableCell<ObservableList<String>, String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-border-color: #d3d3d3; -fx-border-width: 0.5px; -fx-background-color: white;");
                    } else {
                        setText(item);

                        // Check if this is a total row, cumulative row, or total/cumulative column
                        ObservableList<String> rowData = getTableRow().getItem();
                        if (rowData != null && !rowData.isEmpty()) {
                            String rowLabel = rowData.get(0);
                            boolean isTotalRow = "TOTAL".equals(rowLabel);
                            boolean isYCumulativeRow = "EFFECTIF CUMULÉ Y".equals(rowLabel);
                            boolean isTotalColumn = "TOTAL".equals(column.getText());
                            boolean isXCumulativeColumn = "EFFECTIF CUMULÉ X".equals(column.getText());

                            if ((isTotalRow || isYCumulativeRow) && (isTotalColumn || isXCumulativeColumn)) {
                                // Grand total cell or intersection of cumulative/total
                                setStyle("-fx-border-color: #856404; -fx-border-width: 1px; " +
                                        "-fx-background-color: #ffeaa7; -fx-font-weight: bold; " +
                                        "-fx-text-fill: #856404;");
                            } else if (isTotalRow || isYCumulativeRow) {
                                // Total row or Y cumulative row
                                setStyle("-fx-border-color: #495057; -fx-border-width: 1px; " +
                                        "-fx-background-color: #f8f9fa; -fx-font-weight: bold; " +
                                        "-fx-text-fill: #495057;");
                            } else if (isTotalColumn || isXCumulativeColumn) {
                                // Total column or X cumulative column
                                setStyle("-fx-border-color: #856404; -fx-border-width: 1px; " +
                                        "-fx-background-color: #fff3cd; -fx-font-weight: bold; " +
                                        "-fx-text-fill: #856404;");
                            } else {
                                // Regular cell
                                setStyle("-fx-border-color: #d3d3d3; -fx-border-width: 0.5px; " +
                                        "-fx-background-color: white;");
                            }
                        }
                    }
                }
            };
            return cell;
        });
    }

    public TableView<ObservableList<String>> getTableView() {
        return tableView;
    }
}