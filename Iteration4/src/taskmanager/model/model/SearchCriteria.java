package taskmanager.model;

import taskmanager.enums.PriorityLevel;
import taskmanager.enums.TaskStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class SearchCriteria {
    private String keyword;
    private TaskStatus status;
    private PriorityLevel priority;
    private LocalDate startDate;
    private LocalDate endDate;
    private DayOfWeek dayOfWeek;
    private Long projectId;

    public SearchCriteria keyword(String keyword)       { this.keyword   = keyword;   return this; }
    public SearchCriteria status(TaskStatus status)     { this.status    = status;    return this; }
    public SearchCriteria priority(PriorityLevel p)     { this.priority  = p;         return this; }
    public SearchCriteria startDate(LocalDate startDate){ this.startDate = startDate; return this; }
    public SearchCriteria endDate(LocalDate endDate)    { this.endDate   = endDate;   return this; }
    public SearchCriteria dayOfWeek(DayOfWeek dayOfWeek){ this.dayOfWeek = dayOfWeek; return this; }
    public SearchCriteria projectId(long projectId)     { this.projectId = projectId; return this; }

    public String       getKeyword()   { return keyword; }
    public TaskStatus   getStatus()    { return status; }
    public PriorityLevel getPriority() { return priority; }
    public LocalDate    getStartDate() { return startDate; }
    public LocalDate    getEndDate()   { return endDate; }
    public DayOfWeek    getDayOfWeek() { return dayOfWeek; }
    public Long         getProjectId() { return projectId; }

    public boolean isEmpty() {
        return keyword == null && status == null && priority == null
            && startDate == null && endDate == null
            && dayOfWeek == null && projectId == null;
    }
}
