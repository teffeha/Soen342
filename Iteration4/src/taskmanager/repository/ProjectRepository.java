package taskmanager.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Table Data Gateway for the projects table.
 */
public class ProjectRepository {

    private final Connection conn;

    public ProjectRepository(Connection conn) {
        this.conn = conn;
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    public void insert(long projectId, String name, String description) throws SQLException {
        String sql = "INSERT INTO projects(project_id, name, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, projectId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.executeUpdate();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void update(long projectId, String name, String description) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ? WHERE project_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setLong(3, projectId);
            ps.executeUpdate();
        }
    }

    // ── LOAD ──────────────────────────────────────────────────────────────────

    public List<ProjectRow> loadAll() throws SQLException {
        List<ProjectRow> rows = new ArrayList<>();
        String sql = "SELECT project_id, name, description FROM projects";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new ProjectRow(
                        rs.getLong("project_id"),
                        rs.getString("name"),
                        rs.getString("description")));
            }
        }
        return rows;
    }

    public long maxId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(project_id), 0) FROM projects";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ── ROW TYPE ──────────────────────────────────────────────────────────────

    public record ProjectRow(long projectId, String name, String description) {}
}
