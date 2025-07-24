import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;


public class MathOperationRow {

    private HBox rowPane;
    private TextField operationNameField;
    private ComboBox<String> column1Combo;
    private ComboBox<String> operatorCombo;
    private ComboBox<String> column2Combo;
    private Button removeButton;
    private Text operationLabel;

    private final String[] MATH_OPERATORS = {"+", "-", "*", "/", "%", "^"};

    public MathOperationRow() {
        createRowPane();
    }

    public MathOperationRow(List<String> headers, List<String> previousResults) {
        createRowPane();
        updateAvailableValues(headers, previousResults);
    }

    private void createRowPane() {
        rowPane = new HBox(8);
        rowPane.setPadding(new Insets(8));
        rowPane.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1); " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-color: rgba(220, 20, 60, 0.3); " +
                        "-fx-border-radius: 5; " +
                        "-fx-border-width: 1;"
        );

        operationLabel = new Text("Op:");
        operationLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 11));
        operationLabel.setFill(Color.WHITE);

        operationNameField = new TextField();
        operationNameField.setPromptText("Result name...");
        operationNameField.setStyle(createTextFieldStyle());
        operationNameField.setPrefWidth(100);

        column1Combo = new ComboBox<>();
        column1Combo.setPromptText("Value 1");
        column1Combo.setStyle(createComboBoxStyle());
        column1Combo.setPrefWidth(120);

        operatorCombo = new ComboBox<>();
        operatorCombo.setItems(FXCollections.observableArrayList(MATH_OPERATORS));
        operatorCombo.setPromptText("Op");
        operatorCombo.setStyle(createComboBoxStyle());
        operatorCombo.setPrefWidth(50);
        operatorCombo.setValue("+"); 

        column2Combo = new ComboBox<>();
        column2Combo.setPromptText("Value 2");
        column2Combo.setStyle(createComboBoxStyle());
        column2Combo.setPrefWidth(120);

        removeButton = new Button("🗑️");
        removeButton.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        removeButton.setPrefWidth(35);
        removeButton.setStyle(createRemoveButtonStyle());

        removeButton.setOnMouseEntered(e -> {
            removeButton.setStyle(createRemoveButtonHoverStyle());
        });

        removeButton.setOnMouseExited(e -> {
            removeButton.setStyle(createRemoveButtonStyle());
        });

        rowPane.getChildren().addAll(
                operationLabel,
                operationNameField,
                column1Combo,
                operatorCombo,
                column2Combo,
                removeButton
        );

        
        HBox.setHgrow(operationNameField, Priority.SOMETIMES);
        HBox.setHgrow(column1Combo, Priority.SOMETIMES);
        HBox.setHgrow(column2Combo, Priority.SOMETIMES);
    }

    public void updateAvailableValues(List<String> headers, List<String> previousResults) {
        String selected1 = column1Combo.getValue();
        String selected2 = column2Combo.getValue();

        List<String> allValues = new ArrayList<>();

        if (!headers.isEmpty()) {
            allValues.add("--- Columns ---");
            allValues.addAll(headers);
        }

        if (!previousResults.isEmpty()) {
            allValues.add("--- Previous Results ---");
            allValues.addAll(previousResults);
        }

        column1Combo.setItems(FXCollections.observableArrayList(allValues));
        column2Combo.setItems(FXCollections.observableArrayList(allValues));

        if (selected1 != null && allValues.contains(selected1)) {
            column1Combo.setValue(selected1);
        }
        if (selected2 != null && allValues.contains(selected2)) {
            column2Combo.setValue(selected2);
        }
    }

    public void updateHeaders(List<String> headers) {
        updateAvailableValues(headers, new ArrayList<>());
    }

    public void setOperationLabel(String label) {
        operationLabel.setText(label);
        if (operationNameField.getText().trim().isEmpty()) {
            operationNameField.setText("Result_" + label.replace(":", ""));
        }
    }

    public void setRemoveAction(Runnable action) {
        removeButton.setOnAction(e -> action.run());
    }

    public HBox getRowPane() {
        return rowPane;
    }

    public String getOperationName() {
        return operationNameField.getText().trim();
    }

    public String getColumn1() {
        String value = column1Combo.getValue();
        return (value != null && !value.startsWith("---")) ? value : null;
    }

    public String getColumn2() {
        String value = column2Combo.getValue();
        return (value != null && !value.startsWith("---")) ? value : null;
    }

    public String getOperator() {
        return operatorCombo.getValue();
    }

    public ComboBox<String> getColumn1Combo() {
        return column1Combo;
    }

    public ComboBox<String> getColumn2Combo() {
        return column2Combo;
    }

    public ComboBox<String> getOperatorCombo() {
        return operatorCombo;
    }

    public TextField getOperationNameField() {
        return operationNameField;
    }

    public void setOperationName(String name) {
        operationNameField.setText(name);
    }

    public void setColumn1(String column) {
        column1Combo.setValue(column);
    }

    public void setColumn2(String column) {
        column2Combo.setValue(column);
    }

    public void setOperator(String operator) {
        operatorCombo.setValue(operator);
    }

    public boolean isValid() {
        return getOperationName() != null && !getOperationName().isEmpty() &&
                getColumn1() != null && !getColumn1().trim().isEmpty() &&
                getOperator() != null && !getOperator().trim().isEmpty();
    }

    public String getOperationString() {
        String name = getOperationName();
        String col1 = getColumn1();
        String op = getOperator();
        String col2 = getColumn2();

        if (name == null || col1 == null || op == null) {
            return "Invalid Operation";
        }

        String operation = col1 + " " + op;
        if (col2 != null && !col2.trim().isEmpty()) {
            operation += " " + col2;
        }

        return name + " = " + operation;
    }

    private String createTextFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-text-fill: black; " +
                "-fx-border-color: #dc143c; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 3; " +
                "-fx-font-size: 11px; " +
                "-fx-padding: 3;";
    }

    private String createComboBoxStyle() {
        return "-fx-background-color: white; " +
                "-fx-text-fill: black; " +
                "-fx-border-color: #dc143c; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 3; " +
                "-fx-font-size: 11px;";
    }

    private String createRemoveButtonStyle() {
        return "-fx-background-color: #e74c3c; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 4; " +
                "-fx-border-radius: 4; " +
                "-fx-border-color: white; " +
                "-fx-border-width: 1; " +
                "-fx-font-size: 12px;";
    }

    private String createRemoveButtonHoverStyle() {
        return "-fx-background-color: #c0392b; " +
                "-fx-text-fill: white; " +
                "-fx-background-radius: 4; " +
                "-fx-border-radius: 4; " +
                "-fx-border-color: white; " +
                "-fx-border-width: 1; " +
                "-fx-font-size: 12px; " +
                "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.6), 6, 0, 0, 2);";
    }
}
