package taskmanager.model;
import taskmanager.enums.TaskStatus;
import java.time.LocalDate;

public class TaskOccurrence {
    private static long idCounter = 1;

    private final long      occurrenceId;
    private final LocalDate dueDate;
    private       TaskStatus status;

    public TaskOccurrence(LocalDate dueDate) {
        this.occurrenceId = idCounter++;
        this.dueDate      = dueDate;
        this.status       = TaskStatus.OPEN;
    }

    public long       getOccurrenceId() { return occurrenceId; }
    public LocalDate  getDueDate()      { return dueDate; }
    public TaskStatus getStatus()       { return status; }
    public void       setStatus(TaskStatus s) { this.status = s; }

    @Override
    public String toString() {
        return "  Occurrence{id=" + occurrenceId + ", due=" + dueDate + ", status=" + status + "}";
    }
}
