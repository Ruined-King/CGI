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

import java.util.*;
import java.util.stream.Collectors;

public class CountManager {
    private final List<HBox> countSelectors = new ArrayList<>();
    private final VBox countSection;
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();
    private final String[] OPERATORS = {"Equals", "IsEmpty", "IsFull", "LessThan", "GreaterThan", "LessOrEqual", "GreaterOrEqual", "Has"};
    private Button countButton;
    private Label countResultLabel;

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

        countButton = new Button("🔢 COUNT");
        countButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        countButton.setOnAction(e -> performCount());

        countResultLabel = new Label("");
        countResultLabel.setTextFill(Color.LIGHTGREEN);
        countResultLabel.setFont(Font.font("Verdana", 16));

        countSection.getChildren().addAll(countLabel, addSelectorButton, countResultLabel);
        countSection.setStyle("-fx-padding: 10; -fx-background-color: #2c3e50; -fx-background-radius: 5;");
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = new ArrayList<>(headers);
        this.allRows = new ArrayList<>(allRows);
    }

    public void addCountSelector() {
        ComboBox<String> columnCombo = new ComboBox<>();
        ComboBox<String> operatorCombo = new ComboBox<>();
        ComboBox<String> valueCombo = new ComboBox<>();
        TextField valueField = new TextField();
        Button removeButton = new Button("🗑️");

        columnCombo.setPromptText("Select column...");
        operatorCombo.setPromptText("Select operator...");
        valueCombo.setPromptText("Select value...");
        valueField.setPromptText("Enter value...");

        String textFieldStyle = "-fx-background-color: linear-gradient(to right, #ffffff, #ff4444); -fx-text-fill: black; -fx-border-color: #ff4444; -fx-border-width: 2px; -fx-font-size: 12px; -fx-padding: 5;";
        valueField.setStyle(textFieldStyle);

        String comboBoxStyle = "-fx-background-color: linear-gradient(to right, #ffffff, #ff4444); -fx-text-fill: black; -fx-border-color: #ff4444; -fx-border-width: 2px; -fx-font-size: 12px;";
        columnCombo.setStyle(comboBoxStyle);
        operatorCombo.setStyle(comboBoxStyle);
        valueCombo.setStyle(comboBoxStyle);

        columnCombo.setItems(FXCollections.observableArrayList(headers));
        operatorCombo.setItems(FXCollections.observableArrayList(OPERATORS));

        valueCombo.setVisible(false);
        valueField.setVisible(false);

        // Fix the ComboBox autoselection issue
        valueCombo.setEditable(true);
        valueCombo.getEditor().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.SPACE) {
                event.consume();
                String currentText = valueCombo.getEditor().getText();
                int caretPos = valueCombo.getEditor().getCaretPosition();
                String newText = currentText.substring(0, caretPos) + " " + currentText.substring(caretPos);
                valueCombo.getEditor().setText(newText);
                valueCombo.getEditor().positionCaret(caretPos + 1);
            }
        });

        setupColumnAutocomplete(columnCombo, valueCombo);

        operatorCombo.setOnAction(e -> {
            valueCombo.getEditor().clear();
            valueField.clear();

            String selectedOp = operatorCombo.getValue();
            if (selectedOp != null) {
                switch (selectedOp) {
                    case "IsEmpty":
                    case "IsFull":
                        valueCombo.setVisible(false);
                        valueCombo.setManaged(false);
                        valueField.setVisible(false);
                        valueCombo.setManaged(false);
                        break;
                    case "Equals":
                    case "Has":
                        valueCombo.setVisible(true);
                        valueCombo.setManaged(true);
                        valueField.setVisible(false);
                        valueField.setManaged(false);

                        populateValueCombo(columnCombo.getValue(), valueCombo);
                        break;
                    case "LessThan":
                    case "GreaterThan":
                    case "LessOrEqual":
                    case "GreaterOrEqual":
                        valueCombo.setVisible(false);
                        valueCombo.setManaged(false);
                        valueField.setVisible(true);
                        valueField.setManaged(true);
                        break;
                }
            }
        });

        HBox pair = new HBox(10, columnCombo, operatorCombo, valueCombo, valueField, removeButton);
        pair.setStyle("-fx-padding: 5; -fx-background-color: #34495e; -fx-background-radius: 3;");

        removeButton.setOnAction(e -> {
            countSelectors.remove(pair);
            countSection.getChildren().remove(pair);
        });

        countSelectors.add(pair);

        int insertIndex = countSection.getChildren().size() - 2;
        if (insertIndex < 0) insertIndex = countSection.getChildren().size();
        countSection.getChildren().add(insertIndex, pair);
    }

    private void setupColumnAutocomplete(ComboBox<String> columnCombo, ComboBox<String> valueCombo) {
        columnCombo.setOnAction(e -> {
            String selected = columnCombo.getValue();
            if (selected != null && !selected.trim().isEmpty()) {
                populateValueCombo(selected.trim(), valueCombo);
            }
        });

        columnCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                populateValueCombo(newVal.trim(), valueCombo);
            }
        });
    }


    private void populateValueCombo(String columnName, ComboBox<String> valueCombo) {
        int colIndex = headers.indexOf(columnName);
        if (colIndex == -1) return;

        Set<String> values = allRows.stream()
                .filter(row -> colIndex < row.size())
                .map(row -> row.get(colIndex))
                .filter(val -> val != null && !val.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toSet());

        List<String> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);

        Platform.runLater(() -> valueCombo.setItems(FXCollections.observableArrayList(sortedValues)));
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

        for (HBox pair : countSelectors) {
            ComboBox<String> columnCombo = (ComboBox<String>) pair.getChildren().get(0);
            ComboBox<String> operatorCombo = (ComboBox<String>) pair.getChildren().get(1);
            ComboBox<String> valueCombo = (ComboBox<String>) pair.getChildren().get(2);
            TextField valueField = (TextField) pair.getChildren().get(3);

            String column = columnCombo.getValue();
            String operator = operatorCombo.getValue();

            if (column == null || operator == null) continue;

            int colIndex = headers.indexOf(column);
            if (colIndex == -1 || colIndex >= row.size()) return false;

            String rowValue = row.get(colIndex).trim();

            boolean matches = false;
            switch (operator) {
                case "IsEmpty":
                    matches = rowValue.isEmpty();
                    break;
                case "IsFull":
                    matches = !rowValue.isEmpty();
                    break;
                case "Equals":
                    String equalsValue = Optional.ofNullable(valueCombo.getEditor().getText()).orElse("").trim();
                    if (equalsValue.isEmpty()) {
                        equalsValue = Optional.ofNullable(valueField.getText()).orElse("").trim();
                    }

                    matches = rowValue.equals(equalsValue);
                    break;
                case "Has":
                    String hasValue = Optional.ofNullable(valueCombo.getEditor().getText()).orElse("").trim();
                    if (hasValue.isEmpty()) {
                        hasValue = Optional.ofNullable(valueField.getText()).orElse("").trim();
                    }

                    matches = rowValue.toLowerCase().contains(hasValue.toLowerCase());
                    break;

                case "LessThan":
                    matches = compareNumeric(rowValue, valueField.getText(), "LessThan");
                    break;
                case "GreaterThan":
                    matches = compareNumeric(rowValue, valueField.getText(), "GreaterThan");
                    break;
                case "LessOrEqual":
                    matches = compareNumeric(rowValue, valueField.getText(), "LessOrEqual");
                    break;
                case "GreaterOrEqual":
                    matches = compareNumeric(rowValue, valueField.getText(), "GreaterOrEqual");
                    break;
            }

            if (!matches) {
                return false;
            }
        }
        return true;
    }

    private boolean compareNumeric(String rowValue, String compareValue, String operator) {
        try {
            double rowNum = Double.parseDouble(rowValue);
            double compNum = Double.parseDouble(compareValue);

            switch (operator) {
                case "LessThan": return rowNum < compNum;
                case "GreaterThan": return rowNum > compNum;
                case "LessOrEqual": return rowNum <= compNum;
                case "GreaterOrEqual": return rowNum >= compNum;
                default: return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public VBox getCountSection() {
        return countSection;
    }

    public Button getCountButton() {
        return countButton;
    }
}