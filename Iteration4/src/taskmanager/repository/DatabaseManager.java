package taskmanager.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:taskmanager.db";

    private static DatabaseManager instance;
    private final Connection connection;

    private DatabaseManager() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found on classpath.", e);
        }
        connection = DriverManager.getConnection(DB_URL);
        connection.setAutoCommit(true);
        initSchema();
    }

    public static DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            System.out.println("[DB] Creating table: projects");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS projects (" +
                            "    project_id   INTEGER PRIMARY KEY," +
                            "    name         TEXT NOT NULL," +
                            "    description  TEXT" +
                            ")"
            );

            System.out.println("[DB] Creating table: collaborators");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS collaborators (" +
                            "    collaborator_id  INTEGER PRIMARY KEY," +
                            "    name             TEXT NOT NULL," +
                            "    category         TEXT NOT NULL," +
                            "    project_id       INTEGER REFERENCES projects(project_id)" +
                            ")"
            );

            System.out.println("[DB] Creating table: tasks");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS tasks (" +
                            "    task_id        INTEGER PRIMARY KEY," +
                            "    title          TEXT NOT NULL," +
                            "    description    TEXT," +
                            "    creation_date  TEXT NOT NULL," +
                            "    due_date       TEXT," +
                            "    priority       TEXT NOT NULL," +
                            "    status         TEXT NOT NULL," +
                            "    project_id     INTEGER REFERENCES projects(project_id)" +
                            ")"
            );

            System.out.println("[DB] Creating table: subtasks");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS subtasks (" +
                            "    subtask_id       INTEGER PRIMARY KEY," +
                            "    task_id          INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                            "    title            TEXT NOT NULL," +
                            "    completed        INTEGER NOT NULL DEFAULT 0," +
                            "    collaborator_id  INTEGER REFERENCES collaborators(collaborator_id)" +
                            ")"
            );

            System.out.println("[DB] Creating table: tags");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS tags (" +
                            "    tag_id   INTEGER PRIMARY KEY," +
                            "    task_id  INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                            "    keyword  TEXT NOT NULL" +
                            ")"
            );

            System.out.println("[DB] Creating table: activity_entries");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS activity_entries (" +
                            "    entry_id     INTEGER PRIMARY KEY," +
                            "    task_id      INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                            "    timestamp    TEXT NOT NULL," +
                            "    description  TEXT NOT NULL" +
                            ")"
            );

            System.out.println("[DB] Creating table: recurrence_patterns");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS recurrence_patterns (" +
                            "    recurrence_id  INTEGER PRIMARY KEY," +
                            "    task_id        INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                            "    type           TEXT NOT NULL," +
                            "    interval_val   INTEGER NOT NULL," +
                            "    start_date     TEXT NOT NULL," +
                            "    end_date       TEXT," +
                            "    weekdays       TEXT," +
                            "    day_of_month   INTEGER" +
                            ")"
            );

            System.out.println("[DB] Creating table: task_occurrences");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS task_occurrences (" +
                            "    occurrence_id  INTEGER PRIMARY KEY," +
                            "    task_id        INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                            "    due_date       TEXT NOT NULL," +
                            "    status         TEXT NOT NULL" +
                            ")"
            );

            System.out.println("[DB] Creating table: task_collaborators");
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS task_collaborators (" +
                            "    task_id          INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                            "    collaborator_id  INTEGER NOT NULL REFERENCES collaborators(collaborator_id)," +
                            "    PRIMARY KEY (task_id, collaborator_id)" +
                            ")"
            );

            System.out.println("[DB] Schema initialised successfully.");
        }
    }
}