package taskmanager;

import taskmanager.cli.CLI;
import taskmanager.system.TaskManagementSystem;

public class Main {
    public static void main(String[] args) {
        TaskManagementSystem system = new TaskManagementSystem();
        new CLI(system).run();
    }
}
