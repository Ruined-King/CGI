import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class PresetManager {
    private static final String PRESETS_DIR = "presets";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Create Gson with custom LocalDateTime adapter
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .setPrettyPrinting()
            .create();

    static {
        // Create presets directory if it doesn't exist
        File dir = new File(PRESETS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        @Override
        public void write(JsonWriter out, LocalDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(DATE_FORMATTER));
            }
        }

        @Override
        public LocalDateTime read(JsonReader in) throws IOException {
            if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String dateString = in.nextString();
            return LocalDateTime.parse(dateString, DATE_FORMATTER);
        }
    }

    public static void savePreset(String filePath, List<String> filters) {
        showSavePresetDialog(filePath, filters);
    }

    private static void showSavePresetDialog(String filePath, List<String> filters) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("💾 Save Preset");
        dialog.setResizable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2c3e50;");

        // Title
        Label titleLabel = new Label("Save New Preset");
        titleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 16));
        titleLabel.setTextFill(Color.WHITE);

        // Preset name input
        Label nameLabel = new Label("Preset Name:");
        nameLabel.setTextFill(Color.WHITE);
        nameLabel.setFont(Font.font("Verdana", 12));

        TextField nameField = new TextField();
        nameField.setPromptText("Enter preset name...");
        nameField.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-prompt-text-fill: #bdc3c7;");
        nameField.setPrefWidth(300);

        // Auto-suggest name based on current timestamp
        String suggestedName = "Preset_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        nameField.setText(suggestedName);
        nameField.selectAll();

        // Info about the preset
        Label infoLabel = new Label("File: " + new File(filePath).getName() + "\nFilters: " + filters.size());
        infoLabel.setTextFill(Color.LIGHTGRAY);
        infoLabel.setFont(Font.font("Verdana", 10));

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("💾 Save");
        Button cancelButton = new Button("❌ Cancel");

        styleButton(saveButton, "#27ae60");
        styleButton(cancelButton, "#e74c3c");

        buttonBox.getChildren().addAll(saveButton, cancelButton);

        layout.getChildren().addAll(titleLabel, nameLabel, nameField, infoLabel, buttonBox);

        saveButton.setOnAction(e -> {
            String presetName = nameField.getText().trim();
            if (presetName.isEmpty()) {
                showAlert("Error", "Please enter a preset name!");
                return;
            }

            if (savePresetToFile(presetName, filePath, filters)) {
                showAlert("Success", "Preset '" + presetName + "' saved successfully!");
                dialog.close();
            } else {
                showAlert("Error", "Failed to save preset!");
            }
        });

        cancelButton.setOnAction(e -> dialog.close());

        // Enter key saves
        nameField.setOnAction(e -> saveButton.fire());

        Scene scene = new Scene(layout);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static boolean savePresetToFile(String presetName, String filePath, List<String> filters) {
        try {
            String fileName = presetName.replaceAll("[^a-zA-Z0-9_-]", "_") + ".json";
            File presetFile = new File(PRESETS_DIR, fileName);

            PresetData preset = new PresetData(presetName, filePath, filters, LocalDateTime.now());

            try (FileWriter writer = new FileWriter(presetFile, java.nio.charset.StandardCharsets.UTF_8)) {
                gson.toJson(preset, writer);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void showPresetManagerDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("📂 Preset Manager");
        dialog.setResizable(true);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2c3e50;");

        // Title
        Label titleLabel = new Label("Manage Presets");
        titleLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);

        // Preset list
        ListView<PresetItem> presetList = new ListView<>();
        presetList.setPrefHeight(300);
        presetList.setPrefWidth(500);
        presetList.setStyle("-fx-background-color: #34495e; -fx-border-color: #7f8c8d;");

        // Custom cell factory for preset items
        presetList.setCellFactory(listView -> new PresetListCell());

        // Load presets
        loadPresetsIntoList(presetList);

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button loadButton = new Button("📂 Load Selected");
        Button deleteButton = new Button("🗑️ Delete Selected");
        Button refreshButton = new Button("🔄 Refresh");
        Button closeButton = new Button("❌ Close");

        styleButton(loadButton, "#3498db");
        styleButton(deleteButton, "#e74c3c");
        styleButton(refreshButton, "#f39c12");
        styleButton(closeButton, "#95a5a6");

        buttonBox.getChildren().addAll(loadButton, deleteButton, refreshButton, closeButton);

        layout.getChildren().addAll(titleLabel, presetList, buttonBox);

        // Button actions
        loadButton.setOnAction(e -> {
            PresetItem selected = presetList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                loadPresetCallback.accept(selected.preset);
                dialog.close();
            } else {
                showAlert("Warning", "Please select a preset to load!");
            }
        });

        deleteButton.setOnAction(e -> {
            PresetItem selected = presetList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Confirm Delete");
                confirmAlert.setHeaderText("Delete Preset");
                confirmAlert.setContentText("Are you sure you want to delete '" + selected.preset.name + "'?");

                Optional<ButtonType> result = confirmAlert.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    if (deletePreset(selected.fileName)) {
                        loadPresetsIntoList(presetList);
                        showAlert("Success", "Preset deleted successfully!");
                    } else {
                        showAlert("Error", "Failed to delete preset!");
                    }
                }
            } else {
                showAlert("Warning", "Please select a preset to delete!");
            }
        });

        refreshButton.setOnAction(e -> loadPresetsIntoList(presetList));
        closeButton.setOnAction(e -> dialog.close());

        Scene scene = new Scene(layout);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static void loadPresetsIntoList(ListView<PresetItem> presetList) {
        List<PresetItem> presets = new ArrayList<>();
        File presetsDir = new File(PRESETS_DIR);

        if (presetsDir.exists() && presetsDir.isDirectory()) {
            File[] files = presetsDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (FileReader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
                        PresetData preset = gson.fromJson(reader, PresetData.class);
                        if (preset != null) {
                            presets.add(new PresetItem(preset, file.getName()));
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading preset: " + file.getName() + " - " + e.getMessage());
                    }
                }
            }
        }

        // Sort by creation date (newest first)
        presets.sort((a, b) -> {
            if (a.preset.createdAt == null && b.preset.createdAt == null) return 0;
            if (a.preset.createdAt == null) return 1;
            if (b.preset.createdAt == null) return -1;
            return b.preset.createdAt.compareTo(a.preset.createdAt);
        });

        Platform.runLater(() -> {
            presetList.getItems().clear();
            presetList.getItems().addAll(presets);
        });
    }

    private static boolean deletePreset(String fileName) {
        try {
            File file = new File(PRESETS_DIR, fileName);
            return file.delete();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void styleButton(Button button, String color) {
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

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Callback for loading preset
    private static java.util.function.Consumer<PresetData> loadPresetCallback;

    public static void setLoadPresetCallback(java.util.function.Consumer<PresetData> callback) {
        loadPresetCallback = callback;
    }

    // Data classes
    public static class PresetData {
        public String name;
        public String filePath;
        public List<String> filters;
        public LocalDateTime createdAt;

        public PresetData(String name, String filePath, List<String> filters, LocalDateTime createdAt) {
            this.name = name;
            this.filePath = filePath;
            this.filters = filters;
            this.createdAt = createdAt;
        }

        // Default constructor for Gson
        public PresetData() {
        }
    }

    private static class PresetItem {
        public PresetData preset;
        public String fileName;

        public PresetItem(PresetData preset, String fileName) {
            this.preset = preset;
            this.fileName = fileName;
        }
    }

    // Custom list cell for presets
    private static class PresetListCell extends ListCell<PresetItem> {
        @Override
        protected void updateItem(PresetItem item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                setStyle("");
            } else {
                VBox content = new VBox(5);
                content.setPadding(new Insets(8));

                Label nameLabel = new Label(item.preset.name);
                nameLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
                nameLabel.setTextFill(Color.WHITE);

                Label fileLabel = new Label("File: " + new File(item.preset.filePath).getName());
                fileLabel.setFont(Font.font("Verdana", 10));
                fileLabel.setTextFill(Color.LIGHTGRAY);

                Label filtersLabel = new Label("Filters: " + (item.preset.filters != null ? item.preset.filters.size() : 0));
                filtersLabel.setFont(Font.font("Verdana", 10));
                filtersLabel.setTextFill(Color.LIGHTBLUE);

                String dateStr = "Unknown";
                if (item.preset.createdAt != null) {
                    dateStr = item.preset.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                }
                Label dateLabel = new Label("Created: " + dateStr);
                dateLabel.setFont(Font.font("Verdana", 10));
                dateLabel.setTextFill(Color.LIGHTYELLOW);

                content.getChildren().addAll(nameLabel, fileLabel, filtersLabel, dateLabel);

                setGraphic(content);
                setText(null);
                setStyle("-fx-background-color: #34495e; -fx-border-color: #7f8c8d; -fx-border-width: 0 0 1 0;");
            }
        }
    }

    // Legacy method for backward compatibility
    public static PresetData loadPreset() {
        // This method is now deprecated, use showPresetManagerDialog instead
        return null;
    }


}
