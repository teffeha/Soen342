package taskmanager.util;
import taskmanager.enums.CollaboratorCategory;
import taskmanager.enums.PriorityLevel;
import taskmanager.enums.TaskStatus;
import taskmanager.model.*;
import taskmanager.system.TaskManagementSystem;
import java.io.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Handles CSV import and export for the task management system.
 *
 * CSV format (one row per task):
 *   TaskId, Title, Description, Status, Priority, CreationDate, DueDate,
 *   ProjectName, CollaboratorName, CollaboratorCategory,
 *   Tags (semicolon-separated), Subtasks (semicolon-separated)
 */
public class CsvHandler {

    private static final String HEADER =
        "TaskId,Title,Description,Status,Priority,CreationDate,DueDate,"
      + "ProjectName,CollaboratorName,CollaboratorCategory,Tags,Subtasks";
    // Export
    public static void export(String filePath,
                              List<Task> tasks,
                              List<Project> projects,
                              List<Collaborator> collaborators) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println(HEADER);
            for (Task t : tasks) {
                pw.println(taskToRow(t));
            }
        } catch (IOException e) {
            throw new IllegalStateException("CSV export failed: " + e.getMessage(), e);
        }
    }

    private static String taskToRow(Task t) {
        String projectName = t.getProject() != null ? t.getProject().getName() : "";
        String collabName  = t.getCollaborators().isEmpty() ? ""
                           : t.getCollaborators().get(0).getName();
        String collabCat   = t.getCollaborators().isEmpty() ? ""
                           : t.getCollaborators().get(0).getCategory().name();
        String tags = String.join(";",
            t.getTags().stream().map(Tag::getKeyword).toArray(String[]::new));
        String subtasks = String.join(";",
            t.getSubtasks().stream().map(Subtask::getTitle).toArray(String[]::new));

        return joinCsv(
            String.valueOf(t.getTaskId()),
            escape(t.getTitle()),
            escape(t.getDescription() != null ? t.getDescription() : ""),
            t.getStatus().name(),
            t.getPriority().name(),
            t.getCreationDate().toString(),
            t.getDueDate() != null ? t.getDueDate().toString() : "",
            escape(projectName),
            escape(collabName),
            collabCat,
            escape(tags),
            escape(subtasks)
        );
    }
    // Import
    /**
     * Reads the CSV file and creates tasks (and related projects/collaborators)
     * in the system.  Rows with missing required fields are skipped with a warning.
     *
     * Required columns: Title, Priority (Status defaults to OPEN if blank).
     *
     * @return number of tasks successfully imported
     */
    public static int importTasks(String filePath, TaskManagementSystem system) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException("CSV file not found or unreadable: " + filePath);

        int imported = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null)
                throw new IllegalArgumentException("CSV import failed: file is empty.");

            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                try {
                    String[] cols = parseCsvRow(line);
                    if (cols.length < 5)
                        throw new IllegalArgumentException("Not enough columns.");

                    // columns: Title(0), Description(1), Status(2), Priority(3),
                    //          CreationDate(4), DueDate(5), ProjectName(6),
                    //          CollaboratorName(7), CollaboratorCategory(8),
                    //          Tags(9), Subtasks(10)
                    // (first col is TaskId from export — skip it; shift by 1 when reading)
                    // We support both with-id and without-id formats.
                    int offset = 0;
                    try { Long.parseLong(cols[0].trim()); offset = 1; }
                    catch (NumberFormatException ignored) { offset = 0; }

                    String title       = safeGet(cols, offset);
                    String description = safeGet(cols, offset + 1);
                    String statusStr   = safeGet(cols, offset + 2);
                    String priorityStr = safeGet(cols, offset + 3);
                    String dueDateStr  = safeGet(cols, offset + 5);
                    String projectName = safeGet(cols, offset + 6);
                    String collabName  = safeGet(cols, offset + 7);
                    String collabCatStr= safeGet(cols, offset + 8);
                    String tagsStr     = safeGet(cols, offset + 9);
                    String subtasksStr = safeGet(cols, offset + 10);

                    if (title.isBlank())
                        throw new IllegalArgumentException("Title is required.");

                    PriorityLevel priority;
                    try { priority = PriorityLevel.valueOf(priorityStr.toUpperCase()); }
                    catch (Exception e) {
                        throw new IllegalArgumentException("Invalid priority: " + priorityStr);
                    }

                    LocalDate dueDate = null;
                    if (!dueDateStr.isBlank()) {
                        try { dueDate = LocalDate.parse(dueDateStr.trim()); }
                        catch (Exception e) {
                            System.err.println("  Warning line " + lineNum
                                + ": invalid due date '" + dueDateStr + "' — ignored.");
                        }
                    }

                    // find or create project
                    Long projectId = null;
                    if (!projectName.isBlank()) {
                        Project p = system.findProjectByName(projectName);
                        if (p == null) {
                            projectId = system.createProject(projectName, "");
                        } else {
                            projectId = p.getProjectId();
                        }
                    }

                    // find or create collaborator
                    Long collaboratorId = null;
                    if (!collabName.isBlank()) {
                        Collaborator c = system.findCollaboratorByName(collabName);
                        if (c == null) {
                            CollaboratorCategory cat = CollaboratorCategory.JUNIOR;
                            if (!collabCatStr.isBlank()) {
                                try { cat = CollaboratorCategory.valueOf(collabCatStr.toUpperCase()); }
                                catch (Exception ignored) {}
                            }
                            collaboratorId = system.createCollaborator(collabName, cat);
                        } else {
                            collaboratorId = c.getCollaboratorId();
                        }
                        // assign collaborator to project if both exist
                        if (projectId != null) {
                            try { system.assignCollaboratorToProject(collaboratorId, projectId); }
                            catch (Exception ignored) {}
                        }
                    }

                    // create the task (bypass past-date check for imports)
                    long taskId = system.createTask(title, description, priority, null);

                    // assign to project
                    if (projectId != null) {
                        try { system.assignTaskToProject(taskId, projectId); }
                        catch (Exception e) {
                            System.err.println("  Warning line " + lineNum
                                + ": could not assign to project — " + e.getMessage());
                        }
                    }

                    // set due date via updateTask
                    if (dueDate != null) {
                        try { system.updateTask(taskId, "dueDate", dueDate); }
                        catch (Exception ignored) {}
                    }

                    // set status
                    if (!statusStr.isBlank()) {
                        try {
                            TaskStatus s = TaskStatus.valueOf(statusStr.toUpperCase());
                            if (s != TaskStatus.OPEN)
                                system.updateTask(taskId, "status", s);
                        } catch (Exception ignored) {}
                    }

                    // add tags
                    if (!tagsStr.isBlank()) {
                        for (String kw : tagsStr.split(";"))
                            if (!kw.isBlank())
                                try { system.addTagToTask(taskId, kw.trim()); }
                                catch (Exception ignored) {}
                    }

                    // add subtasks
                    if (!subtasksStr.isBlank()) {
                        for (String st : subtasksStr.split(";"))
                            if (!st.isBlank())
                                try { system.addSubtask(taskId, st.trim()); }
                                catch (Exception ignored) {}
                    }

                    // assign collaborator to task
                    if (collaboratorId != null && projectId != null) {
                        try { system.assignCollaboratorToTask(taskId, collaboratorId); }
                        catch (Exception e) {
                            System.err.println("  Info line " + lineNum
                                + ": collaborator not assigned to task — " + e.getMessage());
                        }
                    }

                    imported++;

                } catch (Exception e) {
                    System.err.println("  Skipping line " + lineNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("CSV import failed: " + e.getMessage(), e);
        }
        return imported;
    }
    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String joinCsv(String... parts) {
        return String.join(",", parts);
    }

    private static String[] parseCsvRow(String line) {
        // Simple CSV parser that handles quoted fields
        List<String> fields = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                fields.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        fields.add(cur.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static String safeGet(String[] arr, int idx) {
        if (idx < 0 || idx >= arr.length) return "";
        return arr[idx] != null ? arr[idx].trim() : "";
    }
}
