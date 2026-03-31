package taskmanager.model;

import taskmanager.enums.CollaboratorCategory;

public class Collaborator {
    private static long idCounter = 1;

    private final long collaboratorId;
    private final String name;
    private final CollaboratorCategory category;
    private Project project;

    public Collaborator(String name, CollaboratorCategory category) {
        this.collaboratorId = idCounter++;
        this.name = name;
        this.category = category;
    }

    public long getCollaboratorId() {
        return collaboratorId;
    }

    public String getName() {
        return name;
    }

    public CollaboratorCategory getCategory() {
        return category;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public int getWorkloadLimit() {
        return category.getWorkloadLimit();
    }

    @Override
    public String toString() {
        return "Collaborator{id=" + collaboratorId + ", name='" + name
             + "', category=" + category
             + (project != null ? ", project='" + project.getName() + "'" : "")
             + "}";
    }
}
