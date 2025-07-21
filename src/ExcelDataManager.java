import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ExcelDataManager {
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();

    public void loadExcelFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new Exception("File does not exist or is null");
        }

        if (!file.getName().toLowerCase().endsWith(".xlsx") && !file.getName().toLowerCase().endsWith(".xls")) {
            throw new Exception("File must be an Excel file (.xlsx or .xls)");
        }

        headers.clear();
        allRows.clear();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            if (workbook.getNumberOfSheets() == 0) {
                throw new Exception("Excel file contains no sheets");
            }

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new Exception("Could not read the first sheet");
            }

            boolean isFirstRow = true;
            int processedRows = 0;

            for (Row row : sheet) {
                if (row == null) continue;

                // Skip completely empty rows
                if (isRowEmpty(row)) {
                    if (!isFirstRow) continue; // Skip empty data rows, but process empty header row
                }

                ObservableList<String> rowData = FXCollections.observableArrayList();

                if (isFirstRow) {
                    // Process header row
                    for (int i = 0; i <= row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i);
                        String cellValue = extractCellValue(cell);
                        if (cellValue.trim().isEmpty()) {
                            cellValue = "Column " + (i + 1); // Default header name
                        }
                        rowData.add(cellValue);
                    }

                    if (rowData.isEmpty()) {
                        throw new Exception("Header row is empty");
                    }

                    headers.addAll(rowData);
                    isFirstRow = false;
                } else {
                    // Process data rows
                    int maxCells = Math.max(row.getLastCellNum(), headers.size());

                    for (int i = 0; i < maxCells; i++) {
                        Cell cell = row.getCell(i);
                        String cellValue = extractCellValue(cell);
                        rowData.add(cellValue);
                    }

                    // Ensure row has same number of columns as headers
                    while (rowData.size() < headers.size()) {
                        rowData.add("");
                    }

                    // Trim row to match header size
                    while (rowData.size() > headers.size()) {
                        rowData.remove(rowData.size() - 1);
                    }

                    allRows.add(rowData);
                    processedRows++;
                }
            }

            if (headers.isEmpty()) {
                throw new Exception("No headers found in Excel file");
            }

            System.out.println("Loaded Excel file: " + file.getName() +
                    " - Headers: " + headers.size() +
                    ", Data rows: " + processedRows);

        } catch (Exception e) {
            headers.clear();
            allRows.clear();
            throw new Exception("Failed to load Excel file: " + e.getMessage(), e);
        }
    }

    private String extractCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            CellType cellType = cell.getCellType();

            // Handle formula cells by evaluating them first
            if (cellType == CellType.FORMULA) {
                try {
                    cellType = cell.getCachedFormulaResultType();
                } catch (Exception e) {
                    return "FORMULA_ERROR";
                }
            }

            switch (cellType) {
                case STRING:
                    String stringValue = cell.getStringCellValue();
                    return stringValue != null ? stringValue.trim() : "";

                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        try {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                            return sdf.format(cell.getDateCellValue());
                        } catch (Exception e) {
                            return "DATE_ERROR";
                        }
                    } else {
                        double numValue = cell.getNumericCellValue();
                        // Check if it's a whole number
                        if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    }

                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());

                case BLANK:
                    return "";

                default:
                    return "";
            }
        } catch (Exception ex) {
            System.err.println("Error extracting cell value: " + ex.getMessage());
            return "ERROR";
        }
    }

    public List<String> getHeaders() {
        return headers != null ? new ArrayList<>(headers) : new ArrayList<>();
    }

    public List<ObservableList<String>> getAllRows() {
        return allRows != null ? new ArrayList<>(allRows) : new ArrayList<>();
    }
    public String getDataInfo() {
        return "Headers: " + (headers != null ? headers.size() : 0) +
                ", Rows: " + (allRows != null ? allRows.size() : 0);
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !extractCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

}