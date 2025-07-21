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
                allYValues = crosstabData.createMonthRangeForY(currentYColumn, true);
                System.out.println("Month range for Y: " + allYValues);
            } else {
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

            List<String> sortedYValues = new ArrayList<>(allYValues);
            if (useMonthlyConversion && crosstabData.isDateColumn(currentYColumn)) {
                sortedYValues = crosstabData.sortMonthYearValues(sortedYValues);
            }

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

            Set<String> allXValues = new TreeSet<>();

            if (useMonthlyConversion && crosstabData.isDateColumn(currentXColumn)) {
                allXValues = crosstabData.createMonthRangeForX(currentXColumn, true);
                System.out.println("Month range for X: " + allXValues);
            } else {
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

            Map<String, Integer> rowTotals = new HashMap<>();
            Map<String, Integer> columnTotals = new HashMap<>();
            Map<String, Integer> xCumulativeEffective = new HashMap<>();
            Map<String, Integer> yCumulativeEffective = new HashMap<>();
            int grandTotal = 0;
            int xCumulativeSum = 0;

            for (String yValue : sortedYValues) {
                columnTotals.put(yValue, 0);
            }

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

                row.add(String.valueOf(rowTotal));
                rowTotals.put(xValue, rowTotal);
                grandTotal += rowTotal;

                if (useCumulativeEffective) {
                    xCumulativeSum += rowTotal;
                    row.add(String.valueOf(xCumulativeSum));
                    xCumulativeEffective.put(xValue, xCumulativeSum);
                }

                tableView.getItems().add(row);
            }

            if (useCumulativeEffective) {
                int yCumulativeSum = 0;
                for (String yValue : sortedYValues) {
                    yCumulativeSum += columnTotals.get(yValue);
                    yCumulativeEffective.put(yValue, yCumulativeSum);
                }
            }

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

            crosstabData.setTotals(rowTotals, columnTotals, grandTotal);
            if (useCumulativeEffective) {
                crosstabData.setCumulativeEffective(xCumulativeEffective);
            }

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

                        ObservableList<String> rowData = getTableRow().getItem();
                        if (rowData != null && !rowData.isEmpty()) {
                            String rowLabel = rowData.get(0);
                            boolean isTotalRow = "TOTAL".equals(rowLabel);
                            boolean isYCumulativeRow = "EFFECTIF CUMULÉ Y".equals(rowLabel);
                            boolean isTotalColumn = "TOTAL".equals(column.getText());
                            boolean isXCumulativeColumn = "EFFECTIF CUMULÉ X".equals(column.getText());

                            if ((isTotalRow || isYCumulativeRow) && (isTotalColumn || isXCumulativeColumn)) {
                                setStyle("-fx-border-color: #856404; -fx-border-width: 1px; " +
                                        "-fx-background-color: #ffeaa7; -fx-font-weight: bold; " +
                                        "-fx-text-fill: #856404;");
                            } else if (isTotalRow || isYCumulativeRow) {
                                setStyle("-fx-border-color: #495057; -fx-border-width: 1px; " +
                                        "-fx-background-color: #f8f9fa; -fx-font-weight: bold; " +
                                        "-fx-text-fill: #495057;");
                            } else if (isTotalColumn || isXCumulativeColumn) {
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
