import java.text.SimpleDateFormat;
import java.util.Date;

public class FilterOperations {

    public static boolean applyOperator(String operator, String rowValue, String filterValue) {
        return applyOperator(operator, rowValue, filterValue, null);
    }

    public static boolean applyOperator(String operator, String rowValue, String filterValue, String filterValue2) {
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
                if (filterValue2 == null || filterValue2.isEmpty()) {
                    return false;
                }
                return betweenComparison(rowValue, filterValue, filterValue2);

            case "LessThan":
            case "LessOrEqual":
            case "GreaterThan":
            case "GreaterOrEqual":
                return compareValues(operator, rowValue, filterValue);

            default:
                return rowValue.equals(filterValue);
        }
    }

    private static boolean betweenComparison(String rowValue, String fromValue, String toValue) {
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
                return false;
            }
        }
    }

    private static boolean compareValues(String operator, String rowValue, String filterValue) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            sdf.setLenient(false);
            Date rowDate = sdf.parse(rowValue);
            Date filterDate = sdf.parse(filterValue);

            return compareDates(operator, rowDate, filterDate);
        } catch (Exception dateEx) {
            try {
                double rowNum = Double.parseDouble(rowValue);
                double compareNum = Double.parseDouble(filterValue);

                return compareNumbers(operator, rowNum, compareNum);
            } catch (Exception numEx) {
                return false;
            }
        }
    }

    private static boolean compareDates(String operator, Date rowDate, Date filterDate) {
        switch (operator) {
            case "LessThan":
                return rowDate.before(filterDate);
            case "LessOrEqual":
                return rowDate.before(filterDate) || rowDate.equals(filterDate);
            case "GreaterThan":
                return rowDate.after(filterDate);
            case "GreaterOrEqual":
                return rowDate.after(filterDate) || rowDate.equals(filterDate);
            default:
                return false;
        }
    }

    private static boolean compareNumbers(String operator, double rowNum, double compareNum) {
        switch (operator) {
            case "LessThan":
                return rowNum < compareNum;
            case "LessOrEqual":
                return rowNum <= compareNum;
            case "GreaterThan":
                return rowNum > compareNum;
            case "GreaterOrEqual":
                return rowNum >= compareNum;
            default:
                return false;
        }
    }
}