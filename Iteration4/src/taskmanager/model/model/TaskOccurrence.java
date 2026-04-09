package taskmanager.model;

import taskmanager.enums.TaskStatus;

import java.time.LocalDate;

public class TaskOccurrence {
    private static long idCounter = 1;

    private final long occurrenceId;
    private final LocalDate dueDate;
    private TaskStatus status;

    /** Constructor for new occurrences. */
    public TaskOccurrence(LocalDate dueDate) {
        this.occurrenceId = idCounter++;
        this.dueDate      = dueDate;
        this.status       = TaskStatus.OPEN;
    }

    /** Constructor for loading an existing occurrence from the database. */
    public TaskOccurrence(long occurrenceId, LocalDate dueDate, TaskStatus status) {
        this.occurrenceId = occurrenceId;
        this.dueDate      = dueDate;
        this.status       = status;
        if (occurrenceId >= idCounter) idCounter = occurrenceId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    public long       getOccurrenceId() { return occurrenceId; }
    public LocalDate  getDueDate()      { return dueDate; }
    public TaskStatus getStatus()       { return status; }
    public void       setStatus(TaskStatus s) { this.status = s; }

    @Override
    public String toString() {
        return "Occurrence{id=" + occurrenceId + ", due=" + dueDate + ", status=" + status + "}";
    }
}