package taskmanager.repository;

import taskmanager.enums.CollaboratorCategory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Table Data Gateway for the collaborators table.
 */
public class CollaboratorRepository {

    private final Connection conn;

    public CollaboratorRepository(Connection conn) {
        this.conn = conn;
    }

    // ── INSERT ────────────────────────────────────────────────────────────────

    public void insert(long collaboratorId, String name,
                       CollaboratorCategory category, Long projectId) throws SQLException {
        String sql = "INSERT INTO collaborators(collaborator_id, name, category, project_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, collaboratorId);
            ps.setString(2, name);
            ps.setString(3, category.name());
            if (projectId != null) ps.setLong(4, projectId);
            else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public void updateProjectId(long collaboratorId, Long projectId) throws SQLException {
        String sql = "UPDATE collaborators SET project_id = ? WHERE collaborator_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (projectId != null) ps.setLong(1, projectId);
            else ps.setNull(1, Types.INTEGER);
            ps.setLong(2, collaboratorId);
            ps.executeUpdate();
        }
    }

    // ── LOAD ──────────────────────────────────────────────────────────────────

    public List<CollaboratorRow> loadAll() throws SQLException {
        List<CollaboratorRow> rows = new ArrayList<>();
        String sql = "SELECT collaborator_id, name, category, project_id FROM collaborators";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                long rawProjectId = rs.getLong("project_id");
                Long projectId = rs.wasNull() ? null : rawProjectId;
                rows.add(new CollaboratorRow(
                        rs.getLong("collaborator_id"),
                        rs.getString("name"),
                        CollaboratorCategory.valueOf(rs.getString("category")),
                        projectId));
            }
        }
        return rows;
    }

    public long maxId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(collaborator_id), 0) FROM collaborators";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    // ── ROW TYPE ──────────────────────────────────────────────────────────────

    public record CollaboratorRow(long collaboratorId, String name,
                                  CollaboratorCategory category, Long projectId) {}
}
