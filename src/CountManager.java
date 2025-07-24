import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Popup;
import javafx.geometry.Pos;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CountManager {
    private final List<HBox> countSelectors = new ArrayList<>();
    private final VBox countSection;
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();

    private final String[] OPERATORS = {"Equals", "IsEmpty", "IsFull", "LessThan", "GreaterThan", "LessOrEqual", "GreaterOrEqual", "Has", "Between"};

    private Button countButton;
    private Label countResultLabel;
    private ToggleButton orToggleButton;
    private boolean isOrMode = false;

    public CountManager(VBox countSection) {
        this.countSection = countSection;
        initializeCountSection();
    }

    private void initializeCountSection() {
        Label countLabel = new Label("🔢 Count Rows:");
        countLabel.setTextFill(Color.WHITE);
        countLabel.setFont(Font.font("Verdana", 14));

        Button addSelectorButton = new Button("+ Add Condition");
        addSelectorButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        addSelectorButton.setOnAction(e -> addCountSelector());

        orToggleButton = new ToggleButton("AND");
        orToggleButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
        orToggleButton.setOnAction(e -> {
            isOrMode = orToggleButton.isSelected();
            if (isOrMode) {
                orToggleButton.setText("OR");
                orToggleButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
            } else {
                orToggleButton.setText("AND");
                orToggleButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 5 10;");
            }
        });

        countButton = new Button("🔢 COUNT");
        countButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        countButton.setOnAction(e -> performCount());

        countResultLabel = new Label("");
        countResultLabel.setTextFill(Color.LIGHTGREEN);
        countResultLabel.setFont(Font.font("Verdana", 16));

        HBox buttonRow = new HBox(10, addSelectorButton, orToggleButton, countButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        countSection.getChildren().addAll(countLabel, buttonRow, countResultLabel);
        countSection.setStyle("-fx-padding: 10; -fx-background-color: #2c3e50; -fx-background-radius: 5;");
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = new ArrayList<>(headers);
        this.allRows = new ArrayList<>(allRows);
    }

    public void addCountSelector() {
        AutoCompleteTextField columnField = new AutoCompleteTextField();
        AutoCompleteTextField operatorField = new AutoCompleteTextField();
        AutoCompleteTextField valueField = new AutoCompleteTextField();
        AutoCompleteTextField valueField2 = new AutoCompleteTextField();

        Label andLabel = new Label("and");
        andLabel.setTextFill(Color.WHITE);
        andLabel.setFont(Font.font("Verdana", 12));

        CheckBox notCheckBox = new CheckBox("NOT");
        notCheckBox.setTextFill(Color.WHITE);
        notCheckBox.setFont(Font.font("Verdana", 12));
        notCheckBox.setStyle("-fx-text-fill: white;");

        Button removeButton = new Button("🗑️");

        columnField.setPromptText("Select column...");
        operatorField.setPromptText("Select operator...");
        valueField.setPromptText("Enter value...");
        valueField2.setPromptText("Enter second value...");

        String textFieldStyle = "-fx-background-color: linear-gradient(to right, #ffffff, #ff4444); -fx-text-fill: black; -fx-border-color: #ff4444; -fx-border-width: 2px; -fx-font-size: 12px; -fx-padding: 5;";

        columnField.setStyle(textFieldStyle);
        operatorField.setStyle(textFieldStyle);
        valueField.setStyle(textFieldStyle);
        valueField2.setStyle(textFieldStyle);

        columnField.setItems(headers);
        operatorField.setItems(Arrays.asList(OPERATORS));

        valueField2.setVisible(false);
        valueField2.setManaged(false);
        andLabel.setVisible(false);
        andLabel.setManaged(false);

        columnField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                updateValueSuggestions(newVal.trim(), valueField);
                updateValueSuggestions(newVal.trim(), valueField2);
            }
        });

        operatorField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;

            valueField.clear();
            valueField2.clear();

            String selectedOp = newVal.trim();
            switch (selectedOp) {
                case "IsEmpty":
                case "IsFull":
                    valueField.setVisible(false);
                    valueField.setManaged(false);
                    valueField2.setVisible(false);
                    valueField2.setManaged(false);
                    andLabel.setVisible(false);
                    andLabel.setManaged(false);
                    break;

                case "Between":
                    valueField.setVisible(true);
                    valueField.setManaged(true);
                    valueField2.setVisible(true);
                    valueField2.setManaged(true);
                    andLabel.setVisible(true);
                    andLabel.setManaged(true);
                    valueField.setPromptText("From value...");
                    valueField2.setPromptText("To value...");
                    break;

                default:
                    valueField.setVisible(true);
                    valueField.setManaged(true);
                    valueField2.setVisible(false);
                    valueField2.setManaged(false);
                    andLabel.setVisible(false);
                    andLabel.setManaged(false);
                    valueField.setPromptText("Enter value...");
                    break;
            }
        });

        HBox pair = new HBox(10, columnField, operatorField, valueField, andLabel, valueField2, notCheckBox, removeButton);
        pair.setStyle("-fx-padding: 5; -fx-background-color: #34495e; -fx-background-radius: 3;");
        pair.setAlignment(Pos.CENTER_LEFT);

        removeButton.setOnAction(e -> {
            countSelectors.remove(pair);
            countSection.getChildren().remove(pair);
        });

        countSelectors.add(pair);

        int insertIndex = countSection.getChildren().size() - 1;
        if (insertIndex < 0) insertIndex = countSection.getChildren().size();
        countSection.getChildren().add(insertIndex, pair);
    }

    private void updateValueSuggestions(String columnName, AutoCompleteTextField valueField) {
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

            Platform.runLater(() -> valueField.setItems(sortedValues));
        }
    }

    private void performCount() {
        long count = countMatchingRows();
        countResultLabel.setText("Count: " + count + " rows");
    }

    public long countMatchingRows() {
        return allRows.stream().filter(this::rowMatchesSelectors).count();
    }

    private boolean rowMatchesSelectors(ObservableList<String> row) {
        if (countSelectors.isEmpty()) {
            return true;
        }

        if (isOrMode) {
            for (HBox pair : countSelectors) {
                if (evaluateCondition(row, pair)) {
                    return true;
                }
            }
            return false;
        } else {
            for (HBox pair : countSelectors) {
                if (!evaluateCondition(row, pair)) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean evaluateCondition(ObservableList<String> row, HBox pair) {
        AutoCompleteTextField columnField = (AutoCompleteTextField) pair.getChildren().get(0);
        AutoCompleteTextField operatorField = (AutoCompleteTextField) pair.getChildren().get(1);
        AutoCompleteTextField valueField = (AutoCompleteTextField) pair.getChildren().get(2);
        AutoCompleteTextField valueField2 = (AutoCompleteTextField) pair.getChildren().get(4);
        CheckBox notCheckBox = (CheckBox) pair.getChildren().get(5);

        String column = columnField.getText();
        String operator = operatorField.getText();

        if (column == null || operator == null || column.trim().isEmpty() || operator.trim().isEmpty()) {
            return true;
        }

        column = column.trim();
        operator = operator.trim();

        int colIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).trim().equalsIgnoreCase(column)) {
                colIndex = i;
                break;
            }
        }

        if (colIndex == -1 || colIndex >= row.size()) {
            return false;
        }

        String rowValue = row.get(colIndex);
        if (rowValue == null) rowValue = "";
        rowValue = rowValue.trim();

        String filterValue = valueField.getText();
        String filterValue2 = valueField2.getText();

        if (filterValue == null) filterValue = "";
        filterValue = filterValue.trim();

        if (filterValue2 == null) filterValue2 = "";
        filterValue2 = filterValue2.trim();

        boolean result = applyOperator(rowValue, filterValue, filterValue2, operator);

        if (notCheckBox.isSelected()) {
            result = !result;
        }

        return result;
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
        if (fromValue.isEmpty() || toValue.isEmpty()) {
            return false;
        }

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
                return rowValue.compareToIgnoreCase(fromValue) >= 0 &&
                        rowValue.compareToIgnoreCase(toValue) <= 0;
            }
        }
    }

    private boolean compareValues(String rowValue, String filterValue, String operator) {
        if (filterValue.isEmpty()) {
            return false;
        }

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
                int comparison = rowValue.compareToIgnoreCase(filterValue);
                switch (operator) {
                    case "LessThan": return comparison < 0;
                    case "LessOrEqual": return comparison <= 0;
                    case "GreaterThan": return comparison > 0;
                    case "GreaterOrEqual": return comparison >= 0;
                }
            }
        }
        return false;
    }

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

    public VBox getCountSection() {
        return countSection;
    }

    public Button getCountButton() {
        return countButton;
    }
}
