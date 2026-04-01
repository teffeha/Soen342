package taskmanager.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityEntry {
    private static long idCounter = 1;

    private final long entryId;
    private final LocalDateTime timestamp;
    private final String description;

    public ActivityEntry(String description) {
        this.entryId = idCounter++;
        this.timestamp = LocalDateTime.now();
        this.description = description;
    }

    public long getEntryId() {
        return entryId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "[" + timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "] " + description;
    }
}
