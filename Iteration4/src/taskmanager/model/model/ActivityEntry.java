package taskmanager.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ActivityEntry {
    private static long idCounter = 1;

    private final long entryId;
    private final LocalDateTime timestamp;
    private final String description;

    /** Constructor for new activity entries. */
    public ActivityEntry(String description) {
        this.entryId     = idCounter++;
        this.timestamp   = LocalDateTime.now();
        this.description = description;
    }

    /** Constructor for loading an existing activity entry from the database. */
    public ActivityEntry(long entryId, LocalDateTime timestamp, String description) {
        this.entryId     = entryId;
        this.timestamp   = timestamp;
        this.description = description;
        if (entryId >= idCounter) idCounter = entryId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    public long          getEntryId()    { return entryId; }
    public LocalDateTime getTimestamp()  { return timestamp; }
    public String        getDescription() { return description; }

    @Override
    public String toString() {
        return "[" + timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + "] " + description;
    }
}