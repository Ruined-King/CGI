import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

public class TableViewController {
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();
    private TableView<ObservableList<String>> tableView = new TableView<>();
    {
        tableView.setStyle(
                "-fx-background-color: white;" +
                        "-fx-control-inner-background: white;" +
                        "-fx-table-cell-border-color: #ccc;" +
                        "-fx-table-header-border-color: #bbb;" +
                        "-fx-selection-bar: #d0eaff;" +  
                        "-fx-selection-bar-non-focused: #e0e0e0;" +
                        "-fx-text-fill: black;"
        );

        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY); 
    }
    private ScrollPane tableScrollPane;
    private FilterManager filterManager;

    public TableViewController() {
        setupTableView();
    }
    

    public List<String> getAllHeaders() {
        return new ArrayList<>(headers); 
    }

    public List<ObservableList<String>> getAllRows() {
        return new ArrayList<>(allRows); 
    }
    
    public List<String> getVisibleHeaders() {
        List<String> visibleHeaders = new ArrayList<>();
        for (TableColumn<ObservableList<String>, ?> col : tableView.getColumns()) {
            visibleHeaders.add(col.getText());
        }
        return visibleHeaders;
    }

    
    public List<ObservableList<String>> getFilteredRows() {
        return new ArrayList<>(tableView.getItems());
    }

    public void addNewColumn(String columnName, List<String> columnData) {
        
        headers.add(columnName);

        
        for (int i = 0; i < allRows.size() && i < columnData.size(); i++) {
            allRows.get(i).add(columnData.get(i));
        }

        
        for (int i = allRows.size(); i < columnData.size(); i++) {
            ObservableList<String> newRow = FXCollections.observableArrayList();
            
            for (int j = 0; j < headers.size() - 1; j++) {
                newRow.add("");
            }
            
            newRow.add(columnData.get(i));
            allRows.add(newRow);
        }

        
        refreshTable();
    }

    public TableView<ObservableList<String>> getTableView() {
        return tableView;
    }

    public ScrollPane getTableScrollPane() {
        return tableScrollPane;
    }

    private void setupTableView() {
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        tableScrollPane = new ScrollPane(tableView);
        tableScrollPane.setFitToWidth(true);
        tableScrollPane.setFitToHeight(true);
        tableScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        tableView.setStyle(
                "-fx-background-color: #f8f9fa;" +
                        "-fx-control-inner-background: #f8f9fa;" +
                        "-fx-table-cell-border-color: #cccccc;" +
                        "-fx-table-header-border-color: #bbbbbb;" +
                        "-fx-selection-bar: #d0eaff;" +
                        "-fx-selection-bar-non-focused: #e0e0e0;" +
                        "-fx-text-fill: #222;"
        );

        tableScrollPane.setStyle("-fx-background: white; -fx-background-color: white;");
    }

    public TableView<ObservableList<String>> createClonedTableView() {
        TableView<ObservableList<String>> clone = new TableView<>();
        clone.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        for (TableColumn<ObservableList<String>, ?> col : tableView.getColumns()) {
            TableColumn<ObservableList<String>, String> newCol = new TableColumn<>(col.getText());
            final int colIndex = tableView.getColumns().indexOf(col);
            newCol.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                return new SimpleStringProperty(colIndex < row.size() ? row.get(colIndex) : "");
            });
            newCol.setPrefWidth(150);
            clone.getColumns().add(newCol);
        }
        for (ObservableList<String> row : tableView.getItems()) {
            clone.getItems().add(FXCollections.observableArrayList(row));
        }
        return clone;
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = new ArrayList<>(headers);
        this.allRows = new ArrayList<>(allRows);
        applyFilters("", row -> true); 
    }

    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    public void applyFilters(String columnFilterInput, Predicate<ObservableList<String>> rowFilter) {
        if (headers.isEmpty()) return;

        tableView.getColumns().clear();
        tableView.getItems().clear();

        
        List<Integer> includedIndices = getIncludedColumnIndices(columnFilterInput);

        
        createTableColumns(includedIndices);

        
        addFilteredRows(rowFilter, includedIndices);
    }

    private List<Integer> getIncludedColumnIndices(String columnFilterInput) {
        List<String> includedNames = new ArrayList<>();
        if (columnFilterInput != null && !columnFilterInput.trim().isEmpty()) {
            includedNames = Arrays.stream(columnFilterInput.split(","))
                    .map(String::trim)
                    .map(String::toLowerCase)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<Integer> includedIndices = new ArrayList<>();

        for (int i = 0; i < headers.size(); i++) {
            boolean shouldInclude = includedNames.isEmpty();
            if (!shouldInclude) {
                String headerLower = headers.get(i).toLowerCase();
                for (String filter : includedNames) {
                    if (headerLower.contains(filter)) {
                        shouldInclude = true;
                        break;
                    }
                }
            }
            if (shouldInclude) {
                includedIndices.add(i);
            }
        }
        return includedIndices;
    }

    private void createTableColumns(List<Integer> includedIndices) {
        for (int i = 0; i < includedIndices.size(); i++) {
            int originalIndex = includedIndices.get(i);
            String headerText = headers.get(originalIndex);

            TableColumn<ObservableList<String>, String> column = new TableColumn<>(headerText);
            final int columnIndex = i;

            column.setCellValueFactory(param -> {
                ObservableList<String> row = param.getValue();
                if (columnIndex < row.size()) {
                    return new SimpleStringProperty(row.get(columnIndex));
                } else {
                    return new SimpleStringProperty("");
                }
            });

            column.setPrefWidth(150);
            column.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #111;");

            tableView.getColumns().add(column);
        }
    }

    private void addFilteredRows(Predicate<ObservableList<String>> rowFilter, List<Integer> includedIndices) {
        for (ObservableList<String> originalRow : allRows) {
            if (rowFilter.test(originalRow)) {
                ObservableList<String> filteredRow = FXCollections.observableArrayList();

                for (int index : includedIndices) {
                    if (index < originalRow.size()) {
                        filteredRow.add(originalRow.get(index));
                    } else {
                        filteredRow.add("");
                    }
                }

                tableView.getItems().add(filteredRow);
            }
        }
    }

    public void refreshTable() {
        if (filterManager != null) {
            Predicate<ObservableList<String>> rowFilter = row ->
                    filterManager.rowMatchesFilters(row, headers);
            applyFilters("", rowFilter);
        }
    }
}
