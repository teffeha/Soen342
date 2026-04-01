package taskmanager.util;

import taskmanager.model.Task;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ICalGateway {
    private static final DateTimeFormatter STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public void exportToIcs(List<Task> tasks, String filePath) {
        if (tasks == null) {
            throw new IllegalArgumentException("Tasks to export cannot be null.");
        }
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("A valid destination path must be specified.");
        }

        Path path = Paths.get(filePath);
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write("BEGIN:VCALENDAR\r\n");
                writer.write("VERSION:2.0\r\n");
                writer.write("PRODID:-//SOEN342//TaskManager Iteration 3//EN\r\n");
                writer.write("CALSCALE:GREGORIAN\r\n");
                for (Task task : tasks) {
                    writeEvent(writer, task);
                }
                writer.write("END:VCALENDAR\r\n");
            }
        } catch (IOException e) {
            throw new IllegalStateException("iCal export failed: " + e.getMessage(), e);
        }
    }

    private void writeEvent(BufferedWriter writer, Task task) throws IOException {
        LocalDate dueDate = task.getDueDate();
        if (dueDate == null) {
            return;
        }

        String dtStamp = LocalDateTime.now(ZoneOffset.UTC).format(STAMP_FORMAT);
        String dtStart = dueDate.format(DATE_FORMAT);
        String dtEnd = dueDate.plusDays(1).format(DATE_FORMAT);
        String uid = "task-" + task.getTaskId() + "@soen342.local";

        writer.write("BEGIN:VEVENT\r\n");
        writer.write("UID:" + escape(uid) + "\r\n");
        writer.write("DTSTAMP:" + dtStamp + "\r\n");
        writer.write("DTSTART;VALUE=DATE:" + dtStart + "\r\n");
        writer.write("DTEND;VALUE=DATE:" + dtEnd + "\r\n");
        writer.write("SUMMARY:" + foldLine(escape(task.getTitle())) + "\r\n");
        writer.write("DESCRIPTION:" + foldLine(escape(task.toIcsDescription())) + "\r\n");
        writer.write("STATUS:" + mapStatus(task) + "\r\n");
        writer.write("PRIORITY:" + mapPriority(task) + "\r\n");
        if (task.getProject() != null) {
            writer.write("CATEGORIES:" + foldLine(escape(task.getProject().getName())) + "\r\n");
        }
        writer.write("END:VEVENT\r\n");
    }

    private String mapStatus(Task task) {
        switch (task.getStatus()) {
            case COMPLETED:
                return "COMPLETED";
            case CANCELLED:
                return "CANCELLED";
            default:
                return "CONFIRMED";
        }
    }

    private int mapPriority(Task task) {
        switch (task.getPriority()) {
            case HIGH:
                return 1;
            case MEDIUM:
                return 5;
            default:
                return 9;
        }
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    private String foldLine(String line) {
        final int maxLen = 73;
        if (line.length() <= maxLen) {
            return line;
        }
        StringBuilder folded = new StringBuilder();
        int index = 0;
        while (index < line.length()) {
            int end = Math.min(index + maxLen, line.length());
            if (index == 0) {
                folded.append(line, index, end);
            } else {
                folded.append("\r\n ").append(line, index, end);
            }
            index = end;
        }
        return folded.toString();
    }
}
