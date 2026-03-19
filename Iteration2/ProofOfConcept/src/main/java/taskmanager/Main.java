package taskmanager;

import taskmanager.cli.CLI;
import taskmanager.system.TaskManagementSystem;

/**
 * Entry point for the Personal Task Management System PoC.
 *
 * Run: java -cp out taskmanager.Main
 */
public class Main {
    public static void main(String[] args) {
        TaskManagementSystem system = new TaskManagementSystem();
        new CLI(system).run();
    }
}
