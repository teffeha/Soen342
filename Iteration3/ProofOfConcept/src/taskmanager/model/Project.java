package taskmanager.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Project {
    private static long idCounter = 1;

    private final long projectId;
    private String name;
    private String description;
    private final List<Task> tasks = new ArrayList<>();
    private final List<Collaborator> collaborators = new ArrayList<>();

    public Project(String name, String description) {
        this.projectId = idCounter++;
        this.name = name;
        this.description = description;
    }

    public long getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    public List<Collaborator> getCollaborators() {
        return Collections.unmodifiableList(collaborators);
    }

    public void addTask(Task task) {
        if (!tasks.contains(task)) {
            tasks.add(task);
        }
    }

    public void removeTask(Task task) {
        tasks.remove(task);
    }

    public void addCollaborator(Collaborator collaborator) {
        if (!collaborators.contains(collaborator)) {
            collaborators.add(collaborator);
        }
    }

    public void removeCollaborator(Collaborator collaborator) {
        collaborators.remove(collaborator);
    }

    @Override
    public String toString() {
        return "Project{id=" + projectId + ", name='" + name + "'"
             + (description != null && !description.isEmpty() ? ", desc='" + description + "'" : "")
             + "}";
    }
}
