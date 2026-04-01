package taskmanager.system;

import taskmanager.enums.CollaboratorCategory;
import taskmanager.enums.ExportType;
import taskmanager.enums.PriorityLevel;
import taskmanager.enums.RecurrenceType;
import taskmanager.enums.TaskStatus;
import taskmanager.model.ActivityEntry;
import taskmanager.model.Collaborator;
import taskmanager.model.Project;
import taskmanager.model.RecurrencePattern;
import taskmanager.model.SearchCriteria;
import taskmanager.model.Subtask;
import taskmanager.model.Tag;
import taskmanager.model.Task;
import taskmanager.model.TaskOccurrence;
import taskmanager.repository.CollaboratorRepository;
import taskmanager.repository.DatabaseManager;
import taskmanager.repository.ProjectRepository;
import taskmanager.repository.TaskRepository;
import taskmanager.util.CsvHandler;
import taskmanager.util.ICalGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskManagementSystem {

    // ── In-memory working set ─────────────────────────────────────────────────
    private final Map<Long, Task>         tasks         = new LinkedHashMap<>();
    private final Map<Long, Project>      projects      = new LinkedHashMap<>();
    private final Map<Long, Collaborator> collaborators = new LinkedHashMap<>();

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final ICalGateway           iCalGateway;
    private final TaskRepository        taskRepo;
    private final ProjectRepository     projectRepo;
    private final CollaboratorRepository collabRepo;

    // ── Constructors ─────────────────────────────────────────────────────────

    public TaskManagementSystem() {
        this(new ICalGateway());
    }

    public TaskManagementSystem(ICalGateway iCalGateway) {
        this.iCalGateway = iCalGateway;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            this.taskRepo    = new TaskRepository(conn);
            this.projectRepo = new ProjectRepository(conn);
            this.collabRepo  = new CollaboratorRepository(conn);
            loadFromDatabase();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialise database: " + e.getMessage(), e);
        }
    }

    // ── OP-01  Create Task ────────────────────────────────────────────────────

    public long createTask(String title, String description,
                           PriorityLevel priority, LocalDate dueDate) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Task title is required and cannot be blank.");
        if (priority == null)
            throw new IllegalArgumentException("A priority level must be specified (LOW, MEDIUM, HIGH).");
        if (dueDate != null && dueDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Due date cannot be in the past.");
        if (dueDate == null) {
            long openWithoutDueDate = tasks.values().stream()
                    .filter(t -> t.getStatus() == TaskStatus.OPEN && t.getDueDate() == null)
                    .count();
            if (openWithoutDueDate >= 50)
                throw new IllegalStateException(
                        "The number of open tasks without a due date cannot exceed 50.");
        }

        Task task = new Task(title, description, priority, dueDate);
        ActivityEntry entry = new ActivityEntry("Task created: " + title);
        task.addActivityEntry(entry);
        tasks.put(task.getTaskId(), task);

        persist(() -> {
            taskRepo.insertTask(task.getTaskId(), task.getTitle(), task.getDescription(),
                    task.getCreationDate(), task.getDueDate(), task.getPriority(),
                    task.getStatus(), null);
            taskRepo.insertActivityEntry(entry.getEntryId(), task.getTaskId(),
                    entry.getTimestamp(), entry.getDescription());
        });

        return task.getTaskId();
    }

    // ── OP-02  Update Task ────────────────────────────────────────────────────

    public void updateTask(long taskId, String field, Object newValue) {
        Task task = requireTask(taskId);

        Set<String> validFields = new HashSet<>(Arrays.asList(
                "title", "description", "priority", "dueDate", "status", "project", "tags"));
        if (!validFields.contains(field))
            throw new IllegalArgumentException("Unknown or non-modifiable field: " + field);

        switch (field) {
            case "title": {
                String value = (String) newValue;
                if (value == null || value.isBlank())
                    throw new IllegalArgumentException("Title cannot be blank.");
                task.setTitle(value);
                break;
            }
            case "description":
                task.setDescription((String) newValue);
                break;
            case "priority": {
                PriorityLevel p = (PriorityLevel) newValue;
                if (p == null) throw new IllegalArgumentException("Priority cannot be null.");
                task.setPriority(p);
                break;
            }
            case "dueDate": {
                LocalDate dueDate = (LocalDate) newValue;
                if (dueDate != null && dueDate.isBefore(LocalDate.now()))
                    throw new IllegalArgumentException("Due date cannot be in the past.");
                if (dueDate == null && task.getDueDate() != null
                        && task.getStatus() == TaskStatus.OPEN) {
                    long openWithoutDueDate = tasks.values().stream()
                            .filter(t -> t.getStatus() == TaskStatus.OPEN && t.getDueDate() == null)
                            .count();
                    if (openWithoutDueDate >= 50)
                        throw new IllegalStateException(
                                "The number of open tasks without a due date cannot exceed 50.");
                }
                task.setDueDate(dueDate);
                break;
            }
            case "status": {
                TaskStatus newStatus = (TaskStatus) newValue;
                if (newStatus == null)
                    throw new IllegalArgumentException("Status cannot be null.");
                if (newStatus == TaskStatus.OPEN
                        && (task.getStatus() == TaskStatus.COMPLETED
                         || task.getStatus() == TaskStatus.CANCELLED))
                    throw new IllegalStateException("Cannot change status: task is already finalised.");
                if (newStatus == TaskStatus.OPEN
                        && task.getStatus() != TaskStatus.OPEN
                        && task.getDueDate() == null) {
                    long openWithoutDueDate = tasks.values().stream()
                            .filter(t -> t.getStatus() == TaskStatus.OPEN && t.getDueDate() == null)
                            .count();
                    if (openWithoutDueDate >= 50)
                        throw new IllegalStateException(
                                "The number of open tasks without a due date cannot exceed 50.");
                }
                task.setStatus(newStatus);
                break;
            }
            case "project": {
                if (newValue == null) {
                    removeTaskFromProject(task);
                } else {
                    Project project = requireProject((Long) newValue);
                    moveTaskToProject(task, project);
                }
                break;
            }
            case "tags": {
                @SuppressWarnings("unchecked")
                List<String> keywords = (List<String>) newValue;
                List<Tag> existing = new ArrayList<>(task.getTags());
                existing.forEach(task::removeTag);
                if (keywords != null) {
                    keywords.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .forEach(kw -> task.addTag(new Tag(kw)));
                }
                // Sync tags to DB
                final long tid = task.getTaskId();
                final List<Tag> newTags = new ArrayList<>(task.getTags());
                persist(() -> {
                    taskRepo.deleteAllTagsForTask(tid);
                    for (Tag t : newTags)
                        taskRepo.insertTag(t.getTagId(), tid, t.getKeyword());
                });
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported field: " + field);
        }

        ActivityEntry entry = new ActivityEntry(
                "Task updated: field=" + field + " newValue=" + newValue);
        task.addActivityEntry(entry);

        final Task t = task;
        final ActivityEntry e = entry;
        persist(() -> {
            Long projId = t.getProject() != null ? t.getProject().getProjectId() : null;
            taskRepo.updateTask(t.getTaskId(), t.getTitle(), t.getDescription(),
                    t.getDueDate(), t.getPriority(), t.getStatus(), projId);
            taskRepo.insertActivityEntry(e.getEntryId(), t.getTaskId(),
                    e.getTimestamp(), e.getDescription());
        });
    }

    // ── OP-03  Add Subtask ────────────────────────────────────────────────────

    public long addSubtask(long taskId, String subtaskTitle) {
        Task task = requireTask(taskId);
        if (task.getStatus() == TaskStatus.CANCELLED)
            throw new IllegalStateException("Cannot add a subtask to a Cancelled task.");
        if (subtaskTitle == null || subtaskTitle.isBlank())
            throw new IllegalArgumentException("Subtask title is required.");
        if (task.getSubtasks().size() >= 20)
            throw new IllegalStateException("A task cannot have more than 20 subtasks.");

        Subtask subtask = new Subtask(subtaskTitle);
        task.addSubtask(subtask);

        ActivityEntry entry = new ActivityEntry("Subtask added: " + subtaskTitle);
        task.addActivityEntry(entry);

        persist(() -> {
            taskRepo.insertSubtask(subtask.getSubtaskId(), taskId,
                    subtask.getTitle(), subtask.isCompleted(), null);
            taskRepo.insertActivityEntry(entry.getEntryId(), taskId,
                    entry.getTimestamp(), entry.getDescription());
        });

        return subtask.getSubtaskId();
    }

    // ── OP-04  Assign Task to Project ─────────────────────────────────────────

    public void assignTaskToProject(long taskId, long projectId) {
        Task task       = requireTask(taskId);
        Project project = requireProject(projectId);
        removeTaskFromProject(task);
        task.setProject(project);
        project.addTask(task);

        ActivityEntry entry = new ActivityEntry("Task assigned to project: " + project.getName());
        task.addActivityEntry(entry);

        persist(() -> {
            taskRepo.updateTask(task.getTaskId(), task.getTitle(), task.getDescription(),
                    task.getDueDate(), task.getPriority(), task.getStatus(), project.getProjectId());
            taskRepo.insertActivityEntry(entry.getEntryId(), taskId,
                    entry.getTimestamp(), entry.getDescription());
        });
    }

    // ── OP-05  Create Project ─────────────────────────────────────────────────

    public long createProject(String name, String description) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Project name is required.");

        Project project = new Project(name, description);
        projects.put(project.getProjectId(), project);

        persist(() -> projectRepo.insert(project.getProjectId(),
                project.getName(), project.getDescription()));

        return project.getProjectId();
    }

    // ── OP-06  Create Recurring Task ──────────────────────────────────────────

    public long createRecurringTask(String title, String description,
                                    PriorityLevel priority,
                                    RecurrenceType recurrenceType, int interval,
                                    Set<DayOfWeek> weekdays, Integer dayOfMonth,
                                    LocalDate startDate, LocalDate endDate) {
        if (title == null || title.isBlank())   throw new IllegalArgumentException("Task title is required.");
        if (priority == null)                   throw new IllegalArgumentException("Priority is required.");
        if (recurrenceType == null)             throw new IllegalArgumentException("Recurrence type is required.");
        if (interval <= 0)                      throw new IllegalArgumentException("Recurrence interval must be greater than zero.");
        if (startDate == null)                  throw new IllegalArgumentException("Start date is required.");
        if (endDate != null && endDate.isBefore(startDate))
            throw new IllegalArgumentException("End date cannot be earlier than start date.");
        if (recurrenceType == RecurrenceType.WEEKLY && (weekdays == null || weekdays.isEmpty()))
            throw new IllegalArgumentException("At least one weekday must be selected for weekly recurrence.");

        List<LocalDate> occurrenceDates = generateOccurrenceDates(
                recurrenceType, interval, weekdays, dayOfMonth, startDate, endDate);
        for (LocalDate date : occurrenceDates) checkTitleDueDateUnique(title, date);

        Task task = new Task(title, description, priority, null);
        RecurrencePattern pattern = new RecurrencePattern(recurrenceType, interval, startDate, endDate,
                weekdays != null ? weekdays : Collections.emptySet(), dayOfMonth);
        task.setRecurrencePattern(pattern);
        occurrenceDates.forEach(d -> task.addOccurrence(new TaskOccurrence(d)));

        ActivityEntry entry = new ActivityEntry("Recurring task created: " + title);
        task.addActivityEntry(entry);
        tasks.put(task.getTaskId(), task);

        persist(() -> {
            taskRepo.insertTask(task.getTaskId(), task.getTitle(), task.getDescription(),
                    task.getCreationDate(), null, task.getPriority(), task.getStatus(), null);
            taskRepo.insertActivityEntry(entry.getEntryId(), task.getTaskId(),
                    entry.getTimestamp(), entry.getDescription());
            taskRepo.insertRecurrencePattern(pattern.getRecurrenceId(), task.getTaskId(),
                    pattern.getType(), pattern.getInterval(), pattern.getStartDate(),
                    pattern.getEndDate(), pattern.getWeekdays(), pattern.getDayOfMonth());
            for (TaskOccurrence occ : task.getOccurrences()) {
                taskRepo.insertOccurrence(occ.getOccurrenceId(), task.getTaskId(),
                        occ.getDueDate(), occ.getStatus());
            }
        });

        return task.getTaskId();
    }

    // ── OP-07  Create Collaborator ────────────────────────────────────────────

    public long createCollaborator(String name, CollaboratorCategory category) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Collaborator name is required.");
        if (category == null)
            throw new IllegalArgumentException("Collaborator category is required.");
        if (category.getWorkloadLimit() <= 0)
            throw new IllegalArgumentException("Collaborator workload limit must be positive.");

        Collaborator collaborator = new Collaborator(name, category);
        collaborators.put(collaborator.getCollaboratorId(), collaborator);

        persist(() -> collabRepo.insert(collaborator.getCollaboratorId(),
                collaborator.getName(), collaborator.getCategory(), null));

        return collaborator.getCollaboratorId();
    }

    // ── OP-08  Assign Collaborator to Task ────────────────────────────────────

    public void assignCollaboratorToTask(long taskId, long collaboratorId) {
        Task task               = requireTask(taskId);
        Collaborator collaborator = requireCollaborator(collaboratorId);

        if (task.getProject() == null)
            throw new IllegalStateException(
                    "Collaborators can only be assigned to tasks that belong to a project.");
        if (collaborator.getProject() == null
                || !collaborator.getProject().equals(task.getProject()))
            throw new IllegalStateException(
                    "Collaborator must belong to the same project as the task.");
        if (task.getSubtasks().size() >= 20)
            throw new IllegalStateException("A task cannot have more than 20 subtasks.");

        long openCount = tasks.values().stream()
                .filter(t -> t.getStatus() == TaskStatus.OPEN)
                .filter(t -> t.getCollaborators().contains(collaborator))
                .count();
        if (openCount >= collaborator.getWorkloadLimit())
            throw new IllegalStateException("Collaborator workload limit exceeded ("
                    + collaborator.getCategory() + " limit = "
                    + collaborator.getWorkloadLimit() + ").");

        task.addCollaborator(collaborator);
        Subtask linked = new Subtask("Work by " + collaborator.getName());
        linked.setCollaborator(collaborator);
        task.addSubtask(linked);

        ActivityEntry entry = new ActivityEntry("Collaborator assigned: "
                + collaborator.getName() + " (" + collaborator.getCategory() + ")");
        task.addActivityEntry(entry);

        persist(() -> {
            taskRepo.insertCollaboratorLink(taskId, collaboratorId);
            taskRepo.insertSubtask(linked.getSubtaskId(), taskId,
                    linked.getTitle(), linked.isCompleted(), collaboratorId);
            taskRepo.insertActivityEntry(entry.getEntryId(), taskId,
                    entry.getTimestamp(), entry.getDescription());
        });
    }

    // ── OP-09  Search Tasks ───────────────────────────────────────────────────

    public List<Task> searchTasks(SearchCriteria criteria) {
        if (criteria == null)
            throw new IllegalArgumentException("Search criteria cannot be null.");
        if (criteria.getStartDate() != null && criteria.getEndDate() != null
                && criteria.getStartDate().isAfter(criteria.getEndDate()))
            throw new IllegalArgumentException(
                    "Invalid date range: start date must be before or equal to end date.");

        if (criteria.isEmpty()) {
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

    // ── OP-10  Export to CSV ──────────────────────────────────────────────────

    public void exportTasksToCsv(String filePath) {
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("A valid destination path must be specified.");
        CsvHandler.export(filePath,
                new ArrayList<>(tasks.values()),
                new ArrayList<>(projects.values()),
                new ArrayList<>(collaborators.values()));
    }

    // ── OP-11  Import from CSV ────────────────────────────────────────────────

    public int importTasksFromCsv(String filePath) {
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("A valid file path must be specified.");
        return CsvHandler.importTasks(filePath, this);
    }

    // ── OP-12  Export to iCal ─────────────────────────────────────────────────

    public void exportTasksToIcal(ExportType exportType,
                                  Long taskId, Long projectId,
                                  SearchCriteria criteria, String filePath) {
        if (exportType == null)
            throw new IllegalArgumentException("Export type is required.");
        if (filePath == null || filePath.isBlank())
            throw new IllegalArgumentException("A valid destination path must be specified.");

        List<Task> eligibleTasks = getTasksForExport(exportType, taskId, projectId, criteria);
        iCalGateway.exportToIcs(eligibleTasks, filePath);
    }

    public List<Task> getTasksForExport(ExportType exportType,
                                        Long taskId, Long projectId,
                                        SearchCriteria criteria) {
        List<Task> source;
        switch (exportType) {
            case SINGLE: {
                if (taskId == null)
                    throw new IllegalArgumentException("Task ID is required for SINGLE export.");
                Task task = requireTask(taskId);
                if (!task.hasDueDate())
                    throw new IllegalArgumentException(
                            "Only tasks with a due date are eligible for calendar export.");
                source = List.of(task);
                break;
            }
            case PROJECT: {
                if (projectId == null)
                    throw new IllegalArgumentException("Project ID is required for PROJECT export.");
                source = new ArrayList<>(requireProject(projectId).getTasks());
                break;
            }
            case FILTERED: {
                if (criteria == null)
                    throw new IllegalArgumentException(
                            "Search criteria are required for FILTERED export.");
                source = searchTasks(criteria);
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported export type: " + exportType);
        }

        return source.stream()
                .filter(Task::hasDueDate)
                .sorted(Comparator.comparing(Task::getDueDate))
                .collect(Collectors.toList());
    }

    // ── OP-13  View Overloaded Collaborators ──────────────────────────────────

    public List<Collaborator> getOverloadedCollaborators() {
        return collaborators.values().stream()
                .filter(this::isOverloaded)
                .collect(Collectors.toList());
    }

    public int getOpenTaskCountForCollaborator(long collaboratorId) {
        Collaborator collaborator = requireCollaborator(collaboratorId);
        return (int) tasks.values().stream()
                .filter(t -> t.getStatus() == TaskStatus.OPEN)
                .filter(t -> t.getCollaborators().contains(collaborator))
                .count();
    }

    // ── Tag operations ────────────────────────────────────────────────────────

    public void addTagToTask(long taskId, String keyword) {
        Task task = requireTask(taskId);
        if (keyword == null || keyword.isBlank())
            throw new IllegalArgumentException("Tag keyword cannot be blank.");
        Tag tag = new Tag(keyword.trim());
        task.addTag(tag);

        ActivityEntry entry = new ActivityEntry("Tag added: " + keyword.trim());
        task.addActivityEntry(entry);

        persist(() -> {
            taskRepo.insertTag(tag.getTagId(), taskId, tag.getKeyword());
            taskRepo.insertActivityEntry(entry.getEntryId(), taskId,
                    entry.getTimestamp(), entry.getDescription());
        });
    }

    public void removeTagFromTask(long taskId, String keyword) {
        Task task = requireTask(taskId);
        task.getTags().stream()
                .filter(t -> t.getKeyword().equalsIgnoreCase(keyword))
                .findFirst()
                .ifPresent(task::removeTag);

        ActivityEntry entry = new ActivityEntry("Tag removed: " + keyword);
        task.addActivityEntry(entry);

        persist(() -> {
            taskRepo.deleteTagByKeyword(taskId, keyword);
            taskRepo.insertActivityEntry(entry.getEntryId(), taskId,
                    entry.getTimestamp(), entry.getDescription());
        });
    }

    public void removeTaskFromProjectById(long taskId) {
        Task task = requireTask(taskId);
        removeTaskFromProject(task);

        ActivityEntry entry = new ActivityEntry("Task removed from project.");
        task.addActivityEntry(entry);

        persist(() -> {
            taskRepo.updateTask(task.getTaskId(), task.getTitle(), task.getDescription(),
                    task.getDueDate(), task.getPriority(), task.getStatus(), null);
            taskRepo.insertActivityEntry(entry.getEntryId(), taskId,
                    entry.getTimestamp(), entry.getDescription());
        });
    }

    public void assignCollaboratorToProject(long collaboratorId, long projectId) {
        Collaborator collaborator = requireCollaborator(collaboratorId);
        Project project           = requireProject(projectId);

        if (collaborator.getProject() != null && !collaborator.getProject().equals(project))
            collaborator.getProject().removeCollaborator(collaborator);

        collaborator.setProject(project);
        project.addCollaborator(collaborator);

        persist(() -> collabRepo.updateProjectId(collaboratorId, projectId));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Optional<Task>         getTask(long id)         { return Optional.ofNullable(tasks.get(id)); }
    public Optional<Project>      getProject(long id)      { return Optional.ofNullable(projects.get(id)); }
    public Optional<Collaborator> getCollaborator(long id) { return Optional.ofNullable(collaborators.get(id)); }

    public Collection<Task>         getAllTasks()         { return Collections.unmodifiableCollection(tasks.values()); }
    public Collection<Project>      getAllProjects()      { return Collections.unmodifiableCollection(projects.values()); }
    public Collection<Collaborator> getAllCollaborators() { return Collections.unmodifiableCollection(collaborators.values()); }

    public Project findProjectByName(String name) {
        return projects.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Collaborator findCollaboratorByName(String name) {
        return collaborators.values().stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public void checkTitleDueDateUnique(String title, LocalDate dueDate) {
        boolean conflict = tasks.values().stream().anyMatch(t ->
                t.getTitle().equalsIgnoreCase(title)
                && Objects.equals(t.getDueDate(), dueDate));
        if (conflict)
            throw new IllegalArgumentException("A task with title '" + title
                    + "' and due date " + (dueDate != null ? dueDate : "(none)")
                    + " already exists.");
    }

    // ── DB load on startup ────────────────────────────────────────────────────

    private void loadFromDatabase() throws SQLException {

        // 1. Projects
        Map<Long, Project> loadedProjects = new LinkedHashMap<>();
        for (ProjectRepository.ProjectRow row : projectRepo.loadAll()) {
            Project p = new Project(row.projectId(), row.name(), row.description());
            loadedProjects.put(row.projectId(), p);
        }
        projects.putAll(loadedProjects);

        // 2. Collaborators
        Map<Long, Collaborator> loadedCollabs = new LinkedHashMap<>();
        for (CollaboratorRepository.CollaboratorRow row : collabRepo.loadAll()) {
            Collaborator c = new Collaborator(row.collaboratorId(), row.name(), row.category());
            if (row.projectId() != null) {
                Project p = loadedProjects.get(row.projectId());
                if (p != null) { c.setProject(p); p.addCollaborator(c); }
            }
            loadedCollabs.put(row.collaboratorId(), c);
        }
        collaborators.putAll(loadedCollabs);

        // 3. Tasks with all dependent objects
        for (TaskRepository.TaskRow row : taskRepo.loadAllTasks()) {
            Task task = new Task(row.taskId(), row.title(), row.description(),
                    row.creationDate(), row.dueDate(), row.priority(), row.status());

            // Project reference
            if (row.projectId() != null) {
                Project p = loadedProjects.get(row.projectId());
                if (p != null) { task.setProject(p); p.addTask(task); }
            }

            // Subtasks
            for (TaskRepository.SubtaskRow sr : taskRepo.loadSubtasksForTask(row.taskId())) {
                Subtask subtask = new Subtask(sr.subtaskId(), sr.title(), sr.completed());
                if (sr.collaboratorId() != null) {
                    Collaborator c = loadedCollabs.get(sr.collaboratorId());
                    if (c != null) subtask.setCollaborator(c);
                }
                task.addSubtask(subtask);
            }

            // Tags
            for (TaskRepository.TagRow tr : taskRepo.loadTagsForTask(row.taskId())) {
                task.addTag(new Tag(tr.tagId(), tr.keyword()));
            }

            // Activity log
            for (TaskRepository.ActivityRow ar : taskRepo.loadActivityForTask(row.taskId())) {
                task.addActivityEntry(
                        new ActivityEntry(ar.entryId(), ar.timestamp(), ar.description()));
            }

            // Recurrence pattern + occurrences
            Optional<TaskRepository.RecurrenceRow> recOpt =
                    taskRepo.loadRecurrenceForTask(row.taskId());
            if (recOpt.isPresent()) {
                TaskRepository.RecurrenceRow rr = recOpt.get();
                RecurrencePattern pattern = new RecurrencePattern(
                        rr.recurrenceId(), rr.type(), rr.interval(),
                        rr.startDate(), rr.endDate(), rr.weekdays(), rr.dayOfMonth());
                task.setRecurrencePattern(pattern);
                for (TaskRepository.OccurrenceRow or : taskRepo.loadOccurrencesForTask(row.taskId())) {
                    task.addOccurrence(
                            new TaskOccurrence(or.occurrenceId(), or.dueDate(), or.status()));
                }
            }

            // Collaborator links
            for (long cId : taskRepo.loadCollaboratorIdsForTask(row.taskId())) {
                Collaborator c = loadedCollabs.get(cId);
                if (c != null) task.addCollaborator(c);
            }

            tasks.put(task.getTaskId(), task);
        }

        // 4. Sync all in-memory ID counters so new objects don't collide with loaded IDs
        Task.syncIdCounter(taskRepo.maxTaskId());
        Subtask.syncIdCounter(taskRepo.maxSubtaskId());
        Tag.syncIdCounter(taskRepo.maxTagId());
        ActivityEntry.syncIdCounter(taskRepo.maxActivityId());
        TaskOccurrence.syncIdCounter(taskRepo.maxOccurrenceId());
        RecurrencePattern.syncIdCounter(taskRepo.maxRecurrenceId());
        Project.syncIdCounter(projectRepo.maxId());
        Collaborator.syncIdCounter(collabRepo.maxId());
    }

    // ── Persistence helper ────────────────────────────────────────────────────

    @FunctionalInterface
    private interface DbOperation { void execute() throws SQLException; }

    private void persist(DbOperation op) {
        try {
            op.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Database error: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isOverloaded(Collaborator collaborator) {
        return getOpenTaskCountForCollaborator(
                collaborator.getCollaboratorId()) > collaborator.getWorkloadLimit();
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

    private List<LocalDate> generateOccurrenceDates(RecurrenceType type, int interval,
                                                    Set<DayOfWeek> weekdays,
                                                    Integer dayOfMonth,
                                                    LocalDate startDate, LocalDate endDate) {
        final int max = 365;
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;
        LocalDate limit   = endDate != null ? endDate : startDate.plusYears(1);

        while (!current.isAfter(limit) && dates.size() < max) {
            switch (type) {
                case DAILY:
                    dates.add(current);
                    current = current.plusDays(interval);
                    break;
                case WEEKLY:
                    for (int d = 0; d < 7 && !current.isAfter(limit) && dates.size() < max; d++) {
                        if (weekdays.contains(current.getDayOfWeek())) dates.add(current);
                        current = current.plusDays(1);
                    }
                    current = current.plusWeeks(interval - 1L);
                    break;
                case MONTHLY: {
                    int dom = dayOfMonth != null ? dayOfMonth : startDate.getDayOfMonth();
                    try {
                        LocalDate candidate = current.withDayOfMonth(dom);
                        if (!candidate.isBefore(startDate) && !candidate.isAfter(limit))
                            dates.add(candidate);
                    } catch (Exception ignored) {}
                    current = current.plusMonths(interval);
                    break;
                }
                case CUSTOM:
                    dates.add(current);
                    current = current.plusDays(interval);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported recurrence type: " + type);
            }
        }
        return dates;
    }

    private boolean matchesCriteria(Task task, SearchCriteria criteria) {
        if (criteria.getStatus()   != null && task.getStatus()   != criteria.getStatus())   return false;
        if (criteria.getPriority() != null && task.getPriority() != criteria.getPriority()) return false;
        if (criteria.getProjectId() != null) {
            if (task.getProject() == null
                    || task.getProject().getProjectId() != criteria.getProjectId()) return false;
        }
        if (criteria.getKeyword() != null) {
            String kw = criteria.getKeyword().toLowerCase();
            boolean inTitle = task.getTitle().toLowerCase().contains(kw);
            boolean inDesc  = task.getDescription() != null
                    && task.getDescription().toLowerCase().contains(kw);
            boolean inTags  = task.getTags().stream()
                    .anyMatch(t -> t.getKeyword().toLowerCase().contains(kw));
            if (!inTitle && !inDesc && !inTags) return false;
        }
        if (criteria.getStartDate() != null || criteria.getEndDate() != null) {
            LocalDate due = task.getDueDate();
            if (due == null) return false;
            if (criteria.getStartDate() != null && due.isBefore(criteria.getStartDate())) return false;
            if (criteria.getEndDate()   != null && due.isAfter(criteria.getEndDate()))    return false;
        }
        if (criteria.getDayOfWeek() != null) {
            LocalDate due = task.getDueDate();
            if (due == null || due.getDayOfWeek() != criteria.getDayOfWeek()) return false;
        }
        return true;
    }
}
