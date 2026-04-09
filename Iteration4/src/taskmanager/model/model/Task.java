package taskmanager.model;

import taskmanager.enums.PriorityLevel;
import taskmanager.enums.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Task {
    private static long idCounter = 1;

    private final long taskId;
    private String title;
    private String description;
    private final LocalDate creationDate;
    private LocalDate dueDate;
    private PriorityLevel priority;
    private TaskStatus status;
    private Project project;
    private final List<Subtask> subtasks = new ArrayList<>();
    private final List<Tag> tags = new ArrayList<>();
    private final List<ActivityEntry> activityLog = new ArrayList<>();
    private final List<Collaborator> collaborators = new ArrayList<>();
    private RecurrencePattern recurrencePattern;
    private final List<TaskOccurrence> occurrences = new ArrayList<>();

    /** Constructor for new tasks. */
    public Task(String title, String description, PriorityLevel priority, LocalDate dueDate) {
        this.taskId       = idCounter++;
        this.title        = title;
        this.description  = description;
        this.creationDate = LocalDate.now();
        this.dueDate      = dueDate;
        this.priority     = priority;
        this.status       = TaskStatus.OPEN;
    }

    /** Constructor for loading an existing task from the database. */
    public Task(long taskId, String title, String description,
                LocalDate creationDate, LocalDate dueDate,
                PriorityLevel priority, TaskStatus status) {
        this.taskId       = taskId;
        this.title        = title;
        this.description  = description;
        this.creationDate = creationDate;
        this.dueDate      = dueDate;
        this.priority     = priority;
        this.status       = status;
        if (taskId >= idCounter) idCounter = taskId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    public long             getTaskId()           { return taskId; }
    public String           getTitle()            { return title; }
    public String           getDescription()      { return description; }
    public LocalDate        getCreationDate()      { return creationDate; }
    public LocalDate        getDueDate()           { return dueDate; }
    public PriorityLevel    getPriority()          { return priority; }
    public TaskStatus       getStatus()            { return status; }
    public Project          getProject()           { return project; }
    public RecurrencePattern getRecurrencePattern(){ return recurrencePattern; }

    public List<Subtask>       getSubtasks()    { return Collections.unmodifiableList(subtasks); }
    public List<Tag>           getTags()        { return Collections.unmodifiableList(tags); }
    public List<ActivityEntry> getActivityLog() { return Collections.unmodifiableList(activityLog); }
    public List<Collaborator>  getCollaborators(){ return Collections.unmodifiableList(collaborators); }
    public List<TaskOccurrence> getOccurrences(){ return Collections.unmodifiableList(occurrences); }

    public void setTitle(String title)                            { this.title = title; }
    public void setDescription(String description)                { this.description = description; }
    public void setDueDate(LocalDate dueDate)                     { this.dueDate = dueDate; }
    public void setPriority(PriorityLevel priority)               { this.priority = priority; }
    public void setStatus(TaskStatus status)                      { this.status = status; }
    public void setProject(Project project)                       { this.project = project; }
    public void setRecurrencePattern(RecurrencePattern pattern)   { this.recurrencePattern = pattern; }

    public void addSubtask(Subtask subtask)             { subtasks.add(subtask); }
    public void addTag(Tag tag)                         { tags.add(tag); }
    public void removeTag(Tag tag)                      { tags.remove(tag); }
    public void addActivityEntry(ActivityEntry entry)   { activityLog.add(entry); }
    public void addOccurrence(TaskOccurrence occurrence){ occurrences.add(occurrence); }

    public void addCollaborator(Collaborator collaborator) {
        if (!collaborators.contains(collaborator)) collaborators.add(collaborator);
    }

    public boolean hasDueDate() { return dueDate != null; }

    public String buildSubtaskSummary() {
        if (subtasks.isEmpty()) return "No subtasks.";
        return subtasks.stream()
                .map(s -> "- " + s.getTitle()
                        + (s.isCompleted() ? " [completed]" : " [open]")
                        + (s.getCollaborator() != null ? " {" + s.getCollaborator().getName() + "}" : ""))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public String toIcsDescription() {
        StringBuilder sb = new StringBuilder();
        if (description != null && !description.isBlank())
            sb.append(description).append(System.lineSeparator()).append(System.lineSeparator());
        sb.append("Status: ").append(status).append(System.lineSeparator());
        sb.append("Priority: ").append(priority).append(System.lineSeparator());
        if (project != null)
            sb.append("Project: ").append(project.getName()).append(System.lineSeparator());
        sb.append(System.lineSeparator()).append("Subtasks:").append(System.lineSeparator());
        sb.append(buildSubtaskSummary());
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Task{id=").append(taskId)
          .append(", title='").append(title).append("'")
          .append(", priority=").append(priority)
          .append(", status=").append(status)
          .append(", created=").append(creationDate);
        if (dueDate  != null) sb.append(", due=").append(dueDate);
        if (project  != null) sb.append(", project='").append(project.getName()).append("'");
        if (!tags.isEmpty())  sb.append(", tags=").append(tags);
        sb.append("}");
        return sb.toString();
    }

    public String toDetailString() {
        StringBuilder sb = new StringBuilder(toString());
        if (!subtasks.isEmpty()) {
            sb.append("\n  Subtasks:");
            subtasks.forEach(s -> sb.append("\n    - ").append(s));
        }
        if (!collaborators.isEmpty()) {
            sb.append("\n  Collaborators:");
            collaborators.forEach(c -> sb.append("\n    - ").append(c.getName())
                                         .append(" (").append(c.getCategory()).append(")"));
        }
        if (recurrencePattern != null) {
            sb.append("\n  ").append(recurrencePattern);
            if (!occurrences.isEmpty()) {
                sb.append("\n  Occurrences:");
                occurrences.forEach(o -> sb.append("\n    - ").append(o));
            }
        }
        if (!activityLog.isEmpty()) {
            sb.append("\n  Activity Log:");
            activityLog.forEach(a -> sb.append("\n    ").append(a));
        }
        return sb.toString();
    }
}
