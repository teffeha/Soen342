package taskmanager.model;
import taskmanager.enums.PriorityLevel;
import taskmanager.enums.TaskStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central domain object representing a single task in the system.
 * A task may exist without a project (project is 0..1).
 */
public class Task {
    private static long idCounter = 1;

    private final long          taskId;
    private       String        title;
    private       String        description;
    private final LocalDate     creationDate;
    private       LocalDate     dueDate;          // optional
    private       PriorityLevel priority;
    private       TaskStatus    status;

    /** The project this task belongs to (0..1 — may be null). */
    private Project project;

    private final List<Subtask>       subtasks      = new ArrayList<>();
    private final List<Tag>           tags          = new ArrayList<>();
    private final List<ActivityEntry> activityLog   = new ArrayList<>();
    private final List<Collaborator>  collaborators = new ArrayList<>();

    /** Recurrence pattern (0..1). */
    private RecurrencePattern recurrencePattern;
    /** Generated occurrences for recurring tasks. */
    private final List<TaskOccurrence> occurrences  = new ArrayList<>();

    public Task(String title, String description, PriorityLevel priority, LocalDate dueDate) {
        this.taskId       = idCounter++;
        this.title        = title;
        this.description  = description;
        this.creationDate = LocalDate.now();
        this.dueDate      = dueDate;
        this.priority     = priority;
        this.status       = TaskStatus.OPEN;
    }

    //Getters

    public long          getTaskId()       { return taskId; }
    public String        getTitle()        { return title; }
    public String        getDescription()  { return description; }
    public LocalDate     getCreationDate() { return creationDate; }
    public LocalDate     getDueDate()      { return dueDate; }
    public PriorityLevel getPriority()     { return priority; }
    public TaskStatus    getStatus()       { return status; }
    public Project       getProject()      { return project; }
    public RecurrencePattern getRecurrencePattern() { return recurrencePattern; }

    public List<Subtask>       getSubtasks()     { return Collections.unmodifiableList(subtasks); }
    public List<Tag>           getTags()         { return Collections.unmodifiableList(tags); }
    public List<ActivityEntry> getActivityLog()  { return Collections.unmodifiableList(activityLog); }
    public List<Collaborator>  getCollaborators(){ return Collections.unmodifiableList(collaborators); }
    public List<TaskOccurrence> getOccurrences() { return Collections.unmodifiableList(occurrences); }

    // Setters and Mutators

    public void setTitle(String title)                      { this.title = title; }
    public void setDescription(String description)          { this.description = description; }
    public void setDueDate(LocalDate dueDate)               { this.dueDate = dueDate; }
    public void setPriority(PriorityLevel priority)         { this.priority = priority; }
    public void setStatus(TaskStatus status)                { this.status = status; }
    public void setProject(Project project)                 { this.project = project; }
    public void setRecurrencePattern(RecurrencePattern rp)  { this.recurrencePattern = rp; }

    public void addSubtask(Subtask s)          { subtasks.add(s); }
    public void addTag(Tag t)                  { tags.add(t); }
    public void removeTag(Tag t)               { tags.remove(t); }
    public void addActivityEntry(ActivityEntry e) { activityLog.add(e); }
    public void addCollaborator(Collaborator c) { if (!collaborators.contains(c)) collaborators.add(c); }
    public void addOccurrence(TaskOccurrence o) { occurrences.add(o); }

    //Display

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Task{id=").append(taskId)
          .append(", title='").append(title).append("'")
          .append(", priority=").append(priority)
          .append(", status=").append(status)
          .append(", created=").append(creationDate);
        if (dueDate != null)  sb.append(", due=").append(dueDate);
        if (project != null)  sb.append(", project='").append(project.getName()).append("'");
        if (!tags.isEmpty())  sb.append(", tags=").append(tags);
        sb.append("}");
        return sb.toString();
    }

    public String toDetailString() {
        StringBuilder sb = new StringBuilder(toString());
        if (!subtasks.isEmpty()) {
            sb.append("\n  Subtasks:");
            subtasks.forEach(s -> sb.append("\n  ").append(s));
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
                occurrences.forEach(o -> sb.append("\n  ").append(o));
            }
        }
        if (!activityLog.isEmpty()) {
            sb.append("\n  Activity Log:");
            activityLog.forEach(a -> sb.append("\n    ").append(a));
        }
        return sb.toString();
    }
}
