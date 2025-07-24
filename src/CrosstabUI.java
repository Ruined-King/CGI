import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Popup;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CrosstabUI {
    private VBox crosstabSection;
    private TextField xColumnField;
    private TextField yColumnField;
    private Popup xColumnPopup;
    private Popup yColumnPopup;
    private ListView<String> xColumnListView;
    private ListView<String> yColumnListView;
    private CheckBox applyFiltersCheckBox;
    private CheckBox monthlyConversionCheckBox;
    private CheckBox totalOnlyCheckBox;
    private Button generateButton;
    private Button showChartsButton;
    private Button switchColumnsButton;
    private TableView<ObservableList<String>> crosstabTable;
    private ScrollPane tableScrollPane;
    private Text statusText;
    private CheckBox includeAllMonthsCheckBox;

    private CrosstabData crosstabData;
    private CrosstabTable crosstabTableManager;
    private CrosstabCharts crosstabCharts;
    private FilterManager filterManager;

    private List<String> headers;
    private List<ObservableList<String>> allRows;

    private ObservableList<String> originalItems;

    public CrosstabUI() {
        crosstabData = new CrosstabData();
        crosstabCharts = new CrosstabCharts(crosstabData);
        initializeUI();
    }

    private void initializeUI() {
        crosstabSection = new VBox(20);
        crosstabSection.setPadding(new Insets(20));
        crosstabSection.setStyle("-fx-background-color: #1a1a1a;");

        VBox controlPanel = createControlPanel();

        VBox tableSection = createTableSection();

        crosstabSection.getChildren().addAll(controlPanel, tableSection);
        VBox.setVgrow(tableSection, Priority.ALWAYS);
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(15);
        controlPanel.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 20; " +
                        "-fx-border-color: #dc143c; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.3), 10, 0, 0, 2);"
        );

        Text title = new Text("📈 Crosstab Analysis Configuration");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setFill(Color.WHITE);

        HBox columnSelectionBox = createColumnSelectionBox();

        VBox optionsBox = createOptionsBox();

        HBox buttonsBox = createButtonsBox();

        statusText = new Text("Select columns and click Generate to create crosstab");
        statusText.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        statusText.setFill(Color.rgb(255, 255, 255, 0.8));

        controlPanel.getChildren().addAll(
                title,
                columnSelectionBox,
                optionsBox,
                buttonsBox,
                statusText
        );

        return controlPanel;
    }

    private HBox createColumnSelectionBox() {
        HBox columnBox = new HBox(20);
        columnBox.setAlignment(Pos.CENTER_LEFT);

        VBox xColumnBox = new VBox(5);
        Text xLabel = new Text("X-Axis Column (Rows):");
        xLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        xLabel.setFill(Color.WHITE);

        xColumnField = new TextField();
        xColumnField.setPrefWidth(200);
        xColumnField.setPromptText("Type to search X column...");
        styleTextField(xColumnField);
        setupCustomAutocomplete(xColumnField, true);

        xColumnBox.getChildren().addAll(xLabel, xColumnField);

        
        switchColumnsButton = new Button("⇄");
        switchColumnsButton.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        switchColumnsButton.setPrefWidth(40);
        switchColumnsButton.setPrefHeight(35);
        switchColumnsButton.setStyle(
                "-fx-background-color: #17a2b8; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-border-radius: 20;"
        );
        switchColumnsButton.setOnAction(e -> switchColumns());
        switchColumnsButton.setTooltip(new Tooltip("Switch X and Y columns"));

        
        VBox yColumnBox = new VBox(5);
        Text yLabel = new Text("Y-Axis Column (Columns):");
        yLabel.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        yLabel.setFill(Color.WHITE);

        yColumnField = new TextField();
        yColumnField.setPrefWidth(200);
        yColumnField.setPromptText("Type to search Y column...");
        styleTextField(yColumnField);
        setupCustomAutocomplete(yColumnField, false); 

        yColumnBox.getChildren().addAll(yLabel, yColumnField);

        columnBox.getChildren().addAll(xColumnBox, switchColumnsButton, yColumnBox);
        return columnBox;
    }

    private void setupCustomAutocomplete(TextField textField, boolean isXColumn) {
        
        Popup popup = new Popup();
        ListView<String> listView = new ListView<>();
        listView.setPrefWidth(textField.getPrefWidth());
        listView.setMaxHeight(150);

        
        listView.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d3d3d3; " +
                        "-fx-border-width: 1px;"
        );

        listView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: black; -fx-padding: 4px;");
                }
            }
        });

        popup.getContent().add(listView);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        
        if (isXColumn) {
            xColumnPopup = popup;
            xColumnListView = listView;
        } else {
            yColumnPopup = popup;
            yColumnListView = listView;
        }

        
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (originalItems == null || originalItems.isEmpty()) {
                return;
            }

            if (newVal == null || newVal.trim().isEmpty()) {
                popup.hide();
                return;
            }

            
            String searchText = newVal.toLowerCase().trim();
            String[] searchWords = searchText.split("\\s+");

            List<String> filtered = originalItems.stream()
                    .filter(item -> {
                        String itemLower = item.toLowerCase();
                        
                        for (String word : searchWords) {
                            if (!itemLower.contains(word)) {
                                return false;
                            }
                        }
                        return true;
                    })
                    .collect(Collectors.toList());

            Platform.runLater(() -> {
                listView.setItems(FXCollections.observableArrayList(filtered));

                if (!filtered.isEmpty()) {
                    
                    if (!popup.isShowing()) {
                        popup.show(textField,
                                textField.localToScreen(textField.getBoundsInLocal()).getMinX(),
                                textField.localToScreen(textField.getBoundsInLocal()).getMaxY());
                    }
                } else {
                    popup.hide();
                }
            });
        });

        
        listView.setOnMouseClicked(e -> {
            String selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                textField.setText(selected);
                popup.hide();
            }
        });

        
        textField.setOnKeyPressed(e -> {
            if (popup.isShowing()) {
                switch (e.getCode()) {
                    case DOWN:
                        listView.requestFocus();
                        listView.getSelectionModel().selectFirst();
                        e.consume();
                        break;
                    case UP:
                        listView.requestFocus();
                        listView.getSelectionModel().selectLast();
                        e.consume();
                        break;
                    case ENTER:
                        String selected = listView.getSelectionModel().getSelectedItem();
                        if (selected != null) {
                            textField.setText(selected);
                            popup.hide();
                        }
                        e.consume();
                        break;
                    case ESCAPE:
                        popup.hide();
                        e.consume();
                        break;
                }
            } else {
                switch (e.getCode()) {
                    case DOWN:
                        
                        if (!textField.getText().trim().isEmpty()) {
                            String currentText = textField.getText();
                            textField.setText(currentText); 

                        }
                        e.consume();
                        break;
                }
            }
        });

        
        listView.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER:
                    String selected = listView.getSelectionModel().getSelectedItem();
                    if (selected != null) {
                        textField.setText(selected);
                        popup.hide();
                        textField.requestFocus();
                    }
                    e.consume();
                    break;
                case ESCAPE:
                    popup.hide();
                    textField.requestFocus();
                    e.consume();
                    break;
            }
        });

        
        textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                
                Platform.runLater(() -> {
                    if (!listView.isFocused()) {
                        popup.hide();
                    }
                });
            }
        });

        listView.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused && !textField.isFocused()) {
                popup.hide();
            }
        });
    }

    private void styleTextField(TextField textField) {
        textField.setStyle(
                "-fx-background-color: #2c3e50; " +
                        "-fx-text-fill: white; " +
                        "-fx-prompt-text-fill: white; " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-color: #34495e; " +
                        "-fx-border-radius: 5; " +
                        "-fx-padding: 8px;"
        );
    }

    private VBox createOptionsBox() {
        VBox optionsBox = new VBox(10);
        optionsBox.setAlignment(Pos.CENTER_LEFT);

        HBox optionsRow = new HBox(30);
        optionsRow.setAlignment(Pos.CENTER_LEFT);

        
        applyFiltersCheckBox = new CheckBox("Apply Active Filters");
        applyFiltersCheckBox.setTextFill(Color.WHITE);
        applyFiltersCheckBox.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        applyFiltersCheckBox.setSelected(true);

        
        monthlyConversionCheckBox = new CheckBox("Convert Dates to Month-Year");
        monthlyConversionCheckBox.setTextFill(Color.WHITE);
        monthlyConversionCheckBox.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        monthlyConversionCheckBox.setSelected(false);

        
        totalOnlyCheckBox = new CheckBox("Charts: Show Only Totals");
        totalOnlyCheckBox.setTextFill(Color.WHITE);
        totalOnlyCheckBox.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        totalOnlyCheckBox.setSelected(false);
        totalOnlyCheckBox.setTooltip(new Tooltip("When enabled, charts will only show total values instead of individual categories"));

        
        includeAllMonthsCheckBox = new CheckBox("Charts: Include All Months in Present Years");
        includeAllMonthsCheckBox.setTextFill(Color.WHITE);
        includeAllMonthsCheckBox.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        includeAllMonthsCheckBox.setSelected(false);
        includeAllMonthsCheckBox.setTooltip(new Tooltip("Include all 12 months for each year present in the data"));

        optionsRow.getChildren().addAll(applyFiltersCheckBox, monthlyConversionCheckBox, totalOnlyCheckBox);

        
        styleCheckBox(applyFiltersCheckBox);
        styleCheckBox(monthlyConversionCheckBox);
        styleCheckBox(totalOnlyCheckBox);
        styleCheckBox(includeAllMonthsCheckBox);

        optionsBox.getChildren().add(optionsRow);
        return optionsBox;
    }

    private void styleCheckBox(CheckBox checkBox) {
        checkBox.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-mark-color: #dc143c; " +
                        "-fx-mark-highlight-color: #dc143c;"
        );
    }

    private HBox createButtonsBox() {
        HBox buttonsBox = new HBox(15);
        buttonsBox.setAlignment(Pos.CENTER_LEFT);

        
        generateButton = createStyledButton("🔄 Generate Crosstab", "#dc143c");
        generateButton.setOnAction(e -> generateCrosstab());

        
        showChartsButton = createStyledButton("📊 Open Charts", "#8b0000");
        showChartsButton.setDisable(true);
        showChartsButton.setOnAction(e -> showCharts());

        
        Button clearButton = createStyledButton("🗑️ Clear", "#6c757d");
        clearButton.setOnAction(e -> clearCrosstab());

        buttonsBox.getChildren().addAll(generateButton, showChartsButton, clearButton);
        return buttonsBox;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        button.setPrefHeight(35);
        button.setPrefWidth(150);

        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 6; " +
                        "-fx-border-radius: 6; " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 1; " +
                        "-fx-padding: 8 16 8 16;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: derive(" + color + ", 20%); " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-radius: 6; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 1; " +
                            "-fx-padding: 8 16 8 16; " +
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
                            "-fx-border-width: 1; " +
                            "-fx-padding: 8 16 8 16;"
            );
        });

        return button;
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox(10);
        tableSection.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 20; " +
                        "-fx-border-color: #dc143c; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.3), 10, 0, 0, 2);"
        );

        Text tableTitle = new Text("📋 Crosstab Results");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        tableTitle.setFill(Color.WHITE);

        
        crosstabTable = new TableView<>();
        crosstabTable.setStyle(
                "-fx-background-color: white;" +
                        "-fx-control-inner-background: white;" +
                        "-fx-table-cell-border-color: #d3d3d3;" +
                        "-fx-table-header-border-color: #d3d3d3;" +
                        "-fx-selection-bar: #0078d4;" +
                        "-fx-selection-bar-non-focused: #e6f3ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-border-color: #d3d3d3;" +
                        "-fx-border-width: 1px;"
        );

        crosstabTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        crosstabTable.setPrefHeight(400);

        crosstabTableManager = new CrosstabTable(crosstabTable, crosstabData);

        
        Label placeholderLabel = new Label("No crosstab data. Select columns and click 'Generate Crosstab' to begin.");
        placeholderLabel.setStyle(
                "-fx-text-fill: #6c757d; " +
                        "-fx-font-size: 14px; " +
                        "-fx-padding: 20;"
        );
        crosstabTable.setPlaceholder(placeholderLabel);

        
        tableSection.getChildren().addAll(tableTitle, crosstabTable);
        VBox.setVgrow(crosstabTable, Priority.ALWAYS);

        return tableSection;
    }

    private void switchColumns() {
        String xValue = xColumnField.getText();
        String yValue = yColumnField.getText();

        xColumnField.setText(yValue);
        yColumnField.setText(xValue);

        updateStatus("Columns switched. Click Generate to update crosstab.", false);
    }

    private void generateCrosstab() {
        try {
            String xColumn = xColumnField.getText().trim();
            String yColumn = yColumnField.getText().trim();

            if (xColumn.isEmpty() || yColumn.isEmpty()) {
                updateStatus("Please select both X and Y columns", true);
                return;
            }

            
            if (!originalItems.contains(xColumn)) {
                updateStatus("X column '" + xColumn + "' not found in data", true);
                return;
            }

            if (!originalItems.contains(yColumn)) {
                updateStatus("Y column '" + yColumn + "' not found in data", true);
                return;
            }

            if (xColumn.equals(yColumn)) {
                updateStatus("X and Y columns must be different", true);
                return;
            }

            boolean applyFilters = applyFiltersCheckBox.isSelected();
            boolean useMonthlyConversion = monthlyConversionCheckBox.isSelected();
            boolean useCumulativeEffective = true; 

            
            updateStatus("Generating crosstab...", false);

            
            crosstabData.generateCrosstabData(xColumn, yColumn, applyFilters, useMonthlyConversion);

            Map<String, Map<String, Integer>> filtered = crosstabData.getCrosstabData();
            if (filtered == null || filtered.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Aucune donnée");
                alert.setHeaderText("Impossible de générer le tableau croisé");
                alert.setContentText("Il n’y a pas de données à afficher après application des filtres.");
                alert.showAndWait();
                return; 
            }


            crosstabTableManager.createCrosstabTable(xColumn, yColumn, useMonthlyConversion, useCumulativeEffective, true);

            
            showChartsButton.setDisable(false);

            
            int rowCount = crosstabData.getCrosstabData().size();
            String cumulativeMsg = useCumulativeEffective ? " (with cumulative effective)" : "";
            updateStatus("Crosstab generated successfully! " + rowCount + " rows created" + cumulativeMsg + ".", false);

        } catch (Exception e) {
            updateStatus("Error generating crosstab: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void showCharts() {
        try {
            String xColumn = xColumnField.getText().trim();
            String yColumn = yColumnField.getText().trim();
            boolean useMonthlyConversion = monthlyConversionCheckBox.isSelected();
            boolean totalOnly = totalOnlyCheckBox.isSelected();
            boolean includeAllMonths = includeAllMonthsCheckBox.isSelected();

            crosstabCharts.showGraphTypeDialog(xColumn, yColumn, useMonthlyConversion, totalOnly, includeAllMonths);
        } catch (Exception e) {
            updateStatus("Error opening charts: " + e.getMessage(), true);
            e.printStackTrace();
        }
    }

    private void clearCrosstab() {
        crosstabTable.getColumns().clear();
        crosstabTable.getItems().clear();
        crosstabData.getCrosstabData().clear();
        showChartsButton.setDisable(true);
        updateStatus("Crosstab cleared. Ready to generate new analysis.", false);
    }

    private void updateStatus(String message, boolean isError) {
        statusText.setText(message);
        if (isError) {
            statusText.setFill(Color.rgb(220, 20, 60)); 
        } else {
            statusText.setFill(Color.rgb(255, 255, 255, 0.8)); 
        }
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = headers;
        this.allRows = allRows;

        crosstabData.setData(headers, allRows);

        originalItems = FXCollections.observableArrayList(headers);

        updateStatus("Data loaded. " + headers.size() + " columns available.", false);
    }

    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = filterManager;
        crosstabData.setFilterManager(filterManager);
    }

    public VBox getCrosstabSection() {
        return crosstabSection;
    }

    public TableView<ObservableList<String>> getCrosstabTable() {
        return crosstabTable;
    }

    public void refreshUI() {
        if (headers != null && !headers.isEmpty()) {
            originalItems = FXCollections.observableArrayList(headers);
        }
    }

    public CrosstabData getCrosstabData() {
        return crosstabData;
    }

    public String getXColumnValue() {
        return xColumnField.getText().trim();
    }

    public String getYColumnValue() {
        return yColumnField.getText().trim();
    }

    public void setXColumnValue(String value) {
        xColumnField.setText(value);
    }

    public void setYColumnValue(String value) {
        yColumnField.setText(value);
    }
}
