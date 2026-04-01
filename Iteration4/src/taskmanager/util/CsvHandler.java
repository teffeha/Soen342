package taskmanager.util;

import taskmanager.enums.CollaboratorCategory;
import taskmanager.enums.PriorityLevel;
import taskmanager.enums.TaskStatus;
import taskmanager.model.Collaborator;
import taskmanager.model.Project;
import taskmanager.model.Subtask;
import taskmanager.model.Tag;
import taskmanager.model.Task;
import taskmanager.system.TaskManagementSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CsvHandler {
    private static final String HEADER =
        "TaskId,Title,Description,Status,Priority,CreationDate,DueDate,"
      + "ProjectName,CollaboratorName,CollaboratorCategory,Tags,Subtasks";

    private CsvHandler() {
    }

    public static void export(String filePath,
                              List<Task> tasks,
                              List<Project> projects,
                              List<Collaborator> collaborators) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println(HEADER);
            for (Task task : tasks) {
                pw.println(taskToRow(task));
            }
        } catch (IOException e) {
            throw new IllegalStateException("CSV export failed: " + e.getMessage(), e);
        }
    }

    private static String taskToRow(Task task) {
        String projectName = task.getProject() != null ? task.getProject().getName() : "";
        String collaboratorName = task.getCollaborators().isEmpty() ? ""
                : task.getCollaborators().get(0).getName();
        String collaboratorCategory = task.getCollaborators().isEmpty() ? ""
                : task.getCollaborators().get(0).getCategory().name();
        String tags = String.join(";", task.getTags().stream().map(Tag::getKeyword).toArray(String[]::new));
        String subtasks = String.join(";", task.getSubtasks().stream().map(Subtask::getTitle).toArray(String[]::new));

        return joinCsv(
                String.valueOf(task.getTaskId()),
                escape(task.getTitle()),
                escape(task.getDescription() != null ? task.getDescription() : ""),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getCreationDate().toString(),
                task.getDueDate() != null ? task.getDueDate().toString() : "",
                escape(projectName),
                escape(collaboratorName),
                collaboratorCategory,
                escape(tags),
                escape(subtasks)
        );
    }

    public static int importTasks(String filePath, TaskManagementSystem system) {
        File file = new File(filePath);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException("CSV file not found or unreadable: " + filePath);
        }

        int imported = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV import failed: file is empty.");
            }

            String line;
            int lineNum = 1;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    String[] cols = parseCsvRow(line);
                    if (cols.length < 5) {
                        throw new IllegalArgumentException("Not enough columns.");
                    }

                    int offset = 0;
                    try {
                        Long.parseLong(cols[0].trim());
                        offset = 1;
                    } catch (NumberFormatException ignored) {
                        offset = 0;
                    }

                    String title = safeGet(cols, offset);
                    String description = safeGet(cols, offset + 1);
                    String statusStr = safeGet(cols, offset + 2);
                    String priorityStr = safeGet(cols, offset + 3);
                    String dueDateStr = safeGet(cols, offset + 5);
                    String projectName = safeGet(cols, offset + 6);
                    String collabName = safeGet(cols, offset + 7);
                    String collabCatStr = safeGet(cols, offset + 8);
                    String tagsStr = safeGet(cols, offset + 9);
                    String subtasksStr = safeGet(cols, offset + 10);

                    if (title.isBlank()) {
                        throw new IllegalArgumentException("Title is required.");
                    }

                    PriorityLevel priority;
                    try {
                        priority = PriorityLevel.valueOf(priorityStr.toUpperCase());
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid priority: " + priorityStr);
                    }

                    LocalDate dueDate = null;
                    if (!dueDateStr.isBlank()) {
                        try {
                            dueDate = LocalDate.parse(dueDateStr.trim());
                        } catch (Exception e) {
                            System.err.println("  Warning line " + lineNum
                                    + ": invalid due date '" + dueDateStr + "' — ignored.");
                        }
                    }

                    Long projectId = null;
                    if (!projectName.isBlank()) {
                        Project project = system.findProjectByName(projectName);
                        if (project == null) {
                            projectId = system.createProject(projectName, "");
                        } else {
                            projectId = project.getProjectId();
                        }
                    }

                    Long collaboratorId = null;
                    if (!collabName.isBlank()) {
                        Collaborator collaborator = system.findCollaboratorByName(collabName);
                        if (collaborator == null) {
                            CollaboratorCategory category = CollaboratorCategory.JUNIOR;
                            if (!collabCatStr.isBlank()) {
                                try {
                                    category = CollaboratorCategory.valueOf(collabCatStr.toUpperCase());
                                } catch (Exception ignored) {
                                }
                            }
                            collaboratorId = system.createCollaborator(collabName, category);
                        } else {
                            collaboratorId = collaborator.getCollaboratorId();
                        }
                        if (projectId != null) {
                            try {
                                system.assignCollaboratorToProject(collaboratorId, projectId);
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    long taskId = system.createTask(title, description, priority, null);

                    if (projectId != null) {
                        try {
                            system.assignTaskToProject(taskId, projectId);
                        } catch (Exception e) {
                            System.err.println("  Warning line " + lineNum
                                    + ": could not assign to project — " + e.getMessage());
                        }
                    }

                    if (dueDate != null) {
                        try {
                            system.updateTask(taskId, "dueDate", dueDate);
                        } catch (Exception ignored) {
                        }
                    }

                    if (!statusStr.isBlank()) {
                        try {
                            TaskStatus status = TaskStatus.valueOf(statusStr.toUpperCase());
                            if (status != TaskStatus.OPEN) {
                                system.updateTask(taskId, "status", status);
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (!tagsStr.isBlank()) {
                        for (String kw : tagsStr.split(";")) {
                            if (!kw.isBlank()) {
                                try {
                                    system.addTagToTask(taskId, kw.trim());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    if (!subtasksStr.isBlank()) {
                        for (String st : subtasksStr.split(";")) {
                            if (!st.isBlank()) {
                                try {
                                    system.addSubtask(taskId, st.trim());
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    if (collaboratorId != null && projectId != null) {
                        try {
                            system.assignCollaboratorToTask(taskId, collaboratorId);
                        } catch (Exception e) {
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
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String joinCsv(String... parts) {
        return String.join(",", parts);
    }

    private static String[] parseCsvRow(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static String safeGet(String[] arr, int idx) {
        if (idx < 0 || idx >= arr.length) {
            return "";
        }
        return arr[idx] != null ? arr[idx].trim() : "";
    }
}
