package taskmanager.model;

public class Tag {
    private static long idCounter = 1;

    private final long   tagId;
    private final String keyword;

    public Tag(String keyword) {
        this.tagId   = idCounter++;
        this.keyword = keyword;
    }

    public long   getTagId()   { return tagId; }
    public String getKeyword() { return keyword; }

    @Override
    public String toString() { return keyword; }
}
