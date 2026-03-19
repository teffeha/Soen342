package taskmanager.cli;

import taskmanager.enums.*;
import taskmanager.model.*;
import taskmanager.system.TaskManagementSystem;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Command-line interface for the Personal Task Management System.
 * Provides a menu-driven loop that maps to each system operation.
 */
public class CLI {

    private final TaskManagementSystem system;
    private final Scanner scanner;

    public CLI(TaskManagementSystem system) {
        this.system  = system;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   Personal Task Management System  v1.0  ║");
        System.out.println("║           SOEN 342 — PoC Build           ║");
        System.out.println("╚══════════════════════════════════════════╝");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Choice: ");
            try {
                switch (choice) {
                    // ── Task operations ──────────────────────────────────────
                    case  1: doCreateTask();             break;
                    case  2: doUpdateTask();             break;
                    case  3: doAddSubtask();             break;
                    case  4: doCreateRecurringTask();    break;
                    // ── Project operations ───────────────────────────────────
                    case  5: doCreateProject();          break;
                    case  6: doAssignTaskToProject();    break;
                    case  7: doRemoveTaskFromProject();  break;
                    // ── Collaborator operations ──────────────────────────────
                    case  8: doCreateCollaborator();     break;
                    case  9: doAssignCollaboratorToProject(); break;
                    case 10: doAssignCollaboratorToTask(); break;
                    // ── Search & view ────────────────────────────────────────
                    case 11: doSearchTasks();            break;
                    case 12: doViewTask();               break;
                    case 13: doListProjects();           break;
                    case 14: doListCollaborators();      break;
                    // ── Tag operations ───────────────────────────────────────
                    case 15: doAddTag();                 break;
                    case 16: doRemoveTag();              break;
                    // ── CSV ──────────────────────────────────────────────────
                    case 17: doExportCsv();              break;
                    case 18: doImportCsv();              break;
                    // ── Exit ─────────────────────────────────────────────────
                    case  0: running = false;            break;
                    default:
                        System.out.println("  Unknown option. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("  ✗ Error: " + e.getMessage());
            }
        }
        System.out.println("Goodbye.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu
    // ─────────────────────────────────────────────────────────────────────────

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
        System.out.println("── CSV ───────────────────────────────────────");
        System.out.println(" 17.  Export Tasks to CSV");
        System.out.println(" 18.  Import Tasks from CSV");
        System.out.println("─────────────────────────────────────────────");
        System.out.println("  0.  Exit");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-01  Create Task
    // ─────────────────────────────────────────────────────────────────────────

    private void doCreateTask() {
        System.out.println("\n── Create Task ──────────────────────────────");
        String title    = readString("Title: ");
        String desc     = readStringOptional("Description (optional): ");
        PriorityLevel p = readPriority();
        LocalDate due   = readDateOptional("Due date (YYYY-MM-DD, optional): ");

        long id = system.createTask(title, desc, p, due);
        System.out.println("  ✓ Task created. ID = " + id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-02  Update Task
    // ─────────────────────────────────────────────────────────────────────────

    private void doUpdateTask() {
        System.out.println("\n── Update Task ──────────────────────────────");
        long taskId = readLong("Task ID: ");
        System.out.println("  Fields: title | description | priority | dueDate | status | tags");
        String field = readString("Field to update: ").toLowerCase().trim();

        Object newValue;
        switch (field) {
            case "title":
            case "description":
                newValue = readString("New value: "); break;
            case "priority":
                newValue = readPriority(); break;
            case "duedate":
                field = "dueDate";
                newValue = readDateOptional("New due date (YYYY-MM-DD, blank to clear): "); break;
            case "status":
                newValue = readStatus(); break;
            case "tags":
                String raw = readStringOptional("Tags (comma-separated, blank to clear): ");
                newValue = raw.isBlank() ? Collections.emptyList()
                         : Arrays.asList(raw.split(","));
                break;
            default:
                System.out.println("  Unknown field."); return;
        }

        system.updateTask(taskId, field, newValue);
        System.out.println("  ✓ Task updated.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-03  Add Subtask
    // ─────────────────────────────────────────────────────────────────────────

    private void doAddSubtask() {
        System.out.println("\n── Add Subtask ──────────────────────────────");
        long taskId = readLong("Parent Task ID: ");
        String title = readString("Subtask title: ");
        long id = system.addSubtask(taskId, title);
        System.out.println("  ✓ Subtask created. ID = " + id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-04  Create Recurring Task
    // ─────────────────────────────────────────────────────────────────────────

    private void doCreateRecurringTask() {
        System.out.println("\n── Create Recurring Task ────────────────────");
        String title    = readString("Title: ");
        String desc     = readStringOptional("Description (optional): ");
        PriorityLevel p = readPriority();

        System.out.println("  Recurrence type (DAILY / WEEKLY / MONTHLY / CUSTOM): ");
        RecurrenceType rt = RecurrenceType.valueOf(readString("Type: ").toUpperCase().trim());
        int interval = readInt("Interval (e.g. every N days/weeks/months): ");

        Set<DayOfWeek> weekdays = new HashSet<>();
        if (rt == RecurrenceType.WEEKLY) {
            System.out.println("  Weekdays (MON,TUE,WED,THU,FRI,SAT,SUN — comma separated): ");
            String wds = readString("Weekdays: ");
            for (String wd : wds.split(",")) {
                try { weekdays.add(DayOfWeek.valueOf(wd.trim().toUpperCase())); }
                catch (Exception e) { System.out.println("  Warning: unknown day '" + wd.trim() + "' — skipped."); }
            }
        }

        Integer dayOfMonth = null;
        if (rt == RecurrenceType.MONTHLY) {
            dayOfMonth = readInt("Day of month (1-31): ");
        }

        LocalDate startDate = readDate("Start date (YYYY-MM-DD): ");
        LocalDate endDate   = readDateOptional("End date (YYYY-MM-DD, optional): ");

        long id = system.createRecurringTask(title, desc, p, rt, interval,
                                             weekdays, dayOfMonth, startDate, endDate);
        System.out.println("  ✓ Recurring task created. ID = " + id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-05  Create Project
    // ─────────────────────────────────────────────────────────────────────────

    private void doCreateProject() {
        System.out.println("\n── Create Project ───────────────────────────");
        String name = readString("Project name: ");
        String desc = readStringOptional("Description (optional): ");
        long id = system.createProject(name, desc);
        System.out.println("  ✓ Project created. ID = " + id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-06  Assign Task to Project
    // ─────────────────────────────────────────────────────────────────────────

    private void doAssignTaskToProject() {
        System.out.println("\n── Assign Task to Project ───────────────────");
        long taskId    = readLong("Task ID: ");
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

    // ─────────────────────────────────────────────────────────────────────────
    // OP-07  Create Collaborator
    // ─────────────────────────────────────────────────────────────────────────

    private void doCreateCollaborator() {
        System.out.println("\n── Create Collaborator ──────────────────────");
        String name = readString("Name: ");
        System.out.println("  Category (SENIOR / INTERMEDIATE / JUNIOR): ");
        CollaboratorCategory cat = CollaboratorCategory.valueOf(
            readString("Category: ").toUpperCase().trim());
        long id = system.createCollaborator(name, cat);
        System.out.println("  ✓ Collaborator created. ID = " + id);
    }

    private void doAssignCollaboratorToProject() {
        System.out.println("\n── Assign Collaborator to Project ───────────");
        long cId = readLong("Collaborator ID: ");
        long pId = readLong("Project ID: ");
        system.assignCollaboratorToProject(cId, pId);
        System.out.println("  ✓ Collaborator assigned to project.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-08  Assign Collaborator to Task
    // ─────────────────────────────────────────────────────────────────────────

    private void doAssignCollaboratorToTask() {
        System.out.println("\n── Assign Collaborator to Task ──────────────");
        long taskId  = readLong("Task ID: ");
        long collabId = readLong("Collaborator ID: ");
        system.assignCollaboratorToTask(taskId, collabId);
        System.out.println("  ✓ Collaborator assigned. Linked subtask created automatically.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-09  Search Tasks
    // ─────────────────────────────────────────────────────────────────────────

    private void doSearchTasks() {
        System.out.println("\n── Search Tasks ─────────────────────────────");
        System.out.println("  (Leave blank to use default: all OPEN tasks by due date)");
        SearchCriteria criteria = new SearchCriteria();

        String kw = readStringOptional("Keyword: ");
        if (!kw.isBlank()) criteria.keyword(kw);

        String statusStr = readStringOptional("Status (OPEN/COMPLETED/CANCELLED, blank=any): ");
        if (!statusStr.isBlank()) {
            try { criteria.status(TaskStatus.valueOf(statusStr.toUpperCase())); }
            catch (Exception e) { System.out.println("  Unknown status — ignored."); }
        }

        String prioStr = readStringOptional("Priority (LOW/MEDIUM/HIGH, blank=any): ");
        if (!prioStr.isBlank()) {
            try { criteria.priority(PriorityLevel.valueOf(prioStr.toUpperCase())); }
            catch (Exception e) { System.out.println("  Unknown priority — ignored."); }
        }

        LocalDate from = readDateOptional("Due from (YYYY-MM-DD, optional): ");
        LocalDate to   = readDateOptional("Due to   (YYYY-MM-DD, optional): ");
        if (from != null) criteria.startDate(from);
        if (to   != null) criteria.endDate(to);

        List<Task> results = system.searchTasks(criteria);
        System.out.println("\n  Found " + results.size() + " task(s):");
        results.forEach(t -> System.out.println("  " + t));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // View helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void doViewTask() {
        System.out.println("\n── View Task Detail ─────────────────────────");
        long id = readLong("Task ID: ");
        system.getTask(id).ifPresentOrElse(
            t -> System.out.println("\n" + t.toDetailString()),
            () -> System.out.println("  Task not found.")
        );
    }

    private void doListProjects() {
        System.out.println("\n── Projects ─────────────────────────────────");
        Collection<Project> ps = system.getAllProjects();
        if (ps.isEmpty()) { System.out.println("  No projects found."); return; }
        ps.forEach(p -> System.out.println("  " + p
            + " [tasks=" + p.getTasks().size()
            + ", collaborators=" + p.getCollaborators().size() + "]"));
    }

    private void doListCollaborators() {
        System.out.println("\n── Collaborators ────────────────────────────");
        Collection<Collaborator> cs = system.getAllCollaborators();
        if (cs.isEmpty()) { System.out.println("  No collaborators found."); return; }
        cs.forEach(c -> System.out.println("  " + c));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tag operations
    // ─────────────────────────────────────────────────────────────────────────

    private void doAddTag() {
        System.out.println("\n── Add Tag to Task ──────────────────────────");
        long id = readLong("Task ID: ");
        String kw = readString("Tag keyword: ");
        system.addTagToTask(id, kw);
        System.out.println("  ✓ Tag added.");
    }

    private void doRemoveTag() {
        System.out.println("\n── Remove Tag from Task ─────────────────────");
        long id = readLong("Task ID: ");
        String kw = readString("Tag keyword to remove: ");
        system.removeTagFromTask(id, kw);
        System.out.println("  ✓ Tag removed (if it existed).");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OP-10  Export / OP-11  Import
    // ─────────────────────────────────────────────────────────────────────────

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

    // ─────────────────────────────────────────────────────────────────────────
    // Input helpers
    // ─────────────────────────────────────────────────────────────────────────

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
            try { return Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  Please enter a number."); }
        }
    }

    private long readLong(String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            try { return Long.parseLong(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  Please enter a number."); }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            System.out.print("  " + prompt);
            try { return LocalDate.parse(scanner.nextLine().trim()); }
            catch (DateTimeParseException e) { System.out.println("  Invalid date. Format: YYYY-MM-DD"); }
        }
    }

    private LocalDate readDateOptional(String prompt) {
        System.out.print("  " + prompt);
        String s = scanner.nextLine().trim();
        if (s.isBlank()) return null;
        try { return LocalDate.parse(s); }
        catch (DateTimeParseException e) {
            System.out.println("  Invalid date format — treated as no date.");
            return null;
        }
    }

    private PriorityLevel readPriority() {
        while (true) {
            System.out.print("  Priority (LOW / MEDIUM / HIGH): ");
            try { return PriorityLevel.valueOf(scanner.nextLine().trim().toUpperCase()); }
            catch (Exception e) { System.out.println("  Invalid priority."); }
        }
    }

    private TaskStatus readStatus() {
        while (true) {
            System.out.print("  Status (OPEN / COMPLETED / CANCELLED): ");
            try { return TaskStatus.valueOf(scanner.nextLine().trim().toUpperCase()); }
            catch (Exception e) { System.out.println("  Invalid status."); }
        }
    }
}
