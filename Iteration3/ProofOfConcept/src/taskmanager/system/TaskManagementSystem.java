package taskmanager.system;
import taskmanager.enums.*;
import taskmanager.model.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The system class implementing all 11 public system operations derived from
 * the System Sequence Diagrams.  All state is kept in-memory for the PoC.
 *
 * Operation contracts (pre/post-conditions) are enforced via
 * IllegalArgumentException and IllegalStateException as appropriate.
 */
public class TaskManagementSystem {

    //in-memory stores

    private final Map<Long, Task>         tasks         = new LinkedHashMap<>();
    private final Map<Long, Project>      projects      = new LinkedHashMap<>();
    private final Map<Long, Collaborator> collaborators = new LinkedHashMap<>();

    // OP-01  createTask

    /**
     * Registers a new task.
     *
     * Pre-conditions:
     *   title is non-null and non-blank.
     *   priority is non-null.
     *   dueDate, if provided, is today or in the future.
     *
     * Post-conditions:
     *   A new Task exists with status OPEN and creationDate = today.
     *   An ActivityEntry records the creation.
     *
     * @return the new task's ID
     */
    public long createTask(String title, String description,
                           PriorityLevel priority, LocalDate dueDate) {
        // pre-conditions
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Task title is required and cannot be blank.");
        if (priority == null)
            throw new IllegalArgumentException("A priority level must be specified (LOW, MEDIUM, HIGH).");
        if (dueDate != null && dueDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Due date cannot be in the past.");

        // post-conditions
        Task task = new Task(title, description, priority, dueDate);
        task.addActivityEntry(new ActivityEntry("Task created: " + title));
        tasks.put(task.getTaskId(), task);
        return task.getTaskId();
    }

    // OP-02  updateTask
    /**
     * Updates a single property of an existing task.
     * Supported fields: title, description, priority, dueDate, status, project, tags.
     *
     * Pre-conditions:
     *   Task with taskId exists.
     *   field is a valid modifiable attribute name.
     *   If field=status and newValue=OPEN, the task must not already be finalized.
     *
     * Post-conditions:
     *   The specified property is changed.  An ActivityEntry is created.
     */
    public void updateTask(long taskId, String field, Object newValue) {
        Task task = requireTask(taskId);

        Set<String> validFields = new HashSet<>(Arrays.asList(
            "title", "description", "priority", "dueDate", "status", "project", "tags"));
        if (!validFields.contains(field))
            throw new IllegalArgumentException("Unknown or non-modifiable field: " + field);

        switch (field) {
            case "title": {
                String v = (String) newValue;
                if (v == null || v.isBlank())
                    throw new IllegalArgumentException("Title cannot be blank.");
                task.setTitle(v);
                break;
            }
            case "description":
                task.setDescription((String) newValue);
                break;
            case "priority": {
                PriorityLevel p = (PriorityLevel) newValue;
                if (p == null)
                    throw new IllegalArgumentException("Priority cannot be null.");
                task.setPriority(p);
                break;
            }
            case "dueDate": {
                LocalDate d = (LocalDate) newValue;
                if (d != null && d.isBefore(LocalDate.now()))
                    throw new IllegalArgumentException("Due date cannot be in the past.");
                task.setDueDate(d);
                break;
            }
            case "status": {
                TaskStatus newStatus = (TaskStatus) newValue;
                if (newStatus == null)
                    throw new IllegalArgumentException("Status cannot be null.");
                if (newStatus == TaskStatus.OPEN
                        && (task.getStatus() == TaskStatus.COMPLETED
                            || task.getStatus() == TaskStatus.CANCELLED)) {
                    throw new IllegalStateException(
                        "Cannot change status: task is already finalized.");
                }
                task.setStatus(newStatus);
                break;
            }
            case "project": {
                if (newValue == null) {
                    removeTaskFromProject(task);
                } else {
                    long pid = (Long) newValue;
                    Project p = requireProject(pid);
                    moveTaskToProject(task, p);
                }
                break;
            }
            case "tags": {
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) newValue;
                // replace all tags
                task.getTags().stream().collect(Collectors.toList())
                    .forEach(task::removeTag);
                if (keywords != null)
                    keywords.forEach(kw -> task.addTag(new Tag(kw)));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported field: " + field);
        }
        task.addActivityEntry(new ActivityEntry("Task updated: field=" + field
            + " newValue=" + newValue));
    }

    // OP-03  addSubtask
    /**
     * Attaches a new subtask to a parent task.
     *
     * Pre-conditions:
     *   Task with taskId exists and is not Cancelled.
     *   subtaskTitle is non-blank.
     *
     * Post-conditions:
     *   A new Subtask (isCompleted=false) is associated with the task.
     *
     * @return the new subtask's ID
     */
    public long addSubtask(long taskId, String subtaskTitle) {
        Task task = requireTask(taskId);
        if (task.getStatus() == TaskStatus.CANCELLED)
            throw new IllegalStateException("Cannot add a subtask to a Cancelled task.");
        if (subtaskTitle == null || subtaskTitle.isBlank())
            throw new IllegalArgumentException("Subtask title is required.");

        Subtask subtask = new Subtask(subtaskTitle);
        task.addSubtask(subtask);
        return subtask.getSubtaskId();
    }

    // OP-04  assignTaskToProject
    /**
     * Groups a task under a project.  Any existing project association is replaced.
     *
     * Pre-conditions:
     *   Task with taskId exists.
     *   Project with projectId exists.
     *
     * Post-conditions:
     *   Task is associated with the project; previous association (if any) removed.
     *   ActivityEntry created on the task.
     */
    public void assignTaskToProject(long taskId, long projectId) {
        Task    task    = requireTask(taskId);
        Project project = requireProject(projectId);

        // remove previous project association
        removeTaskFromProject(task);

        // form new association
        task.setProject(project);
        project.addTask(task);
        task.addActivityEntry(new ActivityEntry(
            "Task assigned to project: " + project.getName()));
    }

    // OP-05  createProject

    /**
     * Creates a new named project container.
     *
     * Pre-conditions:  name is non-blank.
     * Post-conditions: A new Project exists with a unique ID and no tasks.
     *
     * @return the new project's ID
     */
    public long createProject(String name, String description) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Project name is required.");

        Project project = new Project(name, description);
        projects.put(project.getProjectId(), project);
        return project.getProjectId();
    }

