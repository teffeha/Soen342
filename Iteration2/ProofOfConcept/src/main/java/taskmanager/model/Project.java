package taskmanager.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class Project {
    private static long idCounter = 1;

    private final long   projectId;
    private       String name;
    private       String description;

    /** Tasks that belong to this project (0..*). */
    private final List<Task>         tasks         = new ArrayList<>();
    /** Collaborators assigned to this project (0..*). */
    private final List<Collaborator> collaborators = new ArrayList<>();

    public Project(String name, String description) {
        this.projectId   = idCounter++;
        this.name        = name;
        this.description = description;
    }

    public long   getProjectId()   { return projectId; }
    public String getName()        { return name; }
    public String getDescription() { return description; }

    public void setName(String name)               { this.name = name; }
    public void setDescription(String description) { this.description = description; }

    public List<Task>         getTasks()         { return Collections.unmodifiableList(tasks); }
    public List<Collaborator> getCollaborators() { return Collections.unmodifiableList(collaborators); }

    public void addTask(Task t)               { if (!tasks.contains(t))         tasks.add(t); }
    public void removeTask(Task t)            { tasks.remove(t); }
    public void addCollaborator(Collaborator c)    { if (!collaborators.contains(c)) collaborators.add(c); }
    public void removeCollaborator(Collaborator c) { collaborators.remove(c); }

    @Override
    public String toString() {
        return "Project{id=" + projectId + ", name='" + name + "'"
             + (description != null && !description.isEmpty() ? ", desc='" + description + "'" : "")
             + "}";
    }
}
