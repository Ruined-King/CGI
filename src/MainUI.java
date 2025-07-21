import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import javafx.scene.control.TableView;

public class MainUI extends Application {
    private Button fullscreenButton;
    private Stage primaryStage;

    private Scene welcomeScene;
    private Scene mainAppScene;
    private boolean isInMainApp = false;

    private File selectedFile;
    private Text pathText;
    private ExcelDataManager dataManager;
    private FilterManager filterManager;
    private TableViewController tableController;
    private CountManager countManager;
    private CrosstabUI crosstabUI;
    private ImageView loadingImage;
    private PresetManager.PresetData pendingPreset = null;
    private TableEditor embeddedEditor;


    // Logo
    private ImageView logoView;
    private ImageView companyLogoView;
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Safe icon loading
        try {
            Image icon = new Image(getClass().getResource("/resources/XED.jpg").toExternalForm());
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Warning: Could not load application icon");
        }

      
        dataManager = new ExcelDataManager();
        filterManager = new FilterManager();
        tableController = new TableViewController();

        VBox countSection = new VBox(10);
        countManager = new CountManager(countSection);
        try {
            crosstabUI = new CrosstabUI();
        } catch (Exception e) {
            System.err.println("Warning: Could not initialize CrosstabUI: " + e.getMessage());
            crosstabUI = null;
        }


        try {
            Image catGif = new Image(getClass().getResource("/resources/cat.gif").toExternalForm());
            loadingImage = new ImageView(catGif);
            loadingImage.setFitWidth(80);
            loadingImage.setPreserveRatio(true);
            loadingImage.setVisible(false);
        } catch (Exception e) {
            System.err.println("Warning: Could not load loading animation");
            loadingImage = new ImageView(); // Create empty ImageView
            loadingImage.setVisible(false);
        }

        // Safe preset callback setup
        try {
            PresetManager.setLoadPresetCallback(this::loadPresetData);
        } catch (Exception e) {
            System.err.println("Warning: Could not set preset callback");
        }

        logoView = createLogo();
        companyLogoView = createCompanyLogo();

