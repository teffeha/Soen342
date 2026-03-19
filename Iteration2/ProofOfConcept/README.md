# Personal Task Management System — Proof of Concept

## SOEN 342 — Iteration II

### Running the application

**Option 1 — Pre-built JAR (recommended)**

```
java -jar TaskManager.jar
```

Project structure
src/main/java/taskmanager/
│
├── Main.java                          Entry point
│
├── enums/
│   ├── TaskStatus.java                OPEN | COMPLETED | CANCELLED
│   ├── PriorityLevel.java             LOW | MEDIUM | HIGH
│   ├── CollaboratorCategory.java      SENIOR(2) | INTERMEDIATE(5) | JUNIOR(10)
│   └── RecurrenceType.java            DAILY | WEEKLY | MONTHLY | CUSTOM
│
├── model/
│   ├── Task.java
│   ├── Project.java
│   ├── Subtask.java
│   ├── Tag.java
│   ├── ActivityEntry.java
│   ├── Collaborator.java
│   ├── RecurrencePattern.java
│   ├── TaskOccurrence.java
│   └── SearchCriteria.java            Value object for OP-09
│
├── system/
│   └── TaskManagementSystem.java      All 11 system operations (OP-01 to OP-11)
│
├── util/
│   └── CsvHandler.java                CSV export (OP-10) and import (OP-11)
│
└── cli/
    └── CLI.java                       Interactive menu-driven interface
```

\---

### System operations implemented

|ID|Operation signature|Menu option|
|-|-|-|
|OP-01|createTask(...)|1|
|OP-02|updateTask(taskId, field, newValue)|2|
|OP-03|addSubtask(taskId, title)|3|
|OP-04|assignTaskToProject(taskId, projectId)|6|
|OP-05|createProject(name, description)|5|
|OP-06|createRecurringTask(...)|4|
|OP-07|createCollaborator(name, category)|8|
|OP-08|assignCollaboratorToTask(taskId, collabId)|10|
|OP-09|searchTasks(criteria)|11|
|OP-10|exportTasksToCsv(filePath)|17|
|OP-11|importTasksFromCsv(filePath)|18|



