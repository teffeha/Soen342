package taskmanager.model;
import taskmanager.enums.RecurrenceType;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

public class RecurrencePattern {
    private static long idCounter = 1;

    private final long          recurrenceId;
    private final RecurrenceType type;
    private final int           interval;
    private final LocalDate     startDate;
    private final LocalDate     endDate;       // may be null
    private final Set<DayOfWeek> weekdays;     // WEEKLY only; may be empty
    private final Integer        dayOfMonth;   // MONTHLY only; may be null

    public RecurrencePattern(RecurrenceType type, int interval,
                             LocalDate startDate, LocalDate endDate,
                             Set<DayOfWeek> weekdays, Integer dayOfMonth) {
        this.recurrenceId = idCounter++;
        this.type         = type;
        this.interval     = interval;
        this.startDate    = startDate;
        this.endDate      = endDate;
        this.weekdays     = weekdays != null ? EnumSet.copyOf(weekdays.isEmpty()
                             ? EnumSet.noneOf(DayOfWeek.class) : weekdays)
                           : EnumSet.noneOf(DayOfWeek.class);
        this.dayOfMonth   = dayOfMonth;
    }

    public long           getRecurrenceId() { return recurrenceId; }
    public RecurrenceType getType()         { return type; }
    public int            getInterval()     { return interval; }
    public LocalDate      getStartDate()    { return startDate; }
    public LocalDate      getEndDate()      { return endDate; }
    public Set<DayOfWeek> getWeekdays()     { return weekdays; }
    public Integer        getDayOfMonth()   { return dayOfMonth; }

    @Override
    public String toString() {
        return "RecurrencePattern{type=" + type + ", interval=" + interval
             + ", start=" + startDate
             + (endDate != null ? ", end=" + endDate : "")
             + (!weekdays.isEmpty() ? ", weekdays=" + weekdays : "")
             + (dayOfMonth != null ? ", dayOfMonth=" + dayOfMonth : "")
             + "}";
    }
}
