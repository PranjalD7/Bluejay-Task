import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


class EmployeeShift implements Comparable<EmployeeShift> {
    private String employeeName;
    private String positionId;
    private Date timeIn;
    private Date timeOut;

    public EmployeeShift(String employeeName, String positionId, Date timeIn, Date timeOut) {
        this.employeeName = employeeName;
        this.positionId = positionId;
        this.timeIn = timeIn;
        this.timeOut = timeOut;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public Date getTimeIn() {
        return timeIn;
    }

    public Date getTimeOut() {
        return timeOut;
    }

    @Override
    public int compareTo(EmployeeShift other) {
        return this.timeIn.compareTo(other.timeIn);
    }
}


public class TimecardAnalysis {

    private static final String CSV_FILE_PATH = "C:\\Users\\Pranjal\\Desktop\\Test\\Assignment_Timecard.xlsx - Sheet1.csv";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm a");

    public static void main(String[] args) {
        List<EmployeeShift> shifts = loadEmployeeShifts(CSV_FILE_PATH);

        // Analyzing the data
        Map<String, List<EmployeeShift>> employeeShiftsMap = organizeShiftsByEmployee(shifts);
        analyzeEmployeeShifts(employeeShiftsMap);
    }

    private static List<EmployeeShift> loadEmployeeShifts(String filePath) {
        List<EmployeeShift> shifts = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
           
            br.readLine(); // Skipping the header
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                try {
                    String positionId = values[0].trim();
                    String employeeName = values[7].replace("\"", "").trim(); // Remove quotes
                    Date timeIn = dateFormat.parse(values[2].trim());
                    Date timeOut = dateFormat.parse(values[3].trim());
                    shifts.add(new EmployeeShift(employeeName, positionId, timeIn, timeOut));
                } catch (ParseException e) {
                    System.err.println("Error parsing date: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return shifts;
    }

    private static Map<String, List<EmployeeShift>> organizeShiftsByEmployee(List<EmployeeShift> shifts) {
        Map<String, List<EmployeeShift>> employeeShiftsMap = new HashMap<>();
        for (EmployeeShift shift : shifts) {
            employeeShiftsMap.computeIfAbsent(shift.getEmployeeName(), k -> new ArrayList<>()).add(shift);
        }
        return employeeShiftsMap;
    }

    private static void analyzeEmployeeShifts(Map<String, List<EmployeeShift>> employeeShiftsMap) {
        try (PrintWriter out = new PrintWriter(new FileWriter("output.txt"))) {
            for (Map.Entry<String, List<EmployeeShift>> entry : employeeShiftsMap.entrySet()) {
                String employeeName = entry.getKey();
                List<EmployeeShift> shifts = entry.getValue();
                Collections.sort(shifts);
    
                // Criteria a: Check for 7 consecutive days
                if (hasWorkedConsecutiveDays(shifts, 7)) {
                    out.println(employeeName + " has worked for 7 consecutive days.");
                }
    
                // Criteria b: Check for shifts with less than 10 hours but more than 1 hour between shifts
                if (hasShortBreakBetweenShifts(shifts)) {
                    out.println(employeeName + " has less than 10 hours but more than 1 hour between shifts.");
                }
    
                // Criteria c: Check for shifts longer than 14 hours
                for (EmployeeShift shift : shifts) {
                    if (getHoursBetweenDates(shift.getTimeIn(), shift.getTimeOut()) > 14) {
                        out.println(employeeName + " has worked more than 14 hours in a single shift.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
    

    private static boolean hasWorkedConsecutiveDays(List<EmployeeShift> shifts, int days) {
        if (shifts.size() < days) {
            return false;
        }
    
        int consecutiveDays = 1;
        Date lastDate = shifts.get(0).getTimeIn();
    
        for (int i = 1; i < shifts.size(); i++) {
            Date currentDate = shifts.get(i).getTimeIn();
            if (isNextDay(lastDate, currentDate)) {
                consecutiveDays++;
                if (consecutiveDays >= days) return true;
            } else if (!isSameDay(lastDate, currentDate)) {
                consecutiveDays = 1;
            }
            lastDate = currentDate;
        }
    
        return false;
    }
    
    private static boolean isNextDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        cal1.add(Calendar.DAY_OF_YEAR, 1);
        return isSameDay(cal1.getTime(), date2);
    }
    
    private static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
    
    private static boolean hasShortBreakBetweenShifts(List<EmployeeShift> shifts) {
        for (int i = 0; i < shifts.size() - 1; i++) {
            Date endTime = shifts.get(i).getTimeOut();
            Date nextStartTime = shifts.get(i + 1).getTimeIn();
    
            double hoursBetween = getHoursBetweenDates(endTime, nextStartTime);
            if (hoursBetween > 1 && hoursBetween < 10) {
                return true;
            }
        }
        return false;
    }
    
    private static double getHoursBetweenDates(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        return diff / (double) (60 * 60 * 1000);
    }
}