        setupStage(primaryStage);
        createWelcomeScene();
        primaryStage.setScene(welcomeScene);
        primaryStage.show();
    }
    private ImageView createLogo() {
        ImageView logo = new ImageView();
        logo.setFitWidth(120);
        logo.setFitHeight(60);
        logo.setPreserveRatio(true);

        return logo;
    }
    private ImageView createCompanyLogo() {
        ImageView companyLogo = new ImageView();

        try {
            Image logoImage = new Image(getClass().getResource("/resources/CGI_LOGO.png").toExternalForm());
            companyLogo.setImage(logoImage);
            companyLogo.setFitWidth(120);
            companyLogo.setPreserveRatio(true);
            companyLogo.setSmooth(true);

            DropShadow logoShadow = new DropShadow();
            logoShadow.setColor(Color.rgb(0, 0, 0, 0.5));
            logoShadow.setOffsetX(2);
            logoShadow.setOffsetY(2);
            logoShadow.setRadius(5);
            companyLogo.setEffect(logoShadow);
        } catch (Exception e) {
            System.err.println("Warning: Could not load company logo");
            companyLogo = new ImageView();
            companyLogo.setFitWidth(120);
            companyLogo.setPreserveRatio(true);
        }

        return companyLogo;
    }

    private Text createTextLogo() {
        Text logoText = new Text("X.E.D");
        logoText.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 36));
        logoText.setFill(LinearGradient.valueOf("linear-gradient(from 0% 0% to 100% 100%, #dc143c 0%, #8b0000 100%)"));

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.8));
        shadow.setOffsetX(2);
        shadow.setOffsetY(2);
        shadow.setRadius(5);
        logoText.setEffect(shadow);

        return logoText;
    }

    private void createWelcomeScene() {
        VBox welcomeLayout = new VBox(40);
        welcomeLayout.setAlignment(Pos.CENTER);
        welcomeLayout.setPadding(new Insets(60));

        welcomeLayout.setStyle(
                "-fx-background: linear-gradient(to bottom right, #000000, #1a0000, #8b0000);"
        );

        HBox topSection = new HBox();
        topSection.setAlignment(Pos.TOP_LEFT);
        topSection.setPadding(new Insets(20));

        HBox companyLogoBox = new HBox();
        companyLogoBox.setAlignment(Pos.CENTER_LEFT);
        companyLogoBox.getChildren().add(companyLogoView);

        HBox logoSection = new HBox();
        logoSection.setAlignment(Pos.CENTER_RIGHT);
        Text logoText = createTextLogo();
        logoSection.getChildren().add(logoText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topSection.getChildren().addAll(companyLogoBox, spacer, logoSection);

        Text welcomeTitle = new Text("Welcome to XED");
        welcomeTitle.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        welcomeTitle.setFill(Color.WHITE);

        DropShadow titleShadow = new DropShadow();
        titleShadow.setColor(Color.rgb(220, 20, 60, 0.8)); // Crimson shadow
        titleShadow.setOffsetX(3);
        titleShadow.setOffsetY(3);
        titleShadow.setRadius(10);
        welcomeTitle.setEffect(titleShadow);


        // Subtitle
        Text subtitle = new Text("Your Excel Evaluator and Displayer Tool");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
        subtitle.setFill(Color.rgb(255, 255, 255, 0.9));

        // Path display
        pathText = createPathText();
        pathText.setFill(Color.rgb(255, 255, 255, 0.8));

        // Action buttons container
        VBox buttonsContainer = new VBox(20);
        buttonsContainer.setAlignment(Pos.CENTER);
        buttonsContainer.setMaxWidth(400);

        // File selection button
        Button chooseFileButton = createModernButton("📂 Choose Excel File", "#dc143c", 16);
        chooseFileButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Excel files", "*.xlsx", "*.xls"));
            selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                pathText.setText("📄 " + selectedFile.getName());
            }
        });

        // Enter button
        Button enterButton = createModernButton("🚀 ENTER APPLICATION", "#8b0000", 16);
        enterButton.setOnAction(e -> {
            if (selectedFile != null) {
                loadExcelFileAndEnterApp();
            } else {
                showModernAlert("File Required", "Please select an Excel file first!");
            }
        });

        // Preset buttons
        Button savePresetButton = createModernButton("💾 Save Preset", "#b22222", 14);
        Button managePresetsButton = createModernButton("📂 Manage Presets", "#a0522d", 14);

        savePresetButton.setOnAction(e -> {
            if (selectedFile != null) {
                PresetManager.savePreset(selectedFile.getAbsolutePath(), filterManager.getActiveFilters());
            } else {
                showModernAlert("File Required", "Please select an Excel file first!");
            }
        });

        managePresetsButton.setOnAction(e -> {
            PresetManager.showPresetManagerDialog();
        });

        // Button layout
        HBox presetButtons = new HBox(15, savePresetButton, managePresetsButton);
        presetButtons.setAlignment(Pos.CENTER);

        buttonsContainer.getChildren().addAll(
                chooseFileButton,
                pathText,
                enterButton,
                presetButtons
        );

        loadingImage.setVisible(false);

        VBox mainContent = new VBox(30);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.getChildren().addAll(
                welcomeTitle,
                subtitle,
                buttonsContainer,
                loadingImage
        );

        BorderPane completeLayout = new BorderPane();
        completeLayout.setTop(topSection);
        completeLayout.setCenter(mainContent);

        completeLayout.setStyle(
                "-fx-background: linear-gradient(to bottom right, #000000, #1a0000, #8b0000);"
        );

        welcomeScene = new Scene(completeLayout, 1200, 800);
    }

    private void createMainAppScene() {
        VBox mainLayout = new VBox();
        mainLayout.setStyle("-fx-background-color: #1a1a1a;"); // Dark background

        HBox topBar = createTopBar();

        TabPane tabPane = createModernTabPane();

        mainLayout.getChildren().addAll(topBar, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        mainAppScene = new Scene(mainLayout);
    }

    private void exportTableToExcel() {
        if (tableController == null) {
            showModernAlert("Export Error", "Table controller not initialized!");
            return;
        }

        TableView<ObservableList<String>> table = tableController.getTableView();
        if (table == null || table.getItems() == null || table.getItems().isEmpty()) {
            showModernAlert("Export Error", "No table data available to export!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel Export");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
        );

        String defaultName = "XED_Export_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".xlsx";
        fileChooser.setInitialFileName(defaultName);

        File saveFile = fileChooser.showSaveDialog(primaryStage);
        if (saveFile == null) {
            return;
        }

        if (loadingImage != null) {
            loadingImage.setVisible(true);
        }

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                    Sheet sheet = workbook.createSheet("XED Export");

                    ObservableList<ObservableList<String>> items = table.getItems();
                    if (items == null || items.isEmpty()) {
                        throw new RuntimeException("No data to export");
                    }

                    // Create header row
                    Row headerRow = sheet.createRow(0);
                    CellStyle headerStyle = workbook.createCellStyle();
                    org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
                    headerFont.setBold(true);
                    headerFont.setColor(IndexedColors.WHITE.getIndex());
                    headerStyle.setFont(headerFont);
                    headerStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
                    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    headerStyle.setBorderBottom(BorderStyle.THICK);
                    headerStyle.setBorderTop(BorderStyle.THICK);
                    headerStyle.setBorderRight(BorderStyle.THICK);
                    headerStyle.setBorderLeft(BorderStyle.THICK);

                    if (table.getColumns() != null) {
                        for (int col = 0; col < table.getColumns().size(); col++) {
                            Cell cell = headerRow.createCell(col);
                            String columnText = table.getColumns().get(col).getText();
                            cell.setCellValue(columnText != null ? columnText : "Column " + col);
                            cell.setCellStyle(headerStyle);
                        }
                    }

                    // Create data rows
                    CellStyle dataStyle = workbook.createCellStyle();
                    dataStyle.setBorderBottom(BorderStyle.THIN);
                    dataStyle.setBorderTop(BorderStyle.THIN);
                    dataStyle.setBorderRight(BorderStyle.THIN);
                    dataStyle.setBorderLeft(BorderStyle.THIN);

                    for (int rowIndex = 0; rowIndex < items.size(); rowIndex++) {
                        Row dataRow = sheet.createRow(rowIndex + 1);
                        ObservableList<String> rowData = items.get(rowIndex);

                        if (rowData != null) {
                            for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                                Cell cell = dataRow.createCell(colIndex);
                                String cellValue = rowData.get(colIndex);
                                cell.setCellValue(cellValue != null ? cellValue : "");
                                cell.setCellStyle(dataStyle);
                            }
                        }
                    }

                    // Auto-size columns
                    for (int i = 0; i < table.getColumns().size(); i++) {
                        sheet.autoSizeColumn(i);
                    }

                    try (FileOutputStream fileOut = new FileOutputStream(saveFile)) {
                        workbook.write(fileOut);
                    }

                } catch (Exception e) {
                    throw new RuntimeException("Failed to export Excel file: " + e.getMessage(), e);
                }

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    if (loadingImage != null) {
                        loadingImage.setVisible(false);
                    }
                    showModernAlert("Export Successful",
                            "Table exported successfully!\n" +
                                    "File saved as: " + saveFile.getName() + "\n" +
                                    "Location: " + saveFile.getAbsolutePath());
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    if (loadingImage != null) {
                        loadingImage.setVisible(false);
                    }
                    showModernAlert("Export Error",
                            "Failed to export table: " +
                                    (getException() != null ? getException().getMessage() : "Unknown error"));
                    if (getException() != null) {
                        getException().printStackTrace();
                    }
                });
            }
        };

        new Thread(exportTask).start();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(15, 25, 15, 25));
        topBar.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-border-color: #dc143c; " +
                        "-fx-border-width: 0 0 2 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.3), 10, 0, 0, 2);"
        );

        ImageView topCompanyLogo = createCompanyLogo();
        topCompanyLogo.setFitWidth(40);
        topCompanyLogo.setFitHeight(40);

        Text fileInfo = new Text();
        if (selectedFile != null) {
            fileInfo.setText("📄 " + selectedFile.getName());
        }
        fileInfo.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 14));
        fileInfo.setFill(Color.WHITE);

        Button newFileButton = createCompactButton("📂 New File", "#dc143c");
        Button savePresetButton = createCompactButton("💾 Save", "#b22222");
        Button managePresetsButton = createCompactButton("📂 Presets", "#8b0000");
        Button exportButton = createCompactButton("📤 Export Excel", "#228b22");
        fullscreenButton = createCompactButton("⛶ Fullscreen", "#a0522d");
        fullscreenButton.setDisable(true);

        newFileButton.setOnAction(e -> {
            primaryStage.setScene(welcomeScene);
            isInMainApp = false;
        });

        Button tableEditorButton = createCompactButton("🛠️ Edit Table", "#8b4513");
        tableEditorButton.setOnAction(e -> {
            try {
                if (embeddedEditor == null) {
                    embeddedEditor = new TableEditor(primaryStage, tableController, filterManager);
                }
                TableEditor editor = new TableEditor(primaryStage, tableController, filterManager);
                editor.showEditor();
            } catch (Exception ex) {
                showModernAlert("Editor Error", "Could not open table editor: " + ex.getMessage());
            }
        });

        savePresetButton.setOnAction(e -> {
            if (selectedFile != null) {
                PresetManager.savePreset(selectedFile.getAbsolutePath(), filterManager.getActiveFilters());
            }
        });

        managePresetsButton.setOnAction(e -> {
            PresetManager.showPresetManagerDialog();
        });


        exportButton.setOnAction(e -> exportTableToExcel());

        fullscreenButton.setOnAction(e -> openFullscreenTable());

        Text logoText = createTextLogo();
        logoText.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 24));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(
                topCompanyLogo,
                fileInfo,
                newFileButton,
                savePresetButton,
                managePresetsButton,
                exportButton,
                fullscreenButton,
                spacer,
                logoText
        );
        return topBar;
    }
    private TabPane createModernTabPane() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle(
                "-fx-background-color: #1a1a1a; " +
                        "-fx-tab-min-height: 40px;"
        );

        VBox dataTab = createDataViewTab();
        Tab dataTabControl = new Tab("📊 Data View", dataTab);
        dataTabControl.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-text-base-color: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );

        Tab crosstabTab;
        if (crosstabUI != null) {
            crosstabTab = new Tab("📈 Crosstab Analysis", crosstabUI.getCrosstabSection());
        } else {
            VBox errorBox = new VBox();
            errorBox.getChildren().add(new Label("Crosstab feature unavailable"));
            crosstabTab = new Tab("📈 Crosstab Analysis", errorBox);
        }

        crosstabTab.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-text-base-color: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );
        crosstabTab.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-text-base-color: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );
        embeddedEditor = new TableEditor(primaryStage, tableController, filterManager);
        Tab tableEditorTab = new Tab("🛠️ Table Editor", embeddedEditor.getEditorUI());
        tableEditorTab.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-text-base-color: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-font-weight: bold;"
        );
        tableEditorTab.setClosable(false);

        tabPane.getTabs().addAll(dataTabControl, crosstabTab,tableEditorTab);
        return tabPane;
    }

    private VBox createDataViewTab() {
        VBox dataTab = new VBox(15);
        dataTab.setPadding(new Insets(20));
        dataTab.setStyle("-fx-background-color: #1a1a1a;");

        VBox filterSection = new VBox(10);
        filterSection.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 20; " +
                        "-fx-border-color: #dc143c; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.3), 10, 0, 0, 2);"
        );

        Text filterTitle = new Text("🔍 Filters");
        filterTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        filterTitle.setFill(Color.WHITE);

        filterSection.getChildren().addAll(
                filterTitle,
                filterManager.getColumnFilterField(),
                filterManager.getFilterControlsBox(),
                filterManager.getActiveFiltersLabel(),
                filterManager.getActiveFiltersPane()
        );

        VBox countSectionStyled = new VBox(10);
        countSectionStyled.setStyle(
                "-fx-background-color: #000000; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 25; " +
                        "-fx-border-color: #dc143c; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.3), 10, 0, 0, 2);"
        );

        Text countTitle = new Text("📊 Statistics");
        countTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        countTitle.setFill(Color.WHITE);

        countSectionStyled.getChildren().addAll(
                countTitle,
                countManager.getCountSection(),
                countManager.getCountButton()
        );

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

        Text tableTitle = new Text("📋 Data Table");
        tableTitle.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        tableTitle.setFill(Color.WHITE);

        ScrollPane tableScrollPane = tableController.getTableScrollPane(); 
        VBox.setVgrow(tableScrollPane, Priority.ALWAYS);

        tableSection.getChildren().addAll(
                tableTitle,
                tableScrollPane
        );


        VBox.setVgrow(tableSection, Priority.ALWAYS);

        HBox grouped = new HBox(20, filterSection, countSectionStyled);
        HBox.setHgrow(filterSection, Priority.ALWAYS);
        HBox.setHgrow(countSectionStyled, Priority.ALWAYS);
        filterSection.setMaxWidth(Double.MAX_VALUE);
        countSectionStyled.setMaxWidth(Double.MAX_VALUE);

        dataTab.getChildren().addAll(grouped, tableSection);
        VBox.setVgrow(tableSection, Priority.ALWAYS);

        return dataTab;
    }

    private Button createModernButton(String text, String color, int fontSize) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
        button.setPrefWidth(300);
        button.setPrefHeight(50);

        button.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 25; " +
                        "-fx-border-radius: 25; " +
                        "-fx-border-color: white; " +
                        "-fx-border-width: 2; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 3);"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(button.getStyle() +
                    "-fx-scale-x: 1.05; -fx-scale-y: 1.05; " +
                    "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.6), 20, 0, 0, 5);"
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: " + color + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 25; " +
                            "-fx-border-radius: 25; " +
                            "-fx-border-color: white; " +
                            "-fx-border-width: 2; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 15, 0, 0, 3); " +
                            "-fx-scale-x: 1.0; -fx-scale-y: 1.0;"
            );
        });

        return button;
    }

    private Button createCompactButton(String text, String color) {
        Button button = new Button(text);
        button.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 12));
        button.setPrefHeight(35);

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
            button.setStyle(button.getStyle() +
                    "-fx-background-color: derive(" + color + ", 20%); " +
                    "-fx-effect: dropshadow(gaussian, rgba(220,20,60,0.4), 8, 0, 0, 2);");
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

    private void loadExcelFileAndEnterApp() {
        loadingImage.setVisible(true);

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                dataManager.loadExcelFile(selectedFile);

                filterManager.setData(dataManager.getHeaders(), dataManager.getAllRows());
                tableController.setData(dataManager.getHeaders(), dataManager.getAllRows());
                countManager.setData(dataManager.getHeaders(), dataManager.getAllRows());
                if (crosstabUI != null) {
                    crosstabUI.setData(dataManager.getHeaders(), dataManager.getAllRows());
                }
                return null;
            }

            @Override
            protected void succeeded() {

                Platform.runLater(() -> {
                    filterManager.setTableController(tableController);
                    tableController.setFilterManager(filterManager);
                    if (crosstabUI != null) {
                        crosstabUI.setFilterManager(filterManager);
                    }

                    if (pendingPreset != null) {
                        filterManager.clearAllFilters();
                        for (String filter : pendingPreset.filters) {
                            filterManager.addFilter(filter);
                        }
                        filterManager.refreshActiveFiltersDisplay();
                        filterManager.refreshUI();
                        if (crosstabUI != null) {
                            crosstabUI.refreshUI();
                        }
                        if (embeddedEditor != null) {
                            embeddedEditor.resetAfterPresetLoad();
                        }

                        showModernAlert("Success",
                                "Preset '" + pendingPreset.name + "' loaded successfully!\n" +
                                        "File: " + new File(pendingPreset.filePath).getName() + "\n" +
                                        "Rows: " + dataManager.getAllRows().size() +
                                        ", Columns: " + dataManager.getHeaders().size() +
                                        ", Filters applied: " + pendingPreset.filters.size());

                        pendingPreset = null;
                    } else {
                        filterManager.refreshUI();
                        if (crosstabUI != null) {

                            crosstabUI.refreshUI();
                        }
                        showModernAlert("Success",
                                "Excel file loaded successfully!\nRows: " + dataManager.getAllRows().size() +
                                        ", Columns: " + dataManager.getHeaders().size());
                    }

                    createMainAppScene();
                    primaryStage.setScene(mainAppScene);
                    isInMainApp = true;

                    fullscreenButton.setDisable(false);
                    loadingImage.setVisible(false);
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    showModernAlert("Error", "Failed to load Excel file: " + getException().getMessage());
                    getException().printStackTrace();
                    loadingImage.setVisible(false);
                    pendingPreset = null;
                });
            }
        };

        new Thread(loadTask).start();
    }

    private void loadPresetData(PresetManager.PresetData preset) {
        if (preset == null || preset.filePath == null) {
            showModernAlert("Error", "Invalid preset data");
            return;
        }

        File presetFile = new File(preset.filePath);
        if (!presetFile.exists()) {
            showModernAlert("Error", "Preset file not found: " + preset.filePath);
            return;
        }

        selectedFile = presetFile;
        if (pathText != null) {
            pathText.setText("📄 " + selectedFile.getName());
        }

        pendingPreset = preset;
        loadExcelFileAndEnterApp();
    }

    private void setupStage(Stage primaryStage) {
        primaryStage.setMaximized(false);
        primaryStage.setWidth(1280);
        primaryStage.setHeight(800);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.setTitle("XED - Excel Evaluator & Displayer");
    }


    private Text createPathText() {
        Text text = new Text("No file selected");
        text.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        text.setFill(Color.WHITE);
        return text;
    }

    private void openFullscreenTable() {
        Stage fullscreenStage = new Stage();
        fullscreenStage.setTitle("🔍 Fullscreen Table View - XED");

        TableView<ObservableList<String>> clonedTable = tableController.createClonedTableView();
        ScrollPane fullscreenPane = new ScrollPane(clonedTable);
        fullscreenPane.setFitToWidth(false); 
        fullscreenPane.setFitToHeight(true);
        fullscreenPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);  
        fullscreenPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fullscreenPane.setStyle("-fx-background-color: #1a1a1a;");

        Button closeButton = createCompactButton("❌ Close", "#dc143c");
        closeButton.setOnAction(e -> fullscreenStage.close());

        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(15));
        buttonBar.setStyle("-fx-background-color: #000000;");
        buttonBar.getChildren().add(closeButton);

        VBox vbox = new VBox(fullscreenPane, buttonBar);
        vbox.setStyle("-fx-background-color: #1a1a1a;");
        VBox.setVgrow(fullscreenPane, Priority.ALWAYS);

        Scene scene = new Scene(vbox, 1600, 900);
        fullscreenStage.setScene(scene);
        fullscreenStage.setMaximized(true);
        fullscreenStage.show();
    }


    private void showModernAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the alert with black and red theme
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-family: Arial; " +
                        "-fx-font-size: 14px; " +
                        "-fx-border-color: red; " +
                        "-fx-border-width: 2;"
        );

        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