    // OP-06  createRecurringTask
    /**
     * Creates a recurring task together with its recurrence pattern and generates
     * future occurrences (up to MAX_OCCURRENCES or the endDate).
     *
     * Pre-conditions:
     *   title non-blank; priority non-null; recurrenceType non-null.
     *   interval > 0; startDate non-null.
     *   endDate (if present) >= startDate.
     *   For WEEKLY: weekdays non-empty.
     *   No generated (title, dueDate) pair duplicates an existing one.
     *
     * Post-conditions:
     *   Task + RecurrencePattern + TaskOccurrences created.
     *   ActivityEntry recorded.
     *
     * @return the new task's ID
     */
    public long createRecurringTask(String title, String description,
                                    PriorityLevel priority,
                                    RecurrenceType recurrenceType, int interval,
                                    Set<DayOfWeek> weekdays, Integer dayOfMonth,
                                    LocalDate startDate, LocalDate endDate) {
        // pre-conditions
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Task title is required.");
        if (priority == null)
            throw new IllegalArgumentException("Priority is required.");
        if (recurrenceType == null)
            throw new IllegalArgumentException("Recurrence type is required.");
        if (interval <= 0)
            throw new IllegalArgumentException("Recurrence interval must be greater than zero.");
        if (startDate == null)
            throw new IllegalArgumentException("Start date is required.");
        if (endDate != null && endDate.isBefore(startDate))
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        if (recurrenceType == RecurrenceType.WEEKLY
                && (weekdays == null || weekdays.isEmpty()))
            throw new IllegalArgumentException(
                "At least one weekday must be selected for weekly recurrence.");

        // generate occurrence dates
        List<LocalDate> occurrenceDates = generateOccurrenceDates(
            recurrenceType, interval, weekdays, dayOfMonth, startDate, endDate);

        // uniqueness check: (title, dueDate) must not already exist
        for (LocalDate d : occurrenceDates) {
            checkTitleDueDateUnique(title, d);
        }

        // create task
        Task task = new Task(title, description, priority, null);
        RecurrencePattern pattern = new RecurrencePattern(
            recurrenceType, interval, startDate, endDate,
            weekdays != null ? weekdays : Collections.emptySet(), dayOfMonth);
        task.setRecurrencePattern(pattern);
        occurrenceDates.forEach(d -> task.addOccurrence(new TaskOccurrence(d)));
        task.addActivityEntry(new ActivityEntry("Recurring task created: " + title));
        tasks.put(task.getTaskId(), task);
        return task.getTaskId();
    }

