package taskmanager.repository;

import taskmanager.enums.PriorityLevel;
import taskmanager.enums.RecurrenceType;
import taskmanager.enums.TaskStatus;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Table Data Gateway for the tasks table and all dependent entity tables:
 * subtasks, tags, activity_entries, recurrence_patterns,
 * task_occurrences, task_collaborators.
 */
public class TaskRepository {

    private final Connection conn;

    public TaskRepository(Connection conn) {
        this.conn = conn;
    }

    // ── TASK INSERT / UPDATE ─────────────────────────────────────────────────

    public void insertTask(long taskId, String title, String description,
                           LocalDate creationDate, LocalDate dueDate,
                           PriorityLevel priority, TaskStatus status,
                           Long projectId) throws SQLException {
        String sql =
            "INSERT INTO tasks(task_id, title, description, creation_date, " +
            "due_date, priority, status, project_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.setString(2, title);
            ps.setString(3, description);
            ps.setString(4, creationDate.toString());
            if (dueDate != null) ps.setString(5, dueDate.toString());
            else ps.setNull(5, Types.VARCHAR);
            ps.setString(6, priority.name());
            ps.setString(7, status.name());
            if (projectId != null) ps.setLong(8, projectId);
            else ps.setNull(8, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public void updateTask(long taskId, String title, String description,
                           LocalDate dueDate, PriorityLevel priority,
                           TaskStatus status, Long projectId) throws SQLException {
        String sql =
            "UPDATE tasks SET title = ?, description = ?, due_date = ?, " +
            "priority = ?, status = ?, project_id = ? WHERE task_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, description);
            if (dueDate != null) ps.setString(3, dueDate.toString());
            else ps.setNull(3, Types.VARCHAR);
            ps.setString(4, priority.name());
            ps.setString(5, status.name());
            if (projectId != null) ps.setLong(6, projectId);
            else ps.setNull(6, Types.INTEGER);
            ps.setLong(7, taskId);
            ps.executeUpdate();
        }
    }

    // ── SUBTASK ───────────────────────────────────────────────────────────────

    public void insertSubtask(long subtaskId, long taskId, String title,
                              boolean completed, Long collaboratorId) throws SQLException {
        String sql =
            "INSERT INTO subtasks(subtask_id, task_id, title, completed, collaborator_id) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, subtaskId);
            ps.setLong(2, taskId);
            ps.setString(3, title);
            ps.setInt(4, completed ? 1 : 0);
            if (collaboratorId != null) ps.setLong(5, collaboratorId);
            else ps.setNull(5, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    public void updateSubtaskCompleted(long subtaskId, boolean completed) throws SQLException {
        String sql = "UPDATE subtasks SET completed = ? WHERE subtask_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, completed ? 1 : 0);
            ps.setLong(2, subtaskId);
            ps.executeUpdate();
        }
    }

    // ── TAG ───────────────────────────────────────────────────────────────────

    public void insertTag(long tagId, long taskId, String keyword) throws SQLException {
        String sql = "INSERT INTO tags(tag_id, task_id, keyword) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tagId);
            ps.setLong(2, taskId);
            ps.setString(3, keyword);
            ps.executeUpdate();
        }
    }

    public void deleteTagByKeyword(long taskId, String keyword) throws SQLException {
        String sql = "DELETE FROM tags WHERE task_id = ? AND LOWER(keyword) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.setString(2, keyword);
            ps.executeUpdate();
        }
    }

