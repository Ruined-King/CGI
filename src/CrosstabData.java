import javafx.collections.ObservableList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class CrosstabData {
    private Map<String, Map<String, Integer>> crosstabData = new HashMap<>();
    private List<String> headers = new ArrayList<>();
    private List<ObservableList<String>> allRows = new ArrayList<>();
    private FilterManager filterManager;

    private static final String[] MONTH_NAMES = {
            "Jan", "Fév", "Mar", "Avr", "Mai", "Jun",
            "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"
    };

    private Map<String, Integer> cumulativeEffective = new HashMap<>();
    private Map<String, Integer> rowTotals = new HashMap<>();
    private Map<String, Integer> columnTotals = new HashMap<>();
    private int grandTotal = 0;

    private static final Map<String, Integer> MONTH_MAP = new HashMap<>();
    static {
        MONTH_MAP.put("Jan", 1);
        MONTH_MAP.put("Janv", 1);
        MONTH_MAP.put("Fév", 2);
        MONTH_MAP.put("Fev", 2);
        MONTH_MAP.put("Mar", 3);
        MONTH_MAP.put("Avr", 4);
        MONTH_MAP.put("Mai", 5);
        MONTH_MAP.put("Jun", 6);
        MONTH_MAP.put("Juin", 6);
        MONTH_MAP.put("Jul", 7);
        MONTH_MAP.put("Juil", 7);
        MONTH_MAP.put("Juill", 7);
        MONTH_MAP.put("Aoû", 8);
        MONTH_MAP.put("Août", 8);
        MONTH_MAP.put("Sep", 9);
        MONTH_MAP.put("Sept", 9);
        MONTH_MAP.put("Oct", 10);
        MONTH_MAP.put("Nov", 11);
        MONTH_MAP.put("Déc", 12);
        MONTH_MAP.put("Dec", 12);

        MONTH_MAP.put("January", 1);
        MONTH_MAP.put("February", 2);
        MONTH_MAP.put("March", 3);
        MONTH_MAP.put("April", 4);
        MONTH_MAP.put("May", 5);
        MONTH_MAP.put("June", 6);
        MONTH_MAP.put("July", 7);
        MONTH_MAP.put("August", 8);
        MONTH_MAP.put("September", 9);
        MONTH_MAP.put("October", 10);
        MONTH_MAP.put("November", 11);
        MONTH_MAP.put("December", 12);
    }

    public CrosstabData() {
    }

    // Setters and getters
    public void setCumulativeEffective(Map<String, Integer> cumulativeEffective) {
        this.cumulativeEffective = cumulativeEffective != null ?
                new HashMap<>(cumulativeEffective) : new HashMap<>();
    }

    public Map<String, Integer> getCumulativeEffective() {
        return new HashMap<>(cumulativeEffective);
    }

    public void setTotals(Map<String, Integer> rowTotals, Map<String, Integer> columnTotals, int grandTotal) {
        this.rowTotals = rowTotals != null ? new HashMap<>(rowTotals) : new HashMap<>();
        this.columnTotals = columnTotals != null ? new HashMap<>(columnTotals) : new HashMap<>();
        this.grandTotal = grandTotal;
    }

    public Map<String, Integer> getRowTotals() {
        return new HashMap<>(rowTotals);
    }

    public Map<String, Integer> getColumnTotals() {
        return new HashMap<>(columnTotals);
    }

    public int getGrandTotal() {
        return grandTotal;
    }

    public void setData(List<String> headers, List<ObservableList<String>> allRows) {
        this.headers = headers != null ? new ArrayList<>(headers) : new ArrayList<>();
        this.allRows = allRows != null ? new ArrayList<>(allRows) : new ArrayList<>();
    }

    public void setFilterManager(FilterManager filterManager) {
        this.filterManager = filterManager;
    }

    public boolean isDateColumn(String columnName) {
        if (columnName == null || columnName.trim().isEmpty()) {
            return false;
        }

        int columnIndex = headers.indexOf(columnName);
        if (columnIndex == -1) return false;
        int checkedCount = 0;
        int dateCount = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        for (ObservableList<String> row : allRows) {
            if (checkedCount >= 10) break;
            if (row == null || columnIndex >= row.size()) continue;

            String value = row.get(columnIndex);
            if (value == null || value.trim().isEmpty()) continue;

            try {
                sdf.parse(value.trim());
                dateCount++;
            } catch (ParseException e) {

            }
            checkedCount++;
        }

        return checkedCount > 0 && (dateCount >= checkedCount * 0.7); // 70% should be dates
    }

    public void generateCrosstabData(String xColumn, String yColumn, boolean applyFilters, boolean useMonthlyConversion) {
        generateCrosstabData(xColumn, yColumn, applyFilters, useMonthlyConversion, false);
    }

    public void generateCrosstabData(String xColumn, String yColumn, boolean applyFilters, boolean useMonthlyConversion, boolean includeAllMonths) {
        if (xColumn == null || yColumn == null || xColumn.trim().isEmpty() || yColumn.trim().isEmpty()) {
            throw new IllegalArgumentException("X and Y columns cannot be null or empty");
        }

        int xIndex = headers.indexOf(xColumn);
        int yIndex = headers.indexOf(yColumn);

        if (xIndex == -1 || yIndex == -1) {
            throw new IllegalArgumentException("Selected columns not found in headers!");
        }

        List<ObservableList<String>> dataToProcess = allRows;
        if (applyFilters && filterManager != null) {
            System.out.println("Applying filters in crosstab generation...");
            System.out.println("FilterManager has " + filterManager.getActiveFilters().size() + " active filters");

            dataToProcess = filterManager.getFilteredRows();

            if (dataToProcess.size() == allRows.size() && filterManager.hasActiveFilters()) {
                System.out.println("Using direct filtering approach...");
                dataToProcess = allRows.stream()
                        .filter(row -> {
                            if (row == null) return false;
                            boolean matches = filterManager.rowMatchesFilters(row, headers);
                            return matches;
                        })
                        .collect(Collectors.toList());
            }

            System.out.println("Filtered data: " + dataToProcess.size() + " rows (from " + allRows.size() + " total)");
        }

        crosstabData.clear();

        System.out.println("Processing " + dataToProcess.size() + " rows for crosstab generation");

        for (ObservableList<String> row : dataToProcess) {
            if (row == null || xIndex >= row.size() || yIndex >= row.size()) continue;

            String xValue = row.get(xIndex);
            String yValue = row.get(yIndex);

            if (xValue == null || yValue == null) continue;

            xValue = xValue.trim();
            yValue = yValue.trim();

            if (xValue.isEmpty() || yValue.isEmpty()) continue;

            if (useMonthlyConversion) {
                if (isDateColumn(xColumn)) {
                    xValue = convertToMonthYear(xValue);
                    if (xValue == null) continue;
                }
                if (isDateColumn(yColumn)) {
                    yValue = convertToMonthYear(yValue);
                    if (yValue == null) continue;
                }
            }

            crosstabData.computeIfAbsent(xValue, k -> new HashMap<>());
            crosstabData.get(xValue).merge(yValue, 1, Integer::sum);
        }

        if (useMonthlyConversion) {
            Set<String> completeXRange = new TreeSet<>(this::compareMonthYear);
            Set<String> completeYRange = new TreeSet<>(this::compareMonthYear);

            if (isDateColumn(xColumn)) {
                completeXRange = createMonthRangeForX(xColumn, applyFilters, includeAllMonths);
            }
            if (isDateColumn(yColumn)) {
                completeYRange = createMonthRangeForY(yColumn, applyFilters, includeAllMonths);
            }

            if (isDateColumn(xColumn)) {
                for (String xMonth : completeXRange) {
                    crosstabData.computeIfAbsent(xMonth, k -> new HashMap<>());
                }
            }

            if (isDateColumn(yColumn)) {
                for (Map<String, Integer> xRow : crosstabData.values()) {
                    for (String yMonth : completeYRange) {
                        xRow.computeIfAbsent(yMonth, k -> 0);
                    }
                }
            }
        }

        int totalEntries = crosstabData.values().stream()
                .mapToInt(map -> map.values().stream().mapToInt(Integer::intValue).sum())
                .sum();
        System.out.println("Total entries in crosstab: " + totalEntries);
        System.out.println("Crosstab data keys: " + crosstabData.keySet());
    }

    public String convertToMonthYear(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
            inputFormat.setLenient(false);
            Date date = inputFormat.parse(dateStr.trim());

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            return convertCalendarToMonthYear(cal);

        } catch (ParseException e) {
            System.err.println("Failed to parse date: " + dateStr);
            return null;
        }
    }

    public Set<String> createCompleteMonthRange() {
        return createCompleteMonthRange(false);
    }

    public Set<String> createCompleteMonthRange(boolean includeAllMonths) {
        Set<String> monthRange = new TreeSet<>(this::compareMonthYear);
        Date minDate = null;
        Date maxDate = null;

        int dateColumnIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (isDateColumn(headers.get(i))) {
                dateColumnIndex = i;
                break;
            }
        }

        if (dateColumnIndex == -1) return monthRange;

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        List<ObservableList<String>> dataToProcess = allRows;
        if (filterManager != null && filterManager.hasActiveFilters()) {
            dataToProcess = filterManager.getFilteredRows();
        }

        for (ObservableList<String> row : dataToProcess) {
            if (row == null || dateColumnIndex >= row.size()) continue;
            String value = row.get(dateColumnIndex);
            if (value == null) continue;
            value = value.trim();

            try {
                Date date = sdf.parse(value);
                if (minDate == null || date.before(minDate)) minDate = date;
                if (maxDate == null || date.after(maxDate)) maxDate = date;
            } catch (ParseException e) {
            }
        }

        if (minDate != null && maxDate != null) {
            monthRange.addAll(generateMonthRange(minDate, maxDate, includeAllMonths));
        }

        return monthRange;
    }

    private Set<String> generateMonthRange(Date minDate, Date maxDate, boolean includeAllMonths) {
        Set<String> monthRange = new TreeSet<>(this::compareMonthYear);

        Calendar cal = Calendar.getInstance();
        cal.setTime(minDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Calendar maxCal = Calendar.getInstance();
        maxCal.setTime(maxDate);

        if (includeAllMonths) {
            maxCal.set(Calendar.MONTH, Calendar.DECEMBER);
            maxCal.set(Calendar.DAY_OF_MONTH, 31);
        }

        while (cal.get(Calendar.YEAR) < maxCal.get(Calendar.YEAR) ||
                (cal.get(Calendar.YEAR) == maxCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) <= maxCal.get(Calendar.MONTH))) {

            String monthYear = convertCalendarToMonthYear(cal);
            monthRange.add(monthYear);
            cal.add(Calendar.MONTH, 1);
        }

        return monthRange;
    }

    public Set<String> createMonthRangeForY(String yColumn, boolean applyFilters) {
        return createMonthRangeForY(yColumn, applyFilters, false);
    }

    public Set<String> createMonthRangeForY(String yColumn, boolean applyFilters, boolean includeAllMonths) {
        Set<String> monthRange = new TreeSet<>(this::compareMonthYear);

        if (yColumn == null || !isDateColumn(yColumn)) {
            return monthRange;
        }

        int yIndex = headers.indexOf(yColumn);
        if (yIndex == -1) return monthRange;

        List<ObservableList<String>> dataToProcess = allRows;
        if (applyFilters && filterManager != null && filterManager.hasActiveFilters()) {
            dataToProcess = filterManager.getFilteredRows();
        }

        Date minDate = null;
        Date maxDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        for (ObservableList<String> row : dataToProcess) {
            if (row != null && yIndex < row.size()) {
                String yValue = row.get(yIndex);
                if (yValue != null && !yValue.trim().isEmpty()) {
                    try {
                        Date date = sdf.parse(yValue.trim());
                        if (minDate == null || date.before(minDate)) {
                            minDate = date;
                        }
                        if (maxDate == null || date.after(maxDate)) {
                            maxDate = date;
                        }
                    } catch (ParseException e) {
                    }
                }
            }
        }

        if (minDate != null && maxDate != null) {
            monthRange.addAll(generateMonthRange(minDate, maxDate, includeAllMonths));
        }

        return monthRange;
    }

    public Set<String> createMonthRangeForX(String xColumn, boolean applyFilters) {
        return createMonthRangeForX(xColumn, applyFilters, false);
    }

    public Set<String> createMonthRangeForX(String xColumn, boolean applyFilters, boolean includeAllMonths) {
        Set<String> monthRange = new TreeSet<>(this::compareMonthYear);

        if (!isDateColumn(xColumn)) {
            return monthRange;
        }

        int xIndex = headers.indexOf(xColumn);
        if (xIndex == -1) return monthRange;

        List<ObservableList<String>> dataToProcess = allRows;
        if (applyFilters && filterManager != null && filterManager.hasActiveFilters()) {
            dataToProcess = filterManager.getFilteredRows();
        }

        Date minDate = null;
        Date maxDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        for (ObservableList<String> row : dataToProcess) {
            if (row != null && xIndex < row.size()) {
                String xValue = row.get(xIndex);
                if (xValue != null && !xValue.trim().isEmpty()) {
                    try {
                        Date date = sdf.parse(xValue.trim());
                        if (minDate == null || date.before(minDate)) {
                            minDate = date;
                        }
                        if (maxDate == null || date.after(maxDate)) {
                            maxDate = date;
                        }
                    } catch (ParseException e) {
                    }
                }
            }
        }

        if (minDate != null && maxDate != null) {
            monthRange.addAll(generateMonthRange(minDate, maxDate, includeAllMonths));
        }

        return monthRange;
    }

    public Set<String> createCompleteMonthRange(String xColumn, String yColumn, boolean applyFilters) {
        return createCompleteMonthRange(xColumn, yColumn, applyFilters, false);
    }

    public Set<String> createCompleteMonthRange(String xColumn, String yColumn, boolean applyFilters, boolean includeAllMonths) {
        Set<String> monthRange = new TreeSet<>(this::compareMonthYear);

        int xIndex = headers.indexOf(xColumn);
        int yIndex = headers.indexOf(yColumn);

        if (xIndex == -1 || yIndex == -1) return monthRange;

        List<ObservableList<String>> dataToProcess = allRows;
        if (applyFilters && filterManager != null && filterManager.hasActiveFilters()) {
            dataToProcess = filterManager.getFilteredRows();
        }

        Date minDate = null;
        Date maxDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        for (ObservableList<String> row : dataToProcess) {
            if (isDateColumn(xColumn) && xIndex < row.size()) {
                String xValue = row.get(xIndex).trim();
                Date date = parseDate(xValue, sdf);
                if (date != null) {
                    if (minDate == null || date.before(minDate)) minDate = date;
                    if (maxDate == null || date.after(maxDate)) maxDate = date;
                }
            }

            if (isDateColumn(yColumn) && yIndex < row.size()) {
                String yValue = row.get(yIndex).trim();
                Date date = parseDate(yValue, sdf);
                if (date != null) {
                    if (minDate == null || date.before(minDate)) minDate = date;
                    if (maxDate == null || date.after(maxDate)) maxDate = date;
                }
            }
        }

        if (minDate != null && maxDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(minDate);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Calendar maxCal = Calendar.getInstance();
            maxCal.setTime(maxDate);

            if (includeAllMonths) {
                maxCal.set(Calendar.MONTH, Calendar.DECEMBER);
                maxCal.set(Calendar.DAY_OF_MONTH, 31);
            }

            while (cal.get(Calendar.YEAR) < maxCal.get(Calendar.YEAR) ||
                    (cal.get(Calendar.YEAR) == maxCal.get(Calendar.YEAR) &&
                            cal.get(Calendar.MONTH) <= maxCal.get(Calendar.MONTH))) {

                String monthYear = convertCalendarToMonthYear(cal);
                monthRange.add(monthYear);
                cal.add(Calendar.MONTH, 1);
            }
        }

        return monthRange;
    }

    private Date parseDate(String dateStr, SimpleDateFormat sdf) {
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    public Set<String> createCompleteMonthRangeForX() {
        return createCompleteMonthRangeForX(false);
    }

    public Set<String> createCompleteMonthRangeForX(boolean includeAllMonths) {
        Set<String> monthRange = new TreeSet<>(this::compareMonthYear);
        List<ObservableList<String>> dataToProcess = allRows;
        if (filterManager != null && filterManager.hasActiveFilters()) {
            dataToProcess = filterManager.getFilteredRows();
        }

        int dateColumnIndex = -1;
        for (int i = 0; i < headers.size(); i++) {
            if (isDateColumn(headers.get(i))) {
                dateColumnIndex = i;
                break;
            }
        }

        if (dateColumnIndex == -1) return monthRange;

        Date minDate = null;
        Date maxDate = null;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        sdf.setLenient(false);

        for (ObservableList<String> row : dataToProcess) {
            if (dateColumnIndex >= row.size()) continue;
            String value = row.get(dateColumnIndex).trim();

            Date date = parseDate(value, sdf);
            if (date != null) {
                if (minDate == null || date.before(minDate)) {
                    minDate = date;
                }
                if (maxDate == null || date.after(maxDate)) {
                    maxDate = date;
                }
            }
        }

        if (minDate != null && maxDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(minDate);
            cal.set(Calendar.DAY_OF_MONTH, 1); // Start from first day of month

            Calendar maxCal = Calendar.getInstance();
            maxCal.setTime(maxDate);

            if (includeAllMonths) {
                maxCal.set(Calendar.MONTH, Calendar.DECEMBER);
                maxCal.set(Calendar.DAY_OF_MONTH, 31);
            }

            while (cal.get(Calendar.YEAR) < maxCal.get(Calendar.YEAR) ||
                    (cal.get(Calendar.YEAR) == maxCal.get(Calendar.YEAR) &&
                            cal.get(Calendar.MONTH) <= maxCal.get(Calendar.MONTH))) {

                String monthYear = convertCalendarToMonthYear(cal);
                monthRange.add(monthYear);
                cal.add(Calendar.MONTH, 1);
            }
        }

        return monthRange;
    }

    private Date parseMonthYearToDate(String monthYear) {
        try {
            String[] parts = monthYear.split(" ");
            if (parts.length == 2) {
                String monthName = parts[0];
                int year = Integer.parseInt(parts[1]);
                Integer month = MONTH_MAP.get(monthName);

                if (month != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.set(year, month - 1, 1);
                    return cal.getTime();
                }
            }
        } catch (Exception e) {
            // Skip invalid dates
        }
        return null;
    }

    public int getMonthNumber(String monthName) {
        Integer monthNum = MONTH_MAP.get(monthName);
        return monthNum != null ? monthNum : 1;
    }

    public String convertCalendarToMonthYear(Calendar cal) {
        return MONTH_NAMES[cal.get(Calendar.MONTH)] + " " + cal.get(Calendar.YEAR);
    }

    private int compareMonthYear(String a, String b) {
        try {
            String[] partsA = a.split(" ");
            String[] partsB = b.split(" ");

            if (partsA.length == 2 && partsB.length == 2) {
                int yearA = Integer.parseInt(partsA[1]);
                int yearB = Integer.parseInt(partsB[1]);

                if (yearA != yearB) {
                    return Integer.compare(yearA, yearB);
                }

                Integer monthA = MONTH_MAP.get(partsA[0]);
                Integer monthB = MONTH_MAP.get(partsB[0]);

                if (monthA != null && monthB != null) {
                    return Integer.compare(monthA, monthB);
                }
            }
            return a.compareTo(b);
        } catch (Exception e) {
            return a.compareTo(b);
        }
    }

    public List<String> sortMonthYearValues(List<String> monthYearValues) {
        return monthYearValues.stream()
                .sorted(this::compareMonthYear)
                .collect(Collectors.toList());
    }

    // Getters
    public Map<String, Map<String, Integer>> getCrosstabData() {
        return crosstabData;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<ObservableList<String>> getAllRows() {
        return allRows;
    }

    public boolean isEmpty() {
        return crosstabData.isEmpty();
    }
}
