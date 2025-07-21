import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TableEditor {

    private Stage parentStage;
    private TableViewController tableController;
    private FilterManager filterManager;
    private List<String> headers;
    private List<ObservableList<String>> filteredRows;
    private List<String> newlyCreatedColumns;
    private Map<String, List<String>> newColumnData;

    // UI Components
    private Stage editorStage;
    private TextField columnNameField;
    private ToggleGroup operationTypeGroup;
    private RadioButton mathOperationRadio;
    private RadioButton conditionalOperationRadio;

    // Math Operation Components
    private VBox mathOperationPane;
    private VBox mathOperationsContainer;
    private List<MathOperationRow> mathOperations;
    private HBox Operations;

    private Map<String, Double> calculationResults;

    // Conditional Operation Components
    private VBox conditionalOperationPane;
    private ComboBox<String> conditionColumnCombo;
    private ComboBox<String> conditionOperatorCombo;
    private TextField conditionValueField;
    private TextField conditionValueField2; // For Between operator
    private Label conditionAndLabel;
    private TextField trueOutputField;
    private TextField falseOutputField;
    private ComboBox<String> trueColumnCombo;
    private ComboBox<String> falseColumnCombo;
    private ToggleGroup trueOutputGroup;
    private ToggleGroup falseOutputGroup;
    private HBox conditionRow1;
    private HBox conditionRow2;

    // Available math operators
    private final String[] MATH_OPERATORS = {"+", "-", "*", "/", "%", "^"};

    // Available condition operators (from FilterOperations)
    private final String[] CONDITION_OPERATORS = {
            "Equals", "IsEmpty", "IsFull", "Has",
            "LessThan", "LessOrEqual", "GreaterThan", "GreaterOrEqual", "Between"
    };

    public TableEditor(Stage parentStage, TableViewController tableController, FilterManager filterManager) {
        this.parentStage = parentStage;
        this.tableController = tableController;
        this.filterManager = filterManager;
        this.mathOperations = new ArrayList<>();
        this.newlyCreatedColumns = new ArrayList<>();
        this.newColumnData = new HashMap<>(); 
    }

    public void showEditor() {
        updateFilteredData();

        if (filteredRows.isEmpty()) {
            showAlert("No Data", "No data available to edit. Please load a file first.");
            return;
        }

        createEditorWindow();
        editorStage.show();
    }

    private void updateFilteredData() {
      
        List<String> allHeaders = tableController.getAllHeaders(); 
        List<ObservableList<String>> allRows = tableController.getAllRows(); 

        headers = new ArrayList<>(allHeaders);
        filteredRows = new ArrayList<>();

        for (ObservableList<String> row : allRows) {
            if (filterManager.rowMatchesFilters(row, allHeaders)) {
                filteredRows.add(row);
            }
        }

        if (!newlyCreatedColumns.isEmpty()) {
            for (String newColumn : newlyCreatedColumns) {
                if (!headers.contains(newColumn)) {
                    headers.add(newColumn);
                    List<String> columnData = newColumnData.get(newColumn);
                    for (int i = 0; i < filteredRows.size(); i++) {
                        ObservableList<String> row = filteredRows.get(i);
                        List<String> extendedRow = new ArrayList<>(row);

                        int originalRowIndex = findOriginalRowIndex(row, allRows);

                        if (columnData != null && originalRowIndex >= 0 && originalRowIndex < columnData.size()) {
                            extendedRow.add(columnData.get(originalRowIndex));
                        } else {
                            extendedRow.add("");
                        }

                        filteredRows.set(i, FXCollections.observableArrayList(extendedRow));
                    }
                }
            }
        }

        // Debug output
        System.out.println("DEBUG: Headers count: " + headers.size());
        System.out.println("DEBUG: Filtered rows count: " + filteredRows.size());
        if (!filteredRows.isEmpty()) {
            System.out.println("DEBUG: First row size: " + filteredRows.get(0).size());
            System.out.println("DEBUG: Row/Header size match: " + (filteredRows.get(0).size() == headers.size()));
        }
    }
    private int findOriginalRowIndex(ObservableList<String> filteredRow, List<ObservableList<String>> allRows) {
        for (int i = 0; i < allRows.size(); i++) {
            ObservableList<String> originalRow = allRows.get(i);
            if (rowsMatch(filteredRow, originalRow)) {
                return i;
            }
        }
        return -1;
    }
    private boolean rowsMatch(ObservableList<String> row1, ObservableList<String> row2) {
        int minSize = Math.min(row1.size(), row2.size());
        for (int i = 0; i < minSize; i++) {
            String val1 = row1.get(i) != null ? row1.get(i) : "";
            String val2 = row2.get(i) != null ? row2.get(i) : "";
            if (!val1.equals(val2)) {
                return false;
            }
        }
        return true;
    }
    private List<String> getAllAvailableColumns() {
        List<String> allColumns = new ArrayList<>(headers);
        // Remove duplicates that might have been added
        return new ArrayList<>(new HashSet<>(allColumns));
    }
    private void updateAllDropdowns() {
        List<String> availableColumns = getAllAvailableColumns();

        // Update conditional operation dropdowns
        if (conditionColumnCombo != null) {
            String selectedCondition = conditionColumnCombo.getValue();
            conditionColumnCombo.setItems(FXCollections.observableArrayList(availableColumns));
            if (availableColumns.contains(selectedCondition)) {
                conditionColumnCombo.setValue(selectedCondition);
            }
        }

        if (trueColumnCombo != null) {
            String selectedTrue = trueColumnCombo.getValue();
            trueColumnCombo.setItems(FXCollections.observableArrayList(availableColumns));
            if (availableColumns.contains(selectedTrue)) {
                trueColumnCombo.setValue(selectedTrue);
            }
        }

        if (falseColumnCombo != null) {
            String selectedFalse = falseColumnCombo.getValue();
            falseColumnCombo.setItems(FXCollections.observableArrayList(availableColumns));
            if (availableColumns.contains(selectedFalse)) {
                falseColumnCombo.setValue(selectedFalse);
            }
        }

        // Update math operation dropdowns
        updateAllMathOperationDropdowns();
    }


    private void createEditorWindow() {
        editorStage = new Stage();
        editorStage.initModality(Modality.WINDOW_MODAL);
        editorStage.initOwner(parentStage);
        editorStage.setTitle("🛠️ Table Editor - Add New Column");
        editorStage.setResizable(true);
        editorStage.setMinWidth(700);
        editorStage.setMinHeight(600);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(25));
        mainLayout.setStyle("-fx-background: linear-gradient(to bottom right, #000000, #1a0000, #8b0000);");

        // Header with warning
        VBox headerSection = createHeaderSection();

        // Column name input
        VBox columnNameSection = createColumnNameSection();

        // Operation type selection
        VBox operationTypeSection = createOperationTypeSection();

        // Operation configuration panes
        createMathOperationPane();
        createConditionalOperationPane();

        // Buttons
        HBox buttonSection = createButtonSection();

        // Scroll pane for main content
        VBox contentBox = new VBox(15);
        contentBox.getChildren().addAll(
                headerSection,
                columnNameSection,
                operationTypeSection,
                mathOperationPane,
                conditionalOperationPane,
                buttonSection
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainLayout.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(mainLayout, 750, 700);
        editorStage.setScene(scene);
    }

    private VBox createHeaderSection() {
        VBox headerSection = new VBox(10);
        headerSection.setAlignment(Pos.CENTER);

        Text title = new Text("🛠️ Table Editor");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(220, 20, 60, 0.8));
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        shadow.setRadius(5);
        title.setEffect(shadow);

        // Warning message
        Text warningText = new Text("⚠️ IMPORTANT: This editor works with the currently filtered table data!\n" +
                "(" + filteredRows.size() + " rows, " + headers.size() + " columns)");
        warningText.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        warningText.setFill(Color.rgb(255, 204, 0)); // Orange color for warning
        warningText.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        headerSection.getChildren().addAll(title, warningText);
        return headerSection;
    }

    private VBox createColumnNameSection() {
        VBox section = new VBox(8);
        section.setStyle(createSectionStyle());

        Text label = new Text("📝 New Column Name:");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setFill(Color.WHITE);

        columnNameField = new TextField();
        columnNameField.setPromptText("Enter the name for your new column...");
        columnNameField.setStyle(createTextFieldStyle());
        columnNameField.setPrefHeight(35);

        section.getChildren().addAll(label, columnNameField);
        return section;
    }

    private VBox createOperationTypeSection() {
        VBox section = new VBox(10);
        section.setStyle(createSectionStyle());

        Text label = new Text("🔧 Operation Type:");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setFill(Color.WHITE);

        operationTypeGroup = new ToggleGroup();

        mathOperationRadio = new RadioButton("➕ Math Operation (e.g., Column1 + Column2 - Column3)");
        mathOperationRadio.setToggleGroup(operationTypeGroup);
        mathOperationRadio.setSelected(true);
        mathOperationRadio.setTextFill(Color.WHITE);
        mathOperationRadio.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        conditionalOperationRadio = new RadioButton("❓ Check Condition (if/then/else logic)");
        conditionalOperationRadio.setToggleGroup(operationTypeGroup);
        conditionalOperationRadio.setTextFill(Color.WHITE);
        conditionalOperationRadio.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        // Add listeners to show/hide appropriate panes
        operationTypeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean isMath = newToggle == mathOperationRadio;
            mathOperationPane.setVisible(isMath);
            mathOperationPane.setManaged(isMath);
            conditionalOperationPane.setVisible(!isMath);
            conditionalOperationPane.setManaged(!isMath);
        });

        section.getChildren().addAll(label, mathOperationRadio, conditionalOperationRadio);
        return section;
    }

    private void createMathOperationPane() {
        mathOperationPane = new VBox(15);
        mathOperationPane.setStyle(createSectionStyle());

        Text label = new Text("➕ Math Operations:");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setFill(Color.WHITE);

        Text instructions = new Text("Build your formula by adding operations. Example: Column1 + Column2 - Column3");
        instructions.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        instructions.setFill(Color.rgb(200, 200, 200));

        mathOperationsContainer = new VBox(10);
        Operations = new HBox(10);
        Button Addmath = createStyledButton(("...") ,"27ae60");
        Button addOperationButton = createStyledButton("➕ Add Operation", "#27ae60");
        addOperationButton.setOnAction(e -> addMathOperation());

        mathOperationPane.getChildren().addAll(label, instructions, mathOperationsContainer, addOperationButton);

        // Add initial operation
        addMathOperation();
    }

    private void createConditionalOperationPane() {
        conditionalOperationPane = new VBox(15);
        conditionalOperationPane.setStyle(createSectionStyle());
        conditionalOperationPane.setVisible(false);
        conditionalOperationPane.setManaged(false);

        Text label = new Text("❓ Conditional Operation:");
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setFill(Color.WHITE);

        // Condition setup
        VBox conditionBox = new VBox(8);
        Text conditionLabel = new Text("Condition:");
        conditionLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        conditionLabel.setFill(Color.WHITE);

        conditionRow1 = new HBox(10);
        conditionColumnCombo = new ComboBox<>();
        conditionColumnCombo.setPromptText("Select column...");
        conditionColumnCombo.setStyle(createComboBoxStyle());
        conditionColumnCombo.setItems(FXCollections.observableArrayList(getAllAvailableColumns()));

        conditionOperatorCombo = new ComboBox<>();
        conditionOperatorCombo.setPromptText("Select operator...");
        conditionOperatorCombo.setStyle(createComboBoxStyle());
        conditionOperatorCombo.setItems(FXCollections.observableArrayList(CONDITION_OPERATORS));

        conditionValueField = new TextField();
        conditionValueField.setPromptText("Enter value...");
        conditionValueField.setStyle(createTextFieldStyle());

        conditionRow1.getChildren().addAll(conditionColumnCombo, conditionOperatorCombo, conditionValueField);

        // Second value field for "Between" operator (initially hidden)
        conditionRow2 = new HBox(10);

        conditionAndLabel = new Label("and");
        conditionAndLabel.setTextFill(Color.WHITE);
        conditionAndLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 12));

        conditionValueField2 = new TextField();
        conditionValueField2.setPromptText("Enter second value...");
        conditionValueField2.setStyle(createTextFieldStyle());

        conditionRow2.getChildren().addAll(conditionAndLabel, conditionValueField2);
        conditionRow2.setVisible(false);
        conditionRow2.setManaged(false);

        // Add listener to show/hide second value field for "Between" operator
        conditionOperatorCombo.setOnAction(e -> {updateConditionValueFields();
            boolean isBetween = "Between".equals(conditionOperatorCombo.getValue());
            conditionRow2.setVisible(isBetween);
            conditionRow2.setManaged(isBetween);
        });

        conditionBox.getChildren().addAll(conditionLabel, conditionRow1, conditionRow2);

        // Output configuration
        VBox outputBox = createOutputConfiguration();

        conditionalOperationPane.getChildren().addAll(label, conditionBox, outputBox);
    }
    private void updateConditionValueFields() {
        String operator = conditionOperatorCombo.getValue();
        if (operator != null) {
            // Hide/show first value field
            boolean needsValue1 = !("IsEmpty".equals(operator) || "IsFull".equals(operator));
            conditionValueField.setVisible(needsValue1);
            conditionValueField.setManaged(needsValue1);

            // Hide/show second value field (only for Between)
            boolean needsValue2 = "Between".equals(operator);
            conditionRow2.setVisible(needsValue2);
            conditionRow2.setManaged(needsValue2);

            // Clear fields when hidden
            if (!needsValue1) {
                conditionValueField.clear();
            }
            if (!needsValue2) {
                conditionValueField2.clear();
            }
        }
    }
    private VBox createOutputConfiguration() {
        VBox outputBox = new VBox(15);

        Text outputLabel = new Text("Output Configuration:");
        outputLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        outputLabel.setFill(Color.WHITE);

        // True output
        VBox trueBox = new VBox(5);
        Text trueLabel = new Text("If condition is TRUE, output:");
        trueLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        trueLabel.setFill(Color.rgb(144, 238, 144)); // Light green

        trueOutputGroup = new ToggleGroup();
        RadioButton trueTextRadio = new RadioButton("Text value:");
        RadioButton trueColumnRadio = new RadioButton("Value from column:");
        trueTextRadio.setToggleGroup(trueOutputGroup);
        trueColumnRadio.setToggleGroup(trueOutputGroup);
        trueTextRadio.setSelected(true);
        trueTextRadio.setTextFill(Color.WHITE);
        trueColumnRadio.setTextFill(Color.WHITE);

        trueOutputField = new TextField();
        trueOutputField.setPromptText("Enter text value...");
        trueOutputField.setStyle(createTextFieldStyle());

        trueColumnCombo = new ComboBox<>();
        trueColumnCombo.setPromptText("Select column...");
        trueColumnCombo.setStyle(createComboBoxStyle());
        trueColumnCombo.setItems(FXCollections.observableArrayList(getAllAvailableColumns())); // Changed this line
        trueColumnCombo.setVisible(false);
        trueColumnCombo.setManaged(false);

        trueOutputGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean isText = newToggle == trueTextRadio;
            trueOutputField.setVisible(isText);
            trueOutputField.setManaged(isText);
            trueColumnCombo.setVisible(!isText);
            trueColumnCombo.setManaged(!isText);
        });

        trueBox.getChildren().addAll(trueLabel, trueTextRadio, trueOutputField, trueColumnRadio, trueColumnCombo);

        // False output (similar changes)
        VBox falseBox = new VBox(5);
        Text falseLabel = new Text("If condition is FALSE, output:");
        falseLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 11));
        falseLabel.setFill(Color.rgb(255, 182, 193)); // Light pink

        falseOutputGroup = new ToggleGroup();
        RadioButton falseTextRadio = new RadioButton("Text value:");
        RadioButton falseColumnRadio = new RadioButton("Value from column:");
        falseTextRadio.setToggleGroup(falseOutputGroup);
        falseColumnRadio.setToggleGroup(falseOutputGroup);
        falseTextRadio.setSelected(true);
        falseTextRadio.setTextFill(Color.WHITE);
        falseColumnRadio.setTextFill(Color.WHITE);

        falseOutputField = new TextField();
        falseOutputField.setPromptText("Enter text value...");
        falseOutputField.setStyle(createTextFieldStyle());

        falseColumnCombo = new ComboBox<>();
        falseColumnCombo.setPromptText("Select column...");
        falseColumnCombo.setStyle(createComboBoxStyle());
        falseColumnCombo.setItems(FXCollections.observableArrayList(getAllAvailableColumns())); // Changed this line
        falseColumnCombo.setVisible(false);
        falseColumnCombo.setManaged(false);

        falseOutputGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            boolean isText = newToggle == falseTextRadio;
            falseOutputField.setVisible(isText);
            falseOutputField.setManaged(isText);
            falseColumnCombo.setVisible(!isText);
            falseColumnCombo.setManaged(!isText);
        });

        falseBox.getChildren().addAll(falseLabel, falseTextRadio, falseOutputField, falseColumnRadio, falseColumnCombo);

        outputBox.getChildren().addAll(outputLabel, trueBox, falseBox);
        return outputBox;
    }
    private void addMathOperation() {
        List<String> previousResults = new ArrayList<>();

        // Collect names of previous operations
        for (int i = 0; i < mathOperations.size(); i++) {
            String name = mathOperations.get(i).getOperationName();
            if (name != null && !name.trim().isEmpty()) {
                previousResults.add(name);
            }
        }

        MathOperationRow row = new MathOperationRow(getAllAvailableColumns(), previousResults); // Changed this line
        row.setOperationLabel((mathOperations.size() + 1) + ":");
        row.setRemoveAction(() -> removeMathOperation(row));

        // Add listener to update other rows when this operation name changes
        row.getOperationNameField().textProperty().addListener((obs, oldVal, newVal) -> {
            updateAllMathOperationDropdowns();
        });

        mathOperations.add(row);
        mathOperationsContainer.getChildren().add(row.getRowPane());
    }
    private void removeMathOperation(MathOperationRow row) {
        mathOperations.remove(row);
        mathOperationsContainer.getChildren().remove(row.getRowPane());

        // Update operation labels
        for (int i = 0; i < mathOperations.size(); i++) {
            mathOperations.get(i).setOperationLabel((i + 1) + ":");
        }

        // Update all dropdowns to reflect the change
        updateAllMathOperationDropdowns();

        // Ensure at least one operation remains
        if (mathOperations.isEmpty()) {
            addMathOperation();
        }
    }
    private void updateAllMathOperationDropdowns() {
        List<String> availableColumns = getAllAvailableColumns(); // Use new method

        for (int i = 0; i < mathOperations.size(); i++) {
            List<String> previousResults = new ArrayList<>();

            // Only include results from operations BEFORE this one
            for (int j = 0; j < i; j++) {
                String name = mathOperations.get(j).getOperationName();
                if (name != null && !name.trim().isEmpty()) {
                    previousResults.add(name);
                }
            }

            mathOperations.get(i).updateAvailableValues(availableColumns, previousResults); // Pass availableColumns instead of headers
        }
    }

    private HBox createButtonSection() {
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));

        Button createButton = createStyledButton("✅ Create Column", "#27ae60");
        createButton.setPrefWidth(150);
        createButton.setOnAction(e -> createNewColumn());

        Button previewButton = createStyledButton("👁️ Preview", "#3498db");
        previewButton.setPrefWidth(150);
        previewButton.setOnAction(e -> previewColumn());

        Button cancelButton = createStyledButton("❌ Cancel", "#e74c3c");
        cancelButton.setPrefWidth(150);
        cancelButton.setOnAction(e -> editorStage.close());

        buttonBox.getChildren().addAll(previewButton, createButton, cancelButton);
        return buttonBox;
    }

    private void createNewColumn() {
        String columnName = columnNameField.getText().trim();
        if (columnName.isEmpty()) {
            showAlert("Error", "Please enter a column name.");
            return;
        }

        if (headers.contains(columnName)) {
            showAlert("Error", "Column name already exists. Please choose a different name.");
            return;
        }

        try {
            // Generate data for ALL rows (not just filtered ones)
            List<ObservableList<String>> allRows = tableController.getAllRows();
            List<String> allHeaders = tableController.getAllHeaders();

            List<String> newColumnDataForAllRows = new ArrayList<>();

            for (ObservableList<String> row : allRows) {
                try {
                    // Temporarily set the current row context for calculations
                    String cellValue = generateCellValue(row, allHeaders);
                    newColumnDataForAllRows.add(cellValue);
                } catch (Exception ex) {
                    newColumnDataForAllRows.add("ERROR");
                    System.err.println("Error generating value for row: " + ex.getMessage());
                }
            }

            // Store the new column data for this session
            this.newlyCreatedColumns.add(columnName);
            this.newColumnData.put(columnName, new ArrayList<>(newColumnDataForAllRows));

            // Add the new column to the table controller
            tableController.addNewColumn(columnName, newColumnDataForAllRows);

            // Update local data and all dropdowns
            updateFilteredData();
            updateAllDropdowns();

            showAlert("Success", "Column '" + columnName + "' has been added successfully!\n" +
                    "You can now use it in other operations within this editor session.");

            // Clear the column name field for next operation
            columnNameField.clear();

        } catch (Exception ex) {
            showAlert("Error", "Failed to create column: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private String generateCellValue(ObservableList<String> row, List<String> rowHeaders) throws Exception {
        if (mathOperationRadio.isSelected()) {
            return String.valueOf(calculateEnhancedMathResultForRow(row, rowHeaders));
        } else {
            return generateConditionalCellValue(row, rowHeaders);
        }
    }

    private double calculateEnhancedMathResultForRow(ObservableList<String> row, List<String> rowHeaders) throws Exception {
        if (mathOperations.isEmpty()) {
            throw new Exception("No operations defined");
        }

        Map<String, Double> rowCalculationResults = new HashMap<>();
        double finalResult = 0;

        // Execute operations in sequence
        for (MathOperationRow operation : mathOperations) {
            String operationName = operation.getOperationName();
            String column1Name = operation.getColumn1();
            String operator = operation.getOperator();
            String column2Name = operation.getColumn2();

            if (column1Name == null || operator == null) {
                throw new Exception("Incomplete operation: " + operationName);
            }

            // Get first value
            double value1 = getValueForRow(row, rowHeaders, column1Name, rowCalculationResults);

            // Get second value (if needed)
            double value2 = 0;
            if (column2Name != null && !column2Name.trim().isEmpty()) {
                value2 = getValueForRow(row, rowHeaders, column2Name, rowCalculationResults);
            }

            // Perform calculation
            double result = performMathOperation(value1, value2, operator);

            // Store result for future operations
            rowCalculationResults.put(operationName, result);
            finalResult = result;
        }

        return finalResult;
    }

    // Helper method to get value from specific row context
    private double getValueForRow(ObservableList<String> row, List<String> rowHeaders, String valueName, Map<String, Double> calculationResults) throws Exception {
        // Check if it's a previous calculation result
        if (calculationResults.containsKey(valueName)) {
            return calculationResults.get(valueName);
        }

        // Otherwise, it's a column value
        return getNumericValueFromRow(row, rowHeaders, valueName);
    }


    private double getNumericValueFromRow(ObservableList<String> row, List<String> rowHeaders, String columnName) throws Exception {
        int columnIndex = rowHeaders.indexOf(columnName);

        if (columnIndex == -1) {
            throw new Exception("Column not found: " + columnName + ". Available columns: " + rowHeaders);
        }

        if (columnIndex >= row.size()) {
            throw new Exception("Column index " + columnIndex + " is out of bounds for row size " + row.size());
        }

        String value = row.get(columnIndex);
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null")) {
            return 0.0;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new Exception("Non-numeric value in column " + columnName + ": '" + value + "'");
        }
    }

    // Enhanced conditional generation for specific row
    private String generateConditionalCellValue(ObservableList<String> row, List<String> rowHeaders) throws Exception {
        String column = conditionColumnCombo.getValue();
        String operator = conditionOperatorCombo.getValue();
        String value1 = conditionValueField.getText().trim();
        String value2 = conditionValueField2.getText().trim();

        if (column == null || operator == null) {
            throw new Exception("Please select both column and operator for the condition.");
        }

        // Find column index in the provided headers
        int columnIndex = rowHeaders.indexOf(column);
        if (columnIndex == -1) {
            throw new Exception("Column '" + column + "' not found in headers: " + rowHeaders);
        }

        if (columnIndex >= row.size()) {
            throw new Exception("Column index out of bounds");
        }

        String rowValue = row.get(columnIndex);
        if (rowValue == null || rowValue.trim().equalsIgnoreCase("null")) {
            rowValue = "";
        } else {
            rowValue = rowValue.trim();
        }

        boolean conditionResult = FilterOperations.applyOperator(operator, rowValue, value1, value2);
        return getConditionalOutputForRow(conditionResult, row, rowHeaders);
    }

    // Enhanced conditional output for specific row
    private String getConditionalOutputForRow(boolean conditionResult, ObservableList<String> row, List<String> rowHeaders) {
        try {
            if (conditionResult) {
                // True case
                RadioButton selectedTrue = (RadioButton) trueOutputGroup.getSelectedToggle();
                if (selectedTrue != null && selectedTrue.getText().startsWith("Text value:")) {
                    return trueOutputField.getText() != null ? trueOutputField.getText() : "";
                } else {
                    // Column value
                    String columnName = trueColumnCombo.getValue();
                    if (columnName != null) {
                        int columnIndex = rowHeaders.indexOf(columnName);
                        if (columnIndex >= 0 && columnIndex < row.size()) {
                            String cellValue = row.get(columnIndex);
                            return cellValue != null ? cellValue : "";
                        }
                    }
                }
            } else {
                // False case
                RadioButton selectedFalse = (RadioButton) falseOutputGroup.getSelectedToggle();
                if (selectedFalse != null && selectedFalse.getText().startsWith("Text value:")) {
                    return falseOutputField.getText() != null ? falseOutputField.getText() : "";
                } else {
                    // Column value
                    String columnName = falseColumnCombo.getValue();
                    if (columnName != null) {
                        int columnIndex = rowHeaders.indexOf(columnName);
                        if (columnIndex >= 0 && columnIndex < row.size()) {
                            String cellValue = row.get(columnIndex);
                            return cellValue != null ? cellValue : "";
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("ERROR in getConditionalOutputForRow: " + ex.getMessage());
            return "ERROR";
        }

        return "";
    }

    private void previewColumn() {
        try {
            List<String> columnData = generateColumnData();
            if (columnData == null) return;

            // Show preview dialog
            showPreviewDialog(columnData);

        } catch (Exception ex) {
            showAlert("Error", "Preview failed: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private List<String> generateColumnData() {
        if (mathOperationRadio.isSelected()) {
            return generateMathColumnData();
        } else {
            return generateConditionalColumnData();
        }
    }

    private List<String> generateMathColumnData() {
        if (mathOperations.isEmpty()) {
            showAlert("Error", "Please add at least one math operation.");
            return null;
        }

        // Validate all operations
        for (MathOperationRow op : mathOperations) {
            if (!op.isValid()) {
                showAlert("Error", "Please complete all math operation fields.\nIncomplete operation: " + op.getOperationString());
                return null;
            }
        }

        // Check for duplicate operation names
        Set<String> operationNames = new HashSet<>();
        for (MathOperationRow op : mathOperations) {
            String name = op.getOperationName();
            if (operationNames.contains(name)) {
                showAlert("Error", "Duplicate operation name: " + name + "\nPlease use unique names for each operation.");
                return null;
            }
            operationNames.add(name);
        }

        List<String> result = new ArrayList<>();

        for (ObservableList<String> row : filteredRows) {
            try {
                double finalResult = calculateEnhancedMathResult(row);
                result.add(String.valueOf(finalResult));
            } catch (Exception ex) {
                result.add("ERROR");
            }
        }

        return result;
    }

    private double getValue(ObservableList<String> row, String valueName) throws Exception {
        if (calculationResults.containsKey(valueName)) {
            return calculationResults.get(valueName);
        }

        return getNumericValue(row, valueName);
    }

    private double calculateMathResult(ObservableList<String> row) throws Exception {
        if (mathOperations.isEmpty()) {
            throw new Exception("No operations defined");
        }

        // Start with first operation
        MathOperationRow firstOp = mathOperations.get(0);
        double result = getNumericValue(row, firstOp.getColumn1());

        if (firstOp.getColumn2() != null && !firstOp.getColumn2().isEmpty()) {
            double value2 = getNumericValue(row, firstOp.getColumn2());
            result = performMathOperation(result, value2, firstOp.getOperator());
        }


        // Apply remaining operations
        for (int i = 1; i < mathOperations.size(); i++) {
            MathOperationRow op = mathOperations.get(i);
            if (op.getColumn1() != null && !op.getColumn1().isEmpty()) {
                double value = getNumericValue(row, op.getColumn1());
                result = performMathOperation(result, value, op.getOperator());
            }
        }

        return result;
    }



    private double calculateEnhancedMathResult(ObservableList<String> row) throws Exception {
        if (mathOperations.isEmpty()) {
            throw new Exception("No operations defined");
        }

        calculationResults = new HashMap<>();
        double finalResult = 0;

        // Execute operations in sequence
        for (MathOperationRow operation : mathOperations) {
            String operationName = operation.getOperationName();
            String column1Name = operation.getColumn1();
            String operator = operation.getOperator();
            String column2Name = operation.getColumn2();

            if (column1Name == null || operator == null) {
                throw new Exception("Incomplete operation: " + operationName);
            }

            // Get first value
            double value1 = getValue(row, column1Name);

            // Get second value (if needed)
            double value2 = 0;
            if (column2Name != null && !column2Name.trim().isEmpty()) {
                value2 = getValue(row, column2Name);
            }

            // Perform calculation
            double result = performMathOperation(value1, value2, operator);

            // Store result for future operations
            calculationResults.put(operationName, result);
            finalResult = result; // The last operation's result is the final result
        }

        return finalResult;
    }


    private double getNumericValue(ObservableList<String> row, String columnName) throws Exception {
        int columnIndex = headers.indexOf(columnName);

        if (columnIndex == -1) {
            throw new Exception("Column not found in current view: " + columnName +
                    ". Available columns: " + headers);
        }

        if (columnIndex >= row.size()) {
            throw new Exception("Column index " + columnIndex + " is out of bounds for row size " + row.size() +
                    ". Column: " + columnName);
        }

        String value = row.get(columnIndex);
        if (value == null || value.trim().isEmpty() || value.trim().equalsIgnoreCase("null")) {
            return 0.0; // Default to 0 for empty values
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new Exception("Non-numeric value in column " + columnName + ": '" + value + "'");
        }
    }

    private double performMathOperation(double value1, double value2, String operator) throws Exception {
        switch (operator) {
            case "+":
                return value1 + value2;
            case "-":
                return value1 - value2;
            case "*":
                return value1 * value2;
            case "/":
                if (value2 == 0) throw new Exception("Division by zero");
                return value1 / value2;
            case "%":
                if (value2 == 0) throw new Exception("Modulo by zero");
                return value1 % value2;
            case "^":
                return Math.pow(value1, value2);
            default:
                throw new Exception("Unknown operator: " + operator);
        }
    }
    private List<String> generateConditionalColumnData() {
        String column = conditionColumnCombo.getValue();
        String operator = conditionOperatorCombo.getValue();
        String value1 = conditionValueField.getText().trim();
        String value2 = conditionValueField2.getText().trim();

        if (column == null || operator == null) {
            showAlert("Error", "Please select both column and operator for the condition.");
            return null;
        }

        // Validate required values based on operator
        boolean needsValue1 = !("IsEmpty".equals(operator) || "IsFull".equals(operator));
        if (needsValue1 && value1.isEmpty()) {
            showAlert("Error", "Please enter a value for the condition.");
            return null;
        }

        if ("Between".equals(operator) && value2.isEmpty()) {
            showAlert("Error", "Please enter both values for Between condition.");
            return null;
        }

        // Ensure we have the latest filtered data
        updateFilteredData();

        // Find column index in the CURRENT filtered headers
        int columnIndex = headers.indexOf(column);
        if (columnIndex == -1) {
            showAlert("Error", "Column '" + column + "' not found in current filtered view.\n" +
                    "Available columns: " + headers);
            return null;
        }

        List<String> result = new ArrayList<>();

        System.out.println("DEBUG Conditional: Column=" + column + ", Index=" + columnIndex +
                ", Operator=" + operator);
        System.out.println("DEBUG: Processing " + filteredRows.size() + " filtered rows");

        for (int i = 0; i < filteredRows.size(); i++) {
            ObservableList<String> row = filteredRows.get(i);

            // Validate row structure
            if (columnIndex >= row.size()) {
                System.err.println("ERROR: Column index " + columnIndex +
                        " out of bounds for row " + i + " (size: " + row.size() + ")");
                result.add("ERROR");
                continue;
            }

            String rowValue = row.get(columnIndex);
            if (rowValue == null || rowValue.trim().equalsIgnoreCase("null")) {
                rowValue = "";
            } else {
                rowValue = rowValue.trim();
            }

            try {
                boolean conditionResult = FilterOperations.applyOperator(operator, rowValue, value1, value2);
                String output = getConditionalOutput(conditionResult, row);
                result.add(output);

                // Debug first few rows
                if (i < 3) {
                    System.out.println("DEBUG Row " + i + ": Value='" + rowValue +
                            "', Condition=" + conditionResult + ", Output='" + output + "'");
                }
            } catch (Exception ex) {
                System.err.println("ERROR processing row " + i + ": " + ex.getMessage());
                result.add("ERROR");
            }
        }

        return result;
    }
    private void debugDataStructure() {
        System.out.println("=== DEBUG DATA STRUCTURE ===");
        System.out.println("Headers (" + headers.size() + "): " + headers);
        System.out.println("FilteredRows count: " + filteredRows.size());

        if (!filteredRows.isEmpty()) {
            System.out.println("First row size: " + filteredRows.get(0).size());
            System.out.println("First row content: " + filteredRows.get(0));

            // Print a sample of data for debugging
            for (int i = 0; i < Math.min(3, filteredRows.size()); i++) {
                ObservableList<String> row = filteredRows.get(i);
                System.out.println("Row " + i + " (" + row.size() + " cols): " + row);
            }
        }

        System.out.println("Newly created columns: " + newlyCreatedColumns);
        System.out.println("=== END DEBUG ===");
    }

    public void resetAfterPresetLoad() {
        this.newlyCreatedColumns.clear();
        this.newColumnData.clear();
        this.mathOperations.clear();

        updateFilteredData();
        updateAllDropdowns();
    }


    private String getConditionalOutput(boolean conditionResult, ObservableList<String> row) {
        try {
            if (conditionResult) {
                RadioButton selectedTrue = (RadioButton) trueOutputGroup.getSelectedToggle();
                if (selectedTrue != null && selectedTrue.getText().startsWith("Text value:")) {
                    return trueOutputField.getText() != null ? trueOutputField.getText() : "";
                } else {
                    String columnName = trueColumnCombo.getValue();
                    if (columnName != null) {
                        int columnIndex = headers.indexOf(columnName);
                        if (columnIndex >= 0 && columnIndex < row.size()) {
                            String cellValue = row.get(columnIndex);
                            return cellValue != null ? cellValue : "";
                        }
                    }
                }
            } else {
                RadioButton selectedFalse = (RadioButton) falseOutputGroup.getSelectedToggle();
                if (selectedFalse != null && selectedFalse.getText().startsWith("Text value:")) {
                    return falseOutputField.getText() != null ? falseOutputField.getText() : "";
                } else {
                    String columnName = falseColumnCombo.getValue();
                    if (columnName != null) {
                        int columnIndex = headers.indexOf(columnName);
                        if (columnIndex >= 0 && columnIndex < row.size()) {
                            String cellValue = row.get(columnIndex);
                            return cellValue != null ? cellValue : "";
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("ERROR in getConditionalOutput: " + ex.getMessage());
            return "ERROR";
        }

        return "";
    }

    private void showPreviewDialog(List<String> columnData) {
        Stage previewStage = new Stage();
        previewStage.initModality(Modality.WINDOW_MODAL);
        previewStage.initOwner(editorStage);
        previewStage.setTitle("👁️ Column Preview");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2c3e50;");

        Text title = new Text("Preview of '" + columnNameField.getText() + "'");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setFill(Color.WHITE);

        ListView<String> previewList = new ListView<>();
        previewList.setItems(FXCollections.observableArrayList(columnData.subList(0, Math.min(20, columnData.size()))));
        previewList.setPrefHeight(300);
        previewList.setStyle("-fx-background-color: white; -fx-text-fill: black;");

        Text info = new Text("Showing first " + Math.min(20, columnData.size()) + " of " + columnData.size() + " rows");
        info.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        info.setFill(Color.rgb(200, 200, 200));

        Button closeButton = createStyledButton("Close", "#95a5a6");
        closeButton.setOnAction(e -> previewStage.close());

        layout.getChildren().addAll(title, previewList, info, closeButton);

        Scene scene = new Scene(layout, 400, 450);
        previewStage.setScene(scene);
        previewStage.show();
    }

    // Helper methods for styling
    private String createSectionStyle() {
        return "-fx-background-color: rgba(0, 0, 0, 0.3); " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 15; " +
                "-fx-border-color: #dc143c; " +
                "-fx-border-radius: 8; " +
                "-fx-border-width: 1;";
    }

    private String createTextFieldStyle() {
        return "-fx-background-color: white; " +
                "-fx-text-fill: black; " +
                "-fx-border-color: #dc143c; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-font-size: 12px; " +
                "-fx-padding: 5;";
    }

    private String createComboBoxStyle() {
        return "-fx-background-color: white; " +
                "-fx-text-fill: black; " +
                "-fx-border-color: #dc143c; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 4; " +
                "-fx-font-size: 12px;";
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        button.setPrefHeight(35);
        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-radius: 6; " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 1;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: derive(" + color + ", 20%); " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-radius: 6; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 1; " +
                            "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.4), 8, 0, 0, 2);"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-radius: 6; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 1;"
            );
        });

        return button;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: black; " +
                        "-fx-font-family: Arial; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: red; " +
                        "-fx-border-width: 2;"
        );

        alert.showAndWait();
    }
    public VBox getEditorUI() {
        updateFilteredData();

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(25));
        mainLayout.setStyle("-fx-background: linear-gradient(to bottom right, #000000, #1a0000, #8b0000);");

        // Header with warning
        VBox headerSection = createHeaderSection();

        // Column name input
        VBox columnNameSection = createColumnNameSection();

        // Operation type selection
        VBox operationTypeSection = createOperationTypeSection();

        // Operation configuration panes
        createMathOperationPane();
        createConditionalOperationPane();

        // Buttons
        HBox buttonSection = createButtonSection();

        VBox contentBox = new VBox(15);
        contentBox.getChildren().addAll(
                headerSection,
                columnNameSection,
                operationTypeSection,
                mathOperationPane,
                conditionalOperationPane,
                buttonSection
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        mainLayout.getChildren().add(scrollPane);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return mainLayout;
    }

}
