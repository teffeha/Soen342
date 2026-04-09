package taskmanager.model;

import taskmanager.enums.RecurrenceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class RecurrencePattern {
    private static long idCounter = 1;

    private final long recurrenceId;
    private final RecurrenceType type;
    private final int interval;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final Set<DayOfWeek> weekdays;
    private final Integer dayOfMonth;

    /** Constructor for new recurrence patterns. */
    public RecurrencePattern(RecurrenceType type, int interval,
                             LocalDate startDate, LocalDate endDate,
                             Set<DayOfWeek> weekdays, Integer dayOfMonth) {
        this.recurrenceId = idCounter++;
        this.type         = type;
        this.interval     = interval;
        this.startDate    = startDate;
        this.endDate      = endDate;
        this.weekdays     = toImmutableSet(weekdays);
        this.dayOfMonth   = dayOfMonth;
    }

    /** Constructor for loading an existing recurrence pattern from the database. */
    public RecurrencePattern(long recurrenceId, RecurrenceType type, int interval,
                             LocalDate startDate, LocalDate endDate,
                             Set<DayOfWeek> weekdays, Integer dayOfMonth) {
        this.recurrenceId = recurrenceId;
        this.type         = type;
        this.interval     = interval;
        this.startDate    = startDate;
        this.endDate      = endDate;
        this.weekdays     = toImmutableSet(weekdays);
        this.dayOfMonth   = dayOfMonth;
        if (recurrenceId >= idCounter) idCounter = recurrenceId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    private static Set<DayOfWeek> toImmutableSet(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) return EnumSet.noneOf(DayOfWeek.class);
        return Collections.unmodifiableSet(EnumSet.copyOf(days));
    }

    public long          getRecurrenceId() { return recurrenceId; }
    public RecurrenceType getType()        { return type; }
    public int            getInterval()    { return interval; }
    public LocalDate      getStartDate()   { return startDate; }
    public LocalDate      getEndDate()     { return endDate; }
    public Set<DayOfWeek> getWeekdays()    { return weekdays; }
    public Integer        getDayOfMonth()  { return dayOfMonth; }

    @Override
    public String toString() {
        return "RecurrencePattern{type=" + type + ", interval=" + interval
             + ", start=" + startDate
             + (endDate      != null ? ", end=" + endDate       : "")
             + (!weekdays.isEmpty()  ? ", weekdays=" + weekdays  : "")
             + (dayOfMonth   != null ? ", dayOfMonth=" + dayOfMonth : "")
             + "}";
    }
}
