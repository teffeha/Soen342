package taskmanager.model;

public class Subtask {
    private static long idCounter = 1;

    private final long   subtaskId;
    private       String title;
    private       boolean isCompleted;

    /** The collaborator this subtask is linked to (0..1 per subtask, but a collaborator has 0..*). */
    private Collaborator collaborator;

    public Subtask(String title) {
        this.subtaskId   = idCounter++;
        this.title       = title;
        this.isCompleted = false;
    }

    public long   getSubtaskId()   { return subtaskId; }
    public String getTitle()       { return title; }
    public boolean isCompleted()   { return isCompleted; }
    public Collaborator getCollaborator() { return collaborator; }

    public void setTitle(String title)                  { this.title = title; }
    public void setCompleted(boolean completed)         { this.isCompleted = completed; }
    public void setCollaborator(Collaborator collaborator) { this.collaborator = collaborator; }

    @Override
    public String toString() {
        return "  Subtask{id=" + subtaskId + ", title='" + title + "'"
             + ", completed=" + isCompleted
             + (collaborator != null ? ", collaborator='" + collaborator.getName() + "'" : "")
             + "}";
    }
}
