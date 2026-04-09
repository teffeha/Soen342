package taskmanager.model;

import taskmanager.enums.CollaboratorCategory;

public class Collaborator {
    private static long idCounter = 1;

    private final long collaboratorId;
    private final String name;
    private final CollaboratorCategory category;
    private Project project;

    /** Constructor for new collaborators. */
    public Collaborator(String name, CollaboratorCategory category) {
        this.collaboratorId = idCounter++;
        this.name     = name;
        this.category = category;
    }

    /** Constructor for loading an existing collaborator from the database. */
    public Collaborator(long collaboratorId, String name, CollaboratorCategory category) {
        this.collaboratorId = collaboratorId;
        this.name     = name;
        this.category = category;
        if (collaboratorId >= idCounter) idCounter = collaboratorId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    public long                  getCollaboratorId() { return collaboratorId; }
    public String                getName()           { return name; }
    public CollaboratorCategory  getCategory()       { return category; }
    public Project               getProject()        { return project; }
    public void                  setProject(Project p) { this.project = p; }
    public int                   getWorkloadLimit()  { return category.getWorkloadLimit(); }

    @Override
    public String toString() {
        return "Collaborator{id=" + collaboratorId + ", name='" + name
             + "', category=" + category
             + (project != null ? ", project='" + project.getName() + "'" : "")
             + "}";
    }
}
