package taskmanager.cli;

import taskmanager.enums.CollaboratorCategory;
import taskmanager.enums.ExportType;
import taskmanager.enums.PriorityLevel;
import taskmanager.enums.RecurrenceType;
import taskmanager.enums.TaskStatus;
import taskmanager.model.Collaborator;
import taskmanager.model.Project;
import taskmanager.model.SearchCriteria;
import taskmanager.model.Task;
import taskmanager.system.TaskManagementSystem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class CLI {
    private final TaskManagementSystem system;
    private final Scanner scanner;

    public CLI(TaskManagementSystem system) {
        this.system = system;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║ Personal Task Management System  v1.3    ║");
        System.out.println("║ SOEN 342 — Iteration III Build           ║");
        System.out.println("╚══════════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Choice: ");
            try {
                switch (choice) {
                    case 1:  doCreateTask(); break;
                    case 2:  doUpdateTask(); break;
                    case 3:  doAddSubtask(); break;
                    case 4:  doCreateRecurringTask(); break;
                    case 5:  doCreateProject(); break;
                    case 6:  doAssignTaskToProject(); break;
                    case 7:  doRemoveTaskFromProject(); break;
                    case 8:  doCreateCollaborator(); break;
                    case 9:  doAssignCollaboratorToProject(); break;
                    case 10: doAssignCollaboratorToTask(); break;
                    case 11: doSearchTasks(); break;
                    case 12: doViewTask(); break;
                    case 13: doListProjects(); break;
                    case 14: doListCollaborators(); break;
                    case 15: doAddTag(); break;
                    case 16: doRemoveTag(); break;
                    case 17: doExportCsv(); break;
                    case 18: doImportCsv(); break;
                    case 19: doExportIcal(); break;
                    case 20: doViewOverloadedCollaborators(); break;
                    case 0:  running = false; break;
                    default:
                        System.out.println("  Unknown option. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("  ✗ Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("── Tasks ─────────────────────────────────────");
        System.out.println("  1.  Create Task");
        System.out.println("  2.  Update Task");
        System.out.println("  3.  Add Subtask");
        System.out.println("  4.  Create Recurring Task");
        System.out.println("── Projects ──────────────────────────────────");
        System.out.println("  5.  Create Project");
        System.out.println("  6.  Assign Task to Project");
        System.out.println("  7.  Remove Task from Project");
        System.out.println("── Collaborators ─────────────────────────────");
        System.out.println("  8.  Create Collaborator");
        System.out.println("  9.  Assign Collaborator to Project");
        System.out.println(" 10.  Assign Collaborator to Task");
        System.out.println("── Search & View ─────────────────────────────");
        System.out.println(" 11.  Search Tasks");
        System.out.println(" 12.  View Task Detail");
        System.out.println(" 13.  List All Projects");
        System.out.println(" 14.  List All Collaborators");
        System.out.println("── Tags ──────────────────────────────────────");
        System.out.println(" 15.  Add Tag to Task");
        System.out.println(" 16.  Remove Tag from Task");
        System.out.println("── Import / Export ───────────────────────────");
        System.out.println(" 17.  Export Tasks to CSV");
        System.out.println(" 18.  Import Tasks from CSV");
        System.out.println(" 19.  Export Tasks to iCal (.ics)");
        System.out.println("── Iteration III ─────────────────────────────");
        System.out.println(" 20.  View Overloaded Collaborators");
        System.out.println("─────────────────────────────────────────────");
        System.out.println("  0.  Exit");
    }

    private void doCreateTask() {
        System.out.println("\n── Create Task ──────────────────────────────");
        String title = readString("Title: ");
        String desc = readStringOptional("Description (optional): ");
        PriorityLevel priority = readPriority();
        LocalDate due = readDateOptional("Due date (YYYY-MM-DD, optional): ");

        long id = system.createTask(title, desc, priority, due);
        System.out.println("  ✓ Task created. ID = " + id);
    }

    private void doUpdateTask() {
        System.out.println("\n── Update Task ──────────────────────────────");
        long taskId = readLong("Task ID: ");
        System.out.println("  Fields: title | description | priority | dueDate | status | tags");
        String field = readString("Field to update: ").toLowerCase().trim();

        Object newValue;
        switch (field) {
            case "title":
            case "description":
                newValue = readString("New value: ");
                break;
            case "priority":
                newValue = readPriority();
                break;
            case "duedate":
                field = "dueDate";
                newValue = readDateOptional("New due date (YYYY-MM-DD, blank to clear): ");
                break;
            case "status":
                newValue = readStatus();
                break;
            case "tags":
                String raw = readStringOptional("Tags (comma-separated, blank to clear): ");
                newValue = raw.isBlank() ? Collections.emptyList() : Arrays.asList(raw.split(","));
                break;
            default:
                System.out.println("  Unknown field.");
                return;
        }

        system.updateTask(taskId, field, newValue);
        System.out.println("  ✓ Task updated.");
    }

    private void doAddSubtask() {
        System.out.println("\n── Add Subtask ──────────────────────────────");
        long taskId = readLong("Parent Task ID: ");
        String title = readString("Subtask title: ");
        long id = system.addSubtask(taskId, title);
        System.out.println("  ✓ Subtask created. ID = " + id);
    }

    private void doCreateRecurringTask() {
        System.out.println("\n── Create Recurring Task ────────────────────");
        String title = readString("Title: ");
        String desc = readStringOptional("Description (optional): ");
        PriorityLevel priority = readPriority();

        System.out.println("  Recurrence type (DAILY / WEEKLY / MONTHLY / CUSTOM): ");
        RecurrenceType recurrenceType = RecurrenceType.valueOf(readString("Type: ").toUpperCase().trim());
        int interval = readInt("Interval (e.g. every N days/weeks/months): ");

        Set<DayOfWeek> weekdays = new HashSet<>();
        if (recurrenceType == RecurrenceType.WEEKLY) {
            System.out.println("  Weekdays (MON,TUE,WED,THU,FRI,SAT,SUN — comma separated): ");
            String days = readString("Weekdays: ");
            for (String wd : days.split(",")) {
                try {
                    weekdays.add(DayOfWeek.valueOf(wd.trim().toUpperCase()));
                } catch (Exception e) {
                    System.out.println("  Warning: unknown day '" + wd.trim() + "' — skipped.");
                }
            }
        }

        Integer dayOfMonth = null;
        if (recurrenceType == RecurrenceType.MONTHLY) {
            dayOfMonth = readInt("Day of month (1-31): ");
        }

        LocalDate startDate = readDate("Start date (YYYY-MM-DD): ");
        LocalDate endDate = readDateOptional("End date (YYYY-MM-DD, optional): ");

        long id = system.createRecurringTask(title, desc, priority, recurrenceType, interval,
                weekdays, dayOfMonth, startDate, endDate);
        System.out.println("  ✓ Recurring task created. ID = " + id);
    }

    private void doCreateProject() {
        System.out.println("\n── Create Project ───────────────────────────");
        String name = readString("Project name: ");
        String desc = readStringOptional("Description (optional): ");
        long id = system.createProject(name, desc);
        System.out.println("  ✓ Project created. ID = " + id);
    }

    private void doAssignTaskToProject() {
        System.out.println("\n── Assign Task to Project ───────────────────");
        long taskId = readLong("Task ID: ");
        long projectId = readLong("Project ID: ");
        system.assignTaskToProject(taskId, projectId);
        System.out.println("  ✓ Task assigned to project.");
    }

    private void doRemoveTaskFromProject() {
        System.out.println("\n── Remove Task from Project ─────────────────");
        long taskId = readLong("Task ID: ");
        system.removeTaskFromProjectById(taskId);
        System.out.println("  ✓ Task removed from project.");
    }

    private void doCreateCollaborator() {
        System.out.println("\n── Create Collaborator ──────────────────────");
        String name = readString("Name: ");
        System.out.println("  Category (SENIOR / INTERMEDIATE / JUNIOR): ");
        CollaboratorCategory category = CollaboratorCategory.valueOf(
                readString("Category: ").toUpperCase().trim());
        long id = system.createCollaborator(name, category);
        System.out.println("  ✓ Collaborator created. ID = " + id);
    }

    private void doAssignCollaboratorToProject() {
        System.out.println("\n── Assign Collaborator to Project ───────────");
        long collaboratorId = readLong("Collaborator ID: ");
        long projectId = readLong("Project ID: ");
        system.assignCollaboratorToProject(collaboratorId, projectId);
        System.out.println("  ✓ Collaborator assigned to project.");
    }

    private void doAssignCollaboratorToTask() {
        System.out.println("\n── Assign Collaborator to Task ──────────────");
        long taskId = readLong("Task ID: ");
        long collaboratorId = readLong("Collaborator ID: ");
        system.assignCollaboratorToTask(taskId, collaboratorId);
        System.out.println("  ✓ Collaborator assigned. Linked subtask created automatically.");
    }

    private void doSearchTasks() {
        System.out.println("\n── Search Tasks ─────────────────────────────");
        System.out.println("  (Leave blank to use default: all OPEN tasks by due date)");
        SearchCriteria criteria = buildSearchCriteria();

        List<Task> results = system.searchTasks(criteria);
        System.out.println("\n  Found " + results.size() + " task(s):");
        results.forEach(t -> System.out.println("  " + t));
    }

    private void doViewTask() {
        System.out.println("\n── View Task Detail ─────────────────────────");
        long id = readLong("Task ID: ");
        system.getTask(id).ifPresentOrElse(
                task -> System.out.println("\n" + task.toDetailString()),
                () -> System.out.println("  Task not found."));
    }

    private void doListProjects() {
        System.out.println("\n── Projects ─────────────────────────────────");
        Collection<Project> projects = system.getAllProjects();
        if (projects.isEmpty()) {
            System.out.println("  No projects found.");
            return;
        }
        projects.forEach(p -> System.out.println("  " + p
                + " [tasks=" + p.getTasks().size()
                + ", collaborators=" + p.getCollaborators().size() + "]"));
    }

    private void doListCollaborators() {
        System.out.println("\n── Collaborators ────────────────────────────");
        Collection<Collaborator> collaborators = system.getAllCollaborators();
        if (collaborators.isEmpty()) {
            System.out.println("  No collaborators found.");
            return;
        }
        collaborators.forEach(c -> {
            int openCount = system.getOpenTaskCountForCollaborator(c.getCollaboratorId());
            System.out.println("  " + c + " [openTasks=" + openCount
                    + "/" + c.getWorkloadLimit() + "]");
        });
    }

    private void doAddTag() {
        System.out.println("\n── Add Tag to Task ──────────────────────────");
        long id = readLong("Task ID: ");
        String keyword = readString("Tag keyword: ");
        system.addTagToTask(id, keyword);
        System.out.println("  ✓ Tag added.");
    }

    private void doRemoveTag() {
        System.out.println("\n── Remove Tag from Task ─────────────────────");
        long id = readLong("Task ID: ");
        String keyword = readString("Tag keyword to remove: ");
        system.removeTagFromTask(id, keyword);
        System.out.println("  ✓ Tag removed (if it existed).");
    }

    private void doExportCsv() {
        System.out.println("\n── Export Tasks to CSV ──────────────────────");
        String path = readString("Destination file path: ");
        system.exportTasksToCsv(path);
        System.out.println("  ✓ Exported successfully to: " + path);
    }

    private void doImportCsv() {
        System.out.println("\n── Import Tasks from CSV ────────────────────");
        String path = readString("CSV file path: ");
        int count = system.importTasksFromCsv(path);
        System.out.println("  ✓ Imported " + count + " task(s).");
    }

    private void doExportIcal() {
        System.out.println("\n── Export Tasks to iCal (.ics) ──────────────");
        System.out.println("  Export type: SINGLE | PROJECT | FILTERED");
        ExportType exportType = ExportType.valueOf(readString("Type: ").toUpperCase().trim());

        Long taskId = null;
        Long projectId = null;
        SearchCriteria criteria = null;

        switch (exportType) {
            case SINGLE:
                taskId = readLong("Task ID: ");
                break;
            case PROJECT:
                projectId = readLong("Project ID: ");
                break;
            case FILTERED:
                criteria = buildSearchCriteria();
                break;
            default:
                System.out.println("  Unknown export type.");
                return;
        }

        String path = readString("Destination .ics file path: ");
        system.exportTasksToIcal(exportType, taskId, projectId, criteria, path);
        System.out.println("  ✓ iCal file exported successfully to: " + path);
    }

    private void doViewOverloadedCollaborators() {
        System.out.println("\n── View Overloaded Collaborators ────────────");
        List<Collaborator> overloaded = system.getOverloadedCollaborators();
        if (overloaded.isEmpty()) {
            System.out.println("  No overloaded collaborators found.");
            return;
        }
        overloaded.forEach(c -> {
            int openCount = system.getOpenTaskCountForCollaborator(c.getCollaboratorId());
            System.out.println("  " + c.getName()
                    + " (" + c.getCategory() + ")"
                    + " — open tasks: " + openCount
                    + ", limit: " + c.getWorkloadLimit());
        });
    }

    private SearchCriteria buildSearchCriteria() {
        SearchCriteria criteria = new SearchCriteria();

        String kw = readStringOptional("Keyword: ");
        if (!kw.isBlank()) {
            criteria.keyword(kw);
        }

        String statusStr = readStringOptional("Status (OPEN/COMPLETED/CANCELLED, blank=any): ");
        if (!statusStr.isBlank()) {
            try {
                criteria.status(TaskStatus.valueOf(statusStr.toUpperCase()));
            } catch (Exception e) {
                System.out.println("  Unknown status — ignored.");
            }
        }

        String priorityStr = readStringOptional("Priority (LOW/MEDIUM/HIGH, blank=any): ");
        if (!priorityStr.isBlank()) {
            try {
                criteria.priority(PriorityLevel.valueOf(priorityStr.toUpperCase()));
            } catch (Exception e) {
                System.out.println("  Unknown priority — ignored.");
            }
        }

        LocalDate from = readDateOptional("Due from (YYYY-MM-DD, optional): ");
        LocalDate to = readDateOptional("Due to   (YYYY-MM-DD, optional): ");
        if (from != null) {
            criteria.startDate(from);
        }
        if (to != null) {
            criteria.endDate(to);
        }

        String dayOfWeekStr = readStringOptional("Day of week (MONDAY...SUNDAY, blank=any): ");
        if (!dayOfWeekStr.isBlank()) {
            try {
                criteria.dayOfWeek(DayOfWeek.valueOf(dayOfWeekStr.toUpperCase()));
            } catch (Exception e) {
                System.out.println("  Unknown day of week — ignored.");
            }
        }

        String projectIdStr = readStringOptional("Project ID (blank=any): ");
        if (!projectIdStr.isBlank()) {
            try {
                criteria.projectId(Long.parseLong(projectIdStr));
            } catch (Exception e) {
                System.out.println("  Invalid project ID — ignored.");
            }
        }

        return criteria;
    }

    private String readString(String prompt) {
        System.out.print("  " + prompt);
        String line = scanner.nextLine().trim();
        if (line.isBlank()) {
            System.out.print("  " + prompt);
            line = scanner.nextLine().trim();
        }
        return line;
    }

    private String readStringOptional(String prompt) {
        System.out.print("  " + prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a number.");
            }
        }
    }

    private long readLong(String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            try {
                return Long.parseLong(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a number.");
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException e) {
                System.out.println("  Invalid date. Format: YYYY-MM-DD");
            }
        }
    }

    private LocalDate readDateOptional(String prompt) {
        System.out.print("  " + prompt);
        String s = scanner.nextLine().trim();
        if (s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            System.out.println("  Invalid date format — treated as no date.");
            return null;
        }
    }

    private PriorityLevel readPriority() {
        while (true) {
            System.out.print("  Priority (LOW / MEDIUM / HIGH): ");
            try {
                return PriorityLevel.valueOf(scanner.nextLine().trim().toUpperCase());
            } catch (Exception e) {
                System.out.println("  Invalid priority.");
            }
        }
    }

    private TaskStatus readStatus() {
        while (true) {
            System.out.print("  Status (OPEN / COMPLETED / CANCELLED): ");
            try {
                return TaskStatus.valueOf(scanner.nextLine().trim().toUpperCase());
            } catch (Exception e) {
                System.out.println("  Invalid status.");
            }
        }
    }
}