    public void deleteAllTagsForTask(long taskId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM tags WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            ps.executeUpdate();
        }
    }

    // ── ACTIVITY ENTRY ────────────────────────────────────────────────────────

    public void insertActivityEntry(long entryId, long taskId,
                                    LocalDateTime timestamp, String description) throws SQLException {
        String sql =
            "INSERT INTO activity_entries(entry_id, task_id, timestamp, description) " +
            "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, entryId);
            ps.setLong(2, taskId);
            ps.setString(3, timestamp.toString());
            ps.setString(4, description);
            ps.executeUpdate();
        }
    }

    // ── RECURRENCE PATTERN ────────────────────────────────────────────────────

    public void insertRecurrencePattern(long recurrenceId, long taskId,
                                        RecurrenceType type, int interval,
                                        LocalDate startDate, LocalDate endDate,
                                        Set<DayOfWeek> weekdays,
                                        Integer dayOfMonth) throws SQLException {
        String sql =
            "INSERT INTO recurrence_patterns" +
            "(recurrence_id, task_id, type, interval_val, start_date, end_date, weekdays, day_of_month) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recurrenceId);
            ps.setLong(2, taskId);
            ps.setString(3, type.name());
            ps.setInt(4, interval);
            ps.setString(5, startDate.toString());
            if (endDate != null) ps.setString(6, endDate.toString());
            else ps.setNull(6, Types.VARCHAR);
            if (weekdays != null && !weekdays.isEmpty())
                ps.setString(7, weekdaysToString(weekdays));
            else ps.setNull(7, Types.VARCHAR);
            if (dayOfMonth != null) ps.setInt(8, dayOfMonth);
            else ps.setNull(8, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    // ── TASK OCCURRENCE ───────────────────────────────────────────────────────

    public void insertOccurrence(long occurrenceId, long taskId,
                                 LocalDate dueDate, TaskStatus status) throws SQLException {
        String sql =
            "INSERT INTO task_occurrences(occurrence_id, task_id, due_date, status) " +
            "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, occurrenceId);
            ps.setLong(2, taskId);
            ps.setString(3, dueDate.toString());
            ps.setString(4, status.name());
            ps.executeUpdate();
        }
    }

    public void updateOccurrenceStatus(long occurrenceId, TaskStatus status) throws SQLException {
        String sql = "UPDATE task_occurrences SET status = ? WHERE occurrence_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setLong(2, occurrenceId);
            ps.executeUpdate();
        }
    }

    // ── TASK-COLLABORATOR LINK ────────────────────────────────────────────────

    public void insertCollaboratorLink(long taskId, long collaboratorId) throws SQLException {
        String sql =
            "INSERT OR IGNORE INTO task_collaborators(task_id, collaborator_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            ps.setLong(2, collaboratorId);
            ps.executeUpdate();
        }
    }

    // ── LOAD ALL ──────────────────────────────────────────────────────────────

    public List<TaskRow> loadAllTasks() throws SQLException {
        List<TaskRow> rows = new ArrayList<>();
        String sql =
            "SELECT task_id, title, description, creation_date, due_date, " +
            "priority, status, project_id FROM tasks";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String dueDateStr = rs.getString("due_date");
                long rawProjId = rs.getLong("project_id");
                Long projectId = rs.wasNull() ? null : rawProjId;
                rows.add(new TaskRow(
                        rs.getLong("task_id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        LocalDate.parse(rs.getString("creation_date")),
                        dueDateStr != null ? LocalDate.parse(dueDateStr) : null,
                        PriorityLevel.valueOf(rs.getString("priority")),
                        TaskStatus.valueOf(rs.getString("status")),
                        projectId));
            }
        }
        return rows;
    }

    public List<SubtaskRow> loadSubtasksForTask(long taskId) throws SQLException {
        List<SubtaskRow> rows = new ArrayList<>();
        String sql =
            "SELECT subtask_id, title, completed, collaborator_id " +
            "FROM subtasks WHERE task_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long rawCollabId = rs.getLong("collaborator_id");
                    Long collaboratorId = rs.wasNull() ? null : rawCollabId;
                    rows.add(new SubtaskRow(
                            rs.getLong("subtask_id"),
                            rs.getString("title"),
                            rs.getInt("completed") == 1,
                            collaboratorId));
                }
            }
        }
        return rows;
    }

    public List<TagRow> loadTagsForTask(long taskId) throws SQLException {
        List<TagRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tag_id, keyword FROM tags WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new TagRow(rs.getLong("tag_id"), rs.getString("keyword")));
                }
            }
        }
        return rows;
    }

    public List<ActivityRow> loadActivityForTask(long taskId) throws SQLException {
        List<ActivityRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT entry_id, timestamp, description FROM activity_entries WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ActivityRow(
                            rs.getLong("entry_id"),
                            LocalDateTime.parse(rs.getString("timestamp")),
                            rs.getString("description")));
                }
            }
        }
        return rows;
    }

    public Optional<RecurrenceRow> loadRecurrenceForTask(long taskId) throws SQLException {
        String sql =
            "SELECT recurrence_id, type, interval_val, start_date, end_date, " +
            "weekdays, day_of_month FROM recurrence_patterns WHERE task_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String endDateStr  = rs.getString("end_date");
                    String weekdaysStr = rs.getString("weekdays");
                    int rawDom = rs.getInt("day_of_month");
                    Integer dayOfMonth = rs.wasNull() ? null : rawDom;
                    return Optional.of(new RecurrenceRow(
                            rs.getLong("recurrence_id"),
                            RecurrenceType.valueOf(rs.getString("type")),
                            rs.getInt("interval_val"),
                            LocalDate.parse(rs.getString("start_date")),
                            endDateStr  != null ? LocalDate.parse(endDateStr)  : null,
                            weekdaysStr != null ? weekdaysFromString(weekdaysStr) : Collections.emptySet(),
                            dayOfMonth));
                }
            }
        }
        return Optional.empty();
    }

    public List<OccurrenceRow> loadOccurrencesForTask(long taskId) throws SQLException {
        List<OccurrenceRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT occurrence_id, due_date, status FROM task_occurrences WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OccurrenceRow(
                            rs.getLong("occurrence_id"),
                            LocalDate.parse(rs.getString("due_date")),
                            TaskStatus.valueOf(rs.getString("status"))));
                }
            }
        }
        return rows;
    }

    public List<Long> loadCollaboratorIdsForTask(long taskId) throws SQLException {
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT collaborator_id FROM task_collaborators WHERE task_id = ?")) {
            ps.setLong(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getLong("collaborator_id"));
            }
        }
        return ids;
    }

    // ── MAX IDs (for counter sync on startup) ─────────────────────────────────

    public long maxTaskId()       throws SQLException { return maxId("tasks",               "task_id"); }
    public long maxSubtaskId()    throws SQLException { return maxId("subtasks",            "subtask_id"); }
    public long maxTagId()        throws SQLException { return maxId("tags",                "tag_id"); }
    public long maxActivityId()   throws SQLException { return maxId("activity_entries",    "entry_id"); }
    public long maxOccurrenceId() throws SQLException { return maxId("task_occurrences",    "occurrence_id"); }
    public long maxRecurrenceId() throws SQLException { return maxId("recurrence_patterns", "recurrence_id"); }

    private long maxId(String table, String col) throws SQLException {
        String sql = "SELECT COALESCE(MAX(" + col + "), 0) FROM " + table;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private String weekdaysToString(Set<DayOfWeek> days) {
        return days.stream()
                .map(DayOfWeek::name)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private Set<DayOfWeek> weekdaysFromString(String s) {
        Set<DayOfWeek> days = new HashSet<>();
        for (String token : s.split(",")) {
            String t = token.trim();
            if (!t.isEmpty()) days.add(DayOfWeek.valueOf(t));
        }
        return days;
    }

    // ── ROW TYPES ─────────────────────────────────────────────────────────────

    public record TaskRow(long taskId, String title, String description,
                          LocalDate creationDate, LocalDate dueDate,
                          PriorityLevel priority, TaskStatus status,
                          Long projectId) {}

    public record SubtaskRow(long subtaskId, String title,
                             boolean completed, Long collaboratorId) {}

    public record TagRow(long tagId, String keyword) {}

    public record ActivityRow(long entryId, LocalDateTime timestamp,
                              String description) {}

    public record RecurrenceRow(long recurrenceId, RecurrenceType type,
                                int interval, LocalDate startDate,
                                LocalDate endDate, Set<DayOfWeek> weekdays,
                                Integer dayOfMonth) {}

    public record OccurrenceRow(long occurrenceId, LocalDate dueDate,
                                TaskStatus status) {}
}
