package taskmanager.model;

public class Tag {
    private static long idCounter = 1;

    private final long tagId;
    private final String keyword;

    /** Constructor for new tags. */
    public Tag(String keyword) {
        this.tagId   = idCounter++;
        this.keyword = keyword;
    }

    /** Constructor for loading an existing tag from the database. */
    public Tag(long tagId, String keyword) {
        this.tagId   = tagId;
        this.keyword = keyword;
        if (tagId >= idCounter) idCounter = tagId + 1;
    }

    public static void syncIdCounter(long maxStoredId) {
        if (maxStoredId >= idCounter) idCounter = maxStoredId + 1;
    }

    public long   getTagId()   { return tagId; }
    public String getKeyword() { return keyword; }

    @Override
    public String toString() { return keyword; }
}
