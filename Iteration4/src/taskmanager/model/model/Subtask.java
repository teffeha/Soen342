package taskmanager.model;

public class Subtask {
    private static long idCounter = 1;

    private final long subtaskId;
    private String title;
    private boolean completed;
    private Collaborator collaborator;

    /** Constructor for new subtasks. */
    public Subtask(String title) {
        this.subtaskId = idCounter++;
        this.title     = title;
        this.completed = false;
    }

    /** Constructor for loading an existing subtask from the database. */
    public Subtask(long subtaskId, String title, boolean completed) {
        this.subtaskId = subtaskId;
        this.title     = title;
        this.completed = completed;
        if (subtaskId >= idCounter) idCounter = subtaskId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    public long         getSubtaskId()  { return subtaskId; }
    public String       getTitle()      { return title; }
    public boolean      isCompleted()   { return completed; }
    public Collaborator getCollaborator() { return collaborator; }
    public void         setTitle(String t)       { this.title = t; }
    public void         setCompleted(boolean c)  { this.completed = c; }
    public void         setCollaborator(Collaborator c) { this.collaborator = c; }

    @Override
    public String toString() {
        return "Subtask{id=" + subtaskId + ", title='" + title + "'"
             + ", completed=" + completed
             + (collaborator != null ? ", collaborator='" + collaborator.getName() + "'" : "")
             + "}";
    }
}
