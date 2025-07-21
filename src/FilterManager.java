import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilterManager {
    private final String[] OPERATORS = {"Equals", "IsEmpty", "IsFull", "LessThan", "GreaterThan", "LessOrEqual", "GreaterOrEqual", "Has", "Between"};
    private List<String> activeFilters = new ArrayList<>();
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();
    private TableViewController tableController;

    private TextField columnFilterField;
    private VBox filterControlsBox;
    private Label activeFiltersLabel;
    private FlowPane activeFiltersPane;

    private TextField valueField2;
    private Label andLabel;
    private ToggleButton orToggleButton;
    private CheckBox notCheckBox;
    private boolean isOrMode = false;

    // Custom AutoComplete TextFields instead of ComboBoxes
    private AutoCompleteTextField columnField;
    private AutoCompleteTextField operatorField;
    private AutoCompleteTextField valueField;
    private AutoCompleteTextField valueField2Custom;

    private Set<String> currentColumnValues = new HashSet<>();
    private List<String> allColumnValues = new ArrayList<>();
    private List<String> allHeaders = new ArrayList<>();
    private List<String> allOperators = new ArrayList<>();

    public FilterManager() {
        initializeUIComponents();
    }

    public List<ObservableList<String>> getFilteredRows() {
        if (activeFilters.isEmpty()) {
            return new ArrayList<>(allRows);
        }

        return allRows.stream()
                .filter(row -> rowMatchesFilters(row, headers))
                .collect(Collectors.toList());
    }

    private void initializeUIComponents() {
        allOperators = Arrays.asList(OPERATORS);

        columnFilterField = new TextField();
        columnFilterField.setPromptText("Filter columns (comma-separated)...");
        columnFilterField.setStyle("-fx-background-color: linear-gradient(to right, #ffffff, #ff4444); -fx-text-fill: black; -fx-border-color: #cc0000; -fx-border-width: 2px; -fx-font-size: 14px; -fx-padding: 8;");
        columnFilterField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Column filter changed: '" + oldValue + "' -> '" + newValue + "'");
            if (tableController != null) {
                applyColumnAndRowFilters();
            }
        });

        // Initialize custom AutoComplete fields
        columnField = new AutoCompleteTextField();
        operatorField = new AutoCompleteTextField();
        valueField = new AutoCompleteTextField();
        valueField2Custom = new AutoCompleteTextField();

        columnField.setPromptText("Select column...");
        operatorField.setPromptText("Select operator...");
        valueField.setPromptText("Enter value...");
        valueField2Custom.setPromptText("Enter second value...");

        String textFieldStyle = "-fx-background-color: linear-gradient(to right, #ffffff, #ff4444); -fx-text-fill: black; -fx-border-color: #cc0000; -fx-border-width: 2px; -fx-font-size: 12px; -fx-padding: 5;";

        columnField.setStyle(textFieldStyle);
        operatorField.setStyle(textFieldStyle);
        valueField.setStyle(textFieldStyle);
        valueField2Custom.setStyle(textFieldStyle);

        andLabel = new Label("and");
        andLabel.setTextFill(Color.WHITE);
        andLabel.setFont(Font.font("Verdana", 12));

        notCheckBox = new CheckBox("NOT");
        notCheckBox.setTextFill(Color.WHITE);
        notCheckBox.setFont(Font.font("Verdana", 12));
        notCheckBox.setStyle("-fx-text-fill: white;");

        orToggleButton = new ToggleButton("AND");
        orToggleButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");

        Button addFilterButton = new Button("Add Filter");
        Button clearFiltersButton = new Button("Clear All");

        valueField2Custom.setVisible(false);
        valueField2Custom.setManaged(false);
        andLabel.setVisible(false);
        andLabel.setManaged(false);

        // Setup autocomplete data
        setupAutoCompleteData();

        // Setup field listeners
        columnField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                updateAutoCompleteValues(newVal.trim());
            }
        });

        operatorField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            String selectedOp = newVal.trim();
            switch (selectedOp) {
                case "IsEmpty":
                case "IsFull":
                    valueField.setVisible(false);
                    valueField.setManaged(false);
                    valueField2Custom.setVisible(false);
                    valueField2Custom.setManaged(false);
                    andLabel.setVisible(false);
                    andLabel.setManaged(false);
                    break;

                case "Between":
                    valueField.setVisible(true);
                    valueField.setManaged(true);
                    valueField2Custom.setVisible(true);
                    valueField2Custom.setManaged(true);
                    andLabel.setVisible(true);
                    andLabel.setManaged(true);
                    valueField.setPromptText("From value...");
                    valueField2Custom.setPromptText("To value...");
                    break;

                default:
                    valueField.setVisible(true);
                    valueField.setManaged(true);
                    valueField2Custom.setVisible(false);
                    valueField2Custom.setManaged(false);
                    andLabel.setVisible(false);
                    andLabel.setManaged(false);
                    valueField.setPromptText("Enter value...");
                    break;
            }
        });

        orToggleButton.setOnAction(e -> {
            isOrMode = orToggleButton.isSelected();
            if (isOrMode) {
                orToggleButton.setText("OR");
                orToggleButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
            } else {
                orToggleButton.setText("AND");
                orToggleButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
            }

            if (!activeFilters.isEmpty()) {
                applyColumnAndRowFilters();
            }
        });

        addFilterButton.setOnAction(e -> {
            String column = columnField.getText().trim();
            String operator = operatorField.getText().trim();
            String value = valueField.getText().trim();
            String value2 = valueField2Custom.getText().trim();
            boolean isNot = notCheckBox.isSelected();

            if (!column.isEmpty() && !operator.isEmpty()) {
                String filter = (isNot ? "NOT " : "") + column + " " + operator;

                if (operator.equals("Between")) {
                    if (!value.isEmpty() && !value2.isEmpty()) {
                        filter += " " + value + " " + value2;
                    } else {
                        showAlert("Error", "Please enter both values for Between operator!");
                        return;
                    }
                } else if (!operator.equals("IsEmpty") && !operator.equals("IsFull")) {
                    filter += " " + value;
                }

                addFilter(filter);
                refreshActiveFiltersDisplay();
                applyColumnAndRowFilters();

                // Clear fields
                columnField.clear();
                operatorField.clear();
                valueField.clear();
                valueField2Custom.clear();
                notCheckBox.setSelected(false);
            }
        });

        clearFiltersButton.setOnAction(e -> {
            clearAllFilters();
            refreshActiveFiltersDisplay();
            applyColumnAndRowFilters();
        });

        // Create layout with NOT checkbox
        HBox firstRow = new HBox(10, columnField, operatorField, valueField, andLabel, valueField2Custom, notCheckBox, addFilterButton);
        firstRow.setAlignment(Pos.CENTER_LEFT);
        HBox secondRow = new HBox(10, orToggleButton, clearFiltersButton);
        secondRow.setAlignment(Pos.CENTER_LEFT);

        filterControlsBox = new VBox(5, firstRow, secondRow);
        filterControlsBox.setStyle("-fx-padding: 10; -fx-background-color: #2c3e50; -fx-background-radius: 5;");

        // Active filters display
        activeFiltersLabel = new Label("Active Filters:");
        activeFiltersLabel.setTextFill(Color.WHITE);
        activeFiltersLabel.setFont(Font.font("Verdana", 12));

        activeFiltersPane = new FlowPane();
        activeFiltersPane.setHgap(5);
        activeFiltersPane.setVgap(5);
        activeFiltersPane.setStyle("-fx-padding: 5; -fx-background-color: #34495e; -fx-background-radius: 3;");
    }

    private void setupAutoCompleteData() {
        // Setup autocomplete for operator field
        operatorField.setItems(allOperators);
    }

    private void updateAutoCompleteValues(String columnName) {
        if (columnName == null || headers.isEmpty() || allRows.isEmpty()) {
            return;
        }

        int columnIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).trim().equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex != -1) {
            Set<String> values = new HashSet<>();
            final int colIndex = columnIndex;

            allRows.forEach(row -> {
                if (colIndex < row.size()) {
                    String value = row.get(colIndex);
                    if (value != null && !value.trim().isEmpty()) {
                        values.add(value.trim());
                    }
                }
            });

            List<String> sortedValues = values.stream()
                    .sorted()
                    .collect(Collectors.toList());

            allColumnValues = sortedValues;
            valueField.setItems(sortedValues);
            valueField2Custom.setItems(sortedValues);
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyColumnAndRowFilters() {
        if (tableController == null) {
            System.out.println("TableController is null, cannot apply filters");
            return;
        }

        String columnInput = columnFilterField.getText().trim();
        Predicate<ObservableList<String>> rowFilter = row -> rowMatchesFilters(row, headers);

        System.out.println("Applying filters with column input: '" + columnInput + "' and " + activeFilters.size() + " row filters (Mode: " + (isOrMode ? "OR" : "AND") + ")");
        tableController.applyFilters(columnInput, rowFilter);
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = new ArrayList<>(headers);
        this.allRows = new ArrayList<>(allRows);
        this.allHeaders = new ArrayList<>(headers);

        System.out.println("FilterManager.setData() called with " + headers.size() + " headers and " + allRows.size() + " rows");

        // Set autocomplete items
        columnField.setItems(headers);
        operatorField.setItems(allOperators);
    }

    public void setTableController(TableViewController tableController) {
        this.tableController = tableController;
        System.out.println("TableController set in FilterManager");

        if (headers.size() > 0 && allRows.size() > 0) {
            System.out.println("Applying initial filters to display table");
            applyColumnAndRowFilters();
        }
    }

    // Custom AutoComplete TextField Class
    public static class AutoCompleteTextField extends TextField {
        private List<String> items = new ArrayList<>();
        private Popup popup;
        private ListView<String> listView;
        private boolean isPopupShowing = false;

        public AutoCompleteTextField() {
            super();
            createPopup();
            setupEventHandlers();
        }

        private void createPopup() {
            popup = new Popup();
            popup.setAutoHide(true);
            popup.setHideOnEscape(true);

            listView = new ListView<>();
            listView.setPrefHeight(200);
            listView.setStyle(
                    "-fx-background-color: linear-gradient(to right, #ffffff, #ff4444);" +
                            "-fx-border-color: #cc0000;" +
                            "-fx-border-width: 2px;" +
                            "-fx-font-size: 12px;"
            );

            // Style individual cells to match the gradient theme
            listView.setCellFactory(lv -> {
                ListCell<String> cell = new ListCell<String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            setStyle(
                                    "-fx-background-color: transparent;" +
                                            "-fx-text-fill: black;" +
                                            "-fx-font-size: 12px;" +
                                            "-fx-padding: 5;"
                            );
                        }
                    }
                };

                // Hover effect for cells
                cell.setOnMouseEntered(e -> {
                    if (!cell.isEmpty()) {
                        cell.setStyle(
                                "-fx-background-color: rgba(255, 255, 255, 0.3);" +
                                        "-fx-text-fill: black;" +
                                        "-fx-font-size: 12px;" +
                                        "-fx-padding: 5;"
                        );
                    }
                });

                cell.setOnMouseExited(e -> {
                    if (!cell.isEmpty()) {
                        cell.setStyle(
                                "-fx-background-color: transparent;" +
                                        "-fx-text-fill: black;" +
                                        "-fx-font-size: 12px;" +
                                        "-fx-padding: 5;"
                        );
                    }
                });

                return cell;
            });

            listView.setOnMouseClicked(this::handleListViewClick);
            listView.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    selectCurrentItem();
                }
            });

            popup.getContent().add(listView);
        }

        private void setupEventHandlers() {
            textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateSuggestions(newVal);
                }
            });

            setOnKeyPressed(event -> {
                if (isPopupShowing) {
                    switch (event.getCode()) {
                        case DOWN:
                            event.consume();
                            if (listView.getSelectionModel().getSelectedIndex() < listView.getItems().size() - 1) {
                                listView.getSelectionModel().selectNext();
                            }
                            break;
                        case UP:
                            event.consume();
                            if (listView.getSelectionModel().getSelectedIndex() > 0) {
                                listView.getSelectionModel().selectPrevious();
                            }
                            break;
                        case ENTER:
                            event.consume();
                            selectCurrentItem();
                            break;
                        case ESCAPE:
                            event.consume();
                            hidePopup();
                            break;
                        case TAB:
                            hidePopup();
                            break;
                    }
                }
            });

            focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    hidePopup();
                }
            });
        }

        private void updateSuggestions(String input) {
            if (input.isEmpty()) {
                hidePopup();
                return;
            }

            String searchText = input.toLowerCase().trim();
            List<String> filteredItems = items.stream()
                    .filter(item -> item.toLowerCase().contains(searchText))
                    .collect(Collectors.toList());

            // Sort suggestions: exact matches first, then starts with, then contains
            filteredItems.sort((a, b) -> {
                String aLower = a.toLowerCase();
                String bLower = b.toLowerCase();

                boolean aExact = aLower.equals(searchText);
                boolean bExact = bLower.equals(searchText);
                if (aExact && !bExact) return -1;
                if (!aExact && bExact) return 1;

                boolean aStarts = aLower.startsWith(searchText);
                boolean bStarts = bLower.startsWith(searchText);
                if (aStarts && !bStarts) return -1;
                if (!aStarts && bStarts) return 1;

                return a.compareToIgnoreCase(b);
            });

            if (filteredItems.isEmpty()) {
                hidePopup();
            } else {
                // Limit to 10 suggestions for performance
                if (filteredItems.size() > 10) {
                    filteredItems = filteredItems.subList(0, 10);
                }

                listView.setItems(FXCollections.observableArrayList(filteredItems));
                listView.getSelectionModel().clearSelection();
                showPopup();
            }
        }

        private void handleListViewClick(MouseEvent event) {
            if (event.getClickCount() == 1) {
                selectCurrentItem();
            }
        }

        private void selectCurrentItem() {
            String selectedItem = listView.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                setText(selectedItem);
                positionCaret(selectedItem.length());
                hidePopup();
            }
        }

        private void showPopup() {
            if (!isPopupShowing && getScene() != null) {
                // Get the bounds of the TextField in screen coordinates
                var bounds = localToScreen(getBoundsInLocal());
                popup.show(this, bounds.getMinX(), bounds.getMaxY());
                isPopupShowing = true;
            }
        }

        private void hidePopup() {
            if (isPopupShowing) {
                popup.hide();
                isPopupShowing = false;
            }
        }

        public void setItems(List<String> items) {
            this.items = new ArrayList<>(items);
        }
    }

    // Rest of the existing methods remain the same...
    public void addFilter(String filter) {
        if (!activeFilters.contains(filter)) {
            activeFilters.add(filter);
            System.out.println("Added filter: " + filter);
        }
    }

    public void removeFilter(String filter) {
        activeFilters.remove(filter);
        System.out.println("Removed filter: " + filter);
    }

    public void clearAllFilters() {
        activeFilters.clear();
        System.out.println("Cleared all filters");
    }

    public List<String> getActiveFilters() {
        return new ArrayList<>(activeFilters);
    }

    public boolean hasActiveFilters() {
        return !activeFilters.isEmpty();
    }

    public void refreshActiveFiltersDisplay() {
        activeFiltersPane.getChildren().clear();

        Label modeLabel = new Label(isOrMode ? "OR Mode" : "AND Mode");
        modeLabel.setStyle("-fx-background-color: " + (isOrMode ? "#e67e22" : "#27ae60") + "; -fx-text-fill: white; -fx-font-size: 10px; -fx-padding: 3 8; -fx-background-radius: 10;");
        if (!activeFilters.isEmpty()) {
            activeFiltersPane.getChildren().add(modeLabel);
        }

        for (String filter : activeFilters) {
            Button filterButton = new Button(filter);
            if (filter.startsWith("NOT ")) {
                filterButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
            } else {
                filterButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
            }

            filterButton.setOnAction(e -> {
                String textToRemove = ((Button) e.getSource()).getText();
                activeFilters.removeIf(f -> f.equals(textToRemove));
                refreshActiveFiltersDisplay();
                applyColumnAndRowFilters();
            });

            activeFiltersPane.getChildren().add(filterButton);
        }
    }

    public void refreshUI() {
        System.out.println("Refreshing FilterManager UI");
        refreshActiveFiltersDisplay();
        applyColumnAndRowFilters();
    }

    public boolean rowMatchesFilters(ObservableList<String> row, List<String> headers) {
        if (activeFilters.isEmpty()) {
            return true;
        }

        if (isOrMode) {
            for (String filter : activeFilters) {
                if (matchesSingleFilter(row, filter, headers)) {
                    return true;
                }
            }
            return false;
        } else {
            for (String filter : activeFilters) {
                if (!matchesSingleFilter(row, filter, headers)) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean matchesSingleFilter(ObservableList<String> row, String filter, List<String> headers) {
        boolean isNot = false;
        String actualFilter = filter;

        if (filter.startsWith("NOT ")) {
            isNot = true;
            actualFilter = filter.substring(4);
        }

        String operator = null;
        int operatorIndex = -1;

        for (String op : OPERATORS) {
            int index = actualFilter.indexOf(" " + op + " ");
            if (index != -1) {
                operator = op;
                operatorIndex = index;
                break;
            }
            if (actualFilter.endsWith(" " + op)) {
                operator = op;
                operatorIndex = actualFilter.length() - op.length() - 1;
                break;
            }
        }

        if (operator == null) return false;

        String columnName = actualFilter.substring(0, operatorIndex).trim();
        String filterValue = "";
        String filterValue2 = "";

        if (!operator.equals("IsEmpty") && !operator.equals("IsFull")) {
            if (operatorIndex + operator.length() + 2 < actualFilter.length()) {
                String remainingValue = actualFilter.substring(operatorIndex + operator.length() + 2).trim();

                if (operator.equals("Between")) {
                    String[] values = remainingValue.split(" ", 2);
                    if (values.length >= 2) {
                        filterValue = values[0].trim();
                        filterValue2 = values[1].trim();
                    }
                } else {
                    filterValue = remainingValue;
                }
            }
        }

        int columnIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).trim().equalsIgnoreCase(columnName)) {
                columnIndex = i;
                break;
            }
        }

        if (columnIndex == -1 || columnIndex >= row.size()) return false;

        String rowValue = row.get(columnIndex);
        if (rowValue == null) rowValue = "";
        rowValue = rowValue.trim();

        boolean result = applyOperator(rowValue, filterValue, filterValue2, operator);

        return isNot ? !result : result;
    }

    private boolean applyOperator(String rowValue, String filterValue, String filterValue2, String operator) {
        switch (operator) {
            case "IsEmpty":
                return rowValue.isEmpty();

            case "IsFull":
                return !rowValue.isEmpty();

            case "Equals":
                if (filterValue.equalsIgnoreCase("null") || filterValue.isEmpty()) {
                    return rowValue.isEmpty();
                } else {
                    return rowValue.equals(filterValue);
                }

            case "Has":
                return rowValue.toLowerCase().contains(filterValue.toLowerCase());

            case "Between":
                return betweenComparison(rowValue, filterValue, filterValue2);

            case "LessThan":
            case "LessOrEqual":
            case "GreaterThan":
            case "GreaterOrEqual":
                return compareValues(rowValue, filterValue, operator);

            default:
                return rowValue.equals(filterValue);
        }
    }

    private boolean betweenComparison(String rowValue, String fromValue, String toValue) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            Date rowDate = sdf.parse(rowValue);
            Date fromDate = sdf.parse(fromValue);
            Date toDate = sdf.parse(toValue);

            return (rowDate.equals(fromDate) || rowDate.after(fromDate)) &&
                    (rowDate.equals(toDate) || rowDate.before(toDate));
        } catch (Exception dateEx) {
            try {
                double rowNum = Double.parseDouble(rowValue);
                double fromNum = Double.parseDouble(fromValue);
                double toNum = Double.parseDouble(toValue);

                return rowNum >= fromNum && rowNum <= toNum;
            } catch (Exception numEx) {
                return false;
            }
        }
    }

    private boolean compareValues(String rowValue, String filterValue, String operator) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            Date rowDate = sdf.parse(rowValue);
            Date filterDate = sdf.parse(filterValue);

            switch (operator) {
                case "LessThan": return rowDate.before(filterDate);
                case "LessOrEqual": return rowDate.before(filterDate) || rowDate.equals(filterDate);
                case "GreaterThan": return rowDate.after(filterDate);
                case "GreaterOrEqual": return rowDate.after(filterDate) || rowDate.equals(filterDate);
            }
        } catch (Exception dateEx) {
            try {
                double rowNum = Double.parseDouble(rowValue);
                double compareNum = Double.parseDouble(filterValue);

                switch (operator) {
                    case "LessThan": return rowNum < compareNum;
                    case "LessOrEqual": return rowNum <= compareNum;
                    case "GreaterThan": return rowNum > compareNum;
                    case "GreaterOrEqual": return rowNum >= compareNum;
                }
            } catch (Exception numEx) {
                return false;
            }
        }
        return false;
    }

    public TextField getColumnFilterField() {
        return columnFilterField;
    }

    public VBox getFilterControlsBox() {
        return (VBox) filterControlsBox;
    }

    public Label getActiveFiltersLabel() {
        return activeFiltersLabel;
    }

    public FlowPane getActiveFiltersPane() {
        return activeFiltersPane;
    }

    public String[] getOperators() {
        return OPERATORS;
    }
}