    // OP-07  createCollaborator
    /**
     * Registers a new collaborator.
     *
     * Pre-conditions:  name non-blank; category non-null.
     * Post-conditions: A new Collaborator exists with unique ID.
     *
     * @return the new collaborator's ID
     */
    public long createCollaborator(String name, CollaboratorCategory category) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Collaborator name is required.");
        if (category == null)
            throw new IllegalArgumentException("Collaborator category is required.");

        Collaborator c = new Collaborator(name, category);
        collaborators.put(c.getCollaboratorId(), c);
        return c.getCollaboratorId();
    }

    // OP-08  assignCollaboratorToTask
    /**
     * Assigns a collaborator to a task and automatically creates a linked subtask.
     *
     * Pre-conditions:
     *   Task and Collaborator exist.
     *   Task belongs to a Project.
     *   Collaborator belongs to the same Project.
     *   Collaborator workload limit not exceeded.
     *
     * Post-conditions:
     *   Collaborator associated with Task.
     *   A Subtask linked to the Collaborator is created.
     *   ActivityEntry recorded.
     */
    public void assignCollaboratorToTask(long taskId, long collaboratorId) {
        Task         task = requireTask(taskId);
        Collaborator c    = requireCollaborator(collaboratorId);

        if (task.getProject() == null)
            throw new IllegalStateException(
                "Collaborators can only be assigned to tasks that belong to a project.");

        if (c.getProject() == null || !c.getProject().equals(task.getProject()))
            throw new IllegalStateException(
                "Collaborator must belong to the same project as the task.");

        // check workload: count open tasks already assigned to this collaborator
        long openCount = tasks.values().stream()
            .filter(t -> t.getStatus() == TaskStatus.OPEN)
            .filter(t -> t.getCollaborators().contains(c))
            .count();
        if (openCount >= c.getWorkloadLimit())
            throw new IllegalStateException(
                "Collaborator workload limit exceeded ("
                + c.getCategory() + " limit = " + c.getWorkloadLimit() + ").");

        // assign collaborator and create linked subtask
        task.addCollaborator(c);
        Subtask linked = new Subtask("Work by " + c.getName());
        linked.setCollaborator(c);
        task.addSubtask(linked);
        task.addActivityEntry(new ActivityEntry(
            "Collaborator assigned: " + c.getName() + " (" + c.getCategory() + ")"));
    }

    // OP-09  searchTasks
    /**
     * Returns tasks matching all provided criteria.
     * If criteria is empty, returns all OPEN tasks ordered by due date ascending.
     *
     * Pre-conditions:
     *   criteria is non-null.
     *   If both startDate and endDate are provided, startDate <= endDate.
     */
    public List<Task> searchTasks(SearchCriteria criteria) {
        if (criteria == null)
            throw new IllegalArgumentException("Search criteria cannot be null.");
        if (criteria.getStartDate() != null && criteria.getEndDate() != null
                && criteria.getStartDate().isAfter(criteria.getEndDate()))
            throw new IllegalArgumentException(
                "Invalid date range: start date must be before or equal to end date.");

        if (criteria.isEmpty()) {
            // default: all OPEN tasks sorted by due date
            return tasks.values().stream()
                .filter(t -> t.getStatus() == TaskStatus.OPEN)
                .sorted(Comparator.comparing(
                    t -> t.getDueDate() != null ? t.getDueDate() : LocalDate.MAX))
                .collect(Collectors.toList());
        }

        return tasks.values().stream()
            .filter(t -> matchesCriteria(t, criteria))
            .sorted(Comparator.comparing(
                t -> t.getDueDate() != null ? t.getDueDate() : LocalDate.MAX))
            .collect(Collectors.toList());
    }

    // OP-10  exportTasksToCsv
    /**
     * Exports all tasks to a CSV file.
     *
     * Pre-conditions:  filePath is non-blank.
     * Post-conditions: A CSV file exists at filePath with one row per task.
     */
    public void exportTasksToCsv(String filePath) {
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("A valid destination path must be specified.");

        taskmanager.util.CsvHandler.export(filePath, new ArrayList<>(tasks.values()),
            new ArrayList<>(projects.values()), new ArrayList<>(collaborators.values()));
    }

    // OP-11  importTasksFromCsv
    /**
     * Imports tasks (and associated projects / collaborators) from a CSV file.
     * Projects and collaborators are created if they do not yet exist.
     *
     * Pre-conditions:  filePath is non-blank; file exists; CSV is well-formed.
     * Post-conditions: Imported tasks, projects, collaborators exist in the system.
     *
     * @return number of tasks imported
     */
    public int importTasksFromCsv(String filePath) {
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("A valid file path must be specified.");

        return taskmanager.util.CsvHandler.importTasks(filePath, this);
    }

    // Additional convenience operations (non-critical)
    /** Adds a tag keyword to a task. */
    public void addTagToTask(long taskId, String keyword) {
        Task task = requireTask(taskId);
        if (keyword == null || keyword.isBlank())
            throw new IllegalArgumentException("Tag keyword cannot be blank.");
        task.addTag(new Tag(keyword));
    }

    /** Removes a tag from a task by keyword. */
    public void removeTagFromTask(long taskId, String keyword) {
        Task task = requireTask(taskId);
        task.getTags().stream()
            .filter(t -> t.getKeyword().equalsIgnoreCase(keyword))
            .findFirst()
            .ifPresent(task::removeTag);
    }

    /** Removes a task from its project (if any). */
    public void removeTaskFromProjectById(long taskId) {
        Task task = requireTask(taskId);
        removeTaskFromProject(task);
    }

    /**
     * Assigns a collaborator to a project.
     * Used both explicitly and by importTasksFromCsv.
     */
    public void assignCollaboratorToProject(long collaboratorId, long projectId) {
        Collaborator c = requireCollaborator(collaboratorId);
        Project p      = requireProject(projectId);
        c.setProject(p);
        p.addCollaborator(c);
    }

    // Read-only accessors (for CLI display)
    public Optional<Task>         getTask(long id)         { return Optional.ofNullable(tasks.get(id)); }
    public Optional<Project>      getProject(long id)      { return Optional.ofNullable(projects.get(id)); }
    public Optional<Collaborator> getCollaborator(long id) { return Optional.ofNullable(collaborators.get(id)); }

    public Collection<Task>         getAllTasks()         { return Collections.unmodifiableCollection(tasks.values()); }
    public Collection<Project>      getAllProjects()      { return Collections.unmodifiableCollection(projects.values()); }
    public Collection<Collaborator> getAllCollaborators() { return Collections.unmodifiableCollection(collaborators.values()); }

    /** Find a project by name (case-insensitive). Returns null if not found. */
    public Project findProjectByName(String name) {
        return projects.values().stream()
            .filter(p -> p.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    /** Find a collaborator by name (case-insensitive). Returns null if not found. */
    public Collaborator findCollaboratorByName(String name) {
        return collaborators.values().stream()
            .filter(c -> c.getName().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    /** Check (title, dueDate) uniqueness; throw if violated. */
    public void checkTitleDueDateUnique(String title, LocalDate dueDate) {
        boolean conflict = tasks.values().stream().anyMatch(t ->
            t.getTitle().equalsIgnoreCase(title)
            && Objects.equals(t.getDueDate(), dueDate));
        if (conflict)
            throw new IllegalArgumentException(
                "A task with title '" + title + "' and due date "
                + (dueDate != null ? dueDate : "(none)") + " already exists.");
    }

    private Task requireTask(long id) {
        Task t = tasks.get(id);
        if (t == null) throw new IllegalArgumentException("Task not found: id=" + id);
        return t;
    }

    private Project requireProject(long id) {
        Project p = projects.get(id);
        if (p == null) throw new IllegalArgumentException("Project not found: id=" + id);
        return p;
    }

    private Collaborator requireCollaborator(long id) {
        Collaborator c = collaborators.get(id);
        if (c == null) throw new IllegalArgumentException("Collaborator not found: id=" + id);
        return c;
    }

    private void removeTaskFromProject(Task task) {
        if (task.getProject() != null) {
            task.getProject().removeTask(task);
            task.setProject(null);
        }
    }

    private void moveTaskToProject(Task task, Project project) {
        removeTaskFromProject(task);
        task.setProject(project);
        project.addTask(task);
    }

    /** Generate occurrence dates for a recurrence pattern (max 365 occurrences!). */
    private List<LocalDate> generateOccurrenceDates(RecurrenceType type, int interval,
                                                     Set<DayOfWeek> weekdays,
                                                     Integer dayOfMonth,
                                                     LocalDate startDate, LocalDate endDate) {
        final int MAX = 365;
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        LocalDate limit   = endDate != null ? endDate : startDate.plusYears(1);

        while (!current.isAfter(limit) && dates.size() < MAX) {
            switch (type) {
                case DAILY:
                    dates.add(current);
                    current = current.plusDays(interval);
                    break;
                case WEEKLY:
                    // walk through the week looking for matching weekdays
                    for (int d = 0; d < 7 && !current.isAfter(limit) && dates.size() < MAX; d++) {
                        if (weekdays.contains(current.getDayOfWeek())) {
                            dates.add(current);
                        }
                        current = current.plusDays(1);
                    }
                    // skip ahead by (interval-1) weeks
                    current = current.plusWeeks(interval - 1);
                    break;
                case MONTHLY:
                    int dom = dayOfMonth != null ? dayOfMonth : startDate.getDayOfMonth();
                    try {
                        LocalDate candidate = current.withDayOfMonth(dom);
                        if (!candidate.isBefore(startDate) && !candidate.isAfter(limit))
                            dates.add(candidate);
                    } catch (Exception ignored) { /* invalid day for month */ }
                    current = current.plusMonths(interval);
                    break;
                case CUSTOM:
                    dates.add(current);
                    current = current.plusDays(interval);
                    break;
            }
        }
        return dates;
    }

    /** Returns true if a task matches ALL non-null fields in the criteria. */
    private boolean matchesCriteria(Task t, SearchCriteria c) {
        if (c.getStatus() != null && t.getStatus() != c.getStatus())
            return false;
        if (c.getPriority() != null && t.getPriority() != c.getPriority())
            return false;
        if (c.getProjectId() != null) {
            if (t.getProject() == null || t.getProject().getProjectId() != c.getProjectId())
                return false;
        }
        if (c.getKeyword() != null) {
            String kw = c.getKeyword().toLowerCase();
            boolean inTitle = t.getTitle().toLowerCase().contains(kw);
            boolean inDesc  = t.getDescription() != null
                           && t.getDescription().toLowerCase().contains(kw);
            boolean inTags  = t.getTags().stream()
                               .anyMatch(tag -> tag.getKeyword().toLowerCase().contains(kw));
            if (!inTitle && !inDesc && !inTags) return false;
        }
        if (c.getStartDate() != null || c.getEndDate() != null) {
            LocalDate due = t.getDueDate();
            if (due == null) return false;
            if (c.getStartDate() != null && due.isBefore(c.getStartDate())) return false;
            if (c.getEndDate()   != null && due.isAfter(c.getEndDate()))    return false;
        }
        if (c.getDayOfWeek() != null) {
            LocalDate due = t.getDueDate();
            if (due == null || due.getDayOfWeek() != c.getDayOfWeek()) return false;
        }
        return true;
    }
}
