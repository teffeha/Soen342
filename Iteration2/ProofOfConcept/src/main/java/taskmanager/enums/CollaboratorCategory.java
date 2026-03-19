package taskmanager.enums;

public enum CollaboratorCategory {
    SENIOR(2),
    INTERMEDIATE(5),
    JUNIOR(10);

    private final int workloadLimit;

    CollaboratorCategory(int workloadLimit) {
        this.workloadLimit = workloadLimit;
    }

    public int getWorkloadLimit() {
        return workloadLimit;
    }
}
