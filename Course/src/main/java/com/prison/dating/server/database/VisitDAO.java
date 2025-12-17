package main.java.com.prison.dating.server.database;

import main.java.com.prison.dating.server.entities.VisitEntity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VisitDAO {
    private Connection connection;

    public VisitDAO() {
        try {
            this.connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных в VisitDAO:");
            e.printStackTrace();
            this.connection = null;
        }
    }

    // Обновить статус свидания
    public boolean updateVisitStatus(int visitId, String status) {
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return false;
        }

        String sql = "UPDATE visits SET status = ? WHERE visit_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, visitId);

            int rowsUpdated = pstmt.executeUpdate();
            System.out.println("Обновлено свиданий: " + rowsUpdated);
            return rowsUpdated > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении статуса свидания #" + visitId + ":");
            e.printStackTrace();
            return false;
        }
    }

    // Получить все свидания
    public List<VisitEntity> getAllVisits() {
        List<VisitEntity> visits = new ArrayList<>();
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return visits;
        }

        String sql = "SELECT * FROM visits ORDER BY visit_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                VisitEntity visit = mapResultSetToVisit(rs);
                visits.add(visit);
            }

            System.out.println("Загружено свиданий: " + visits.size());

        } catch (SQLException e) {
            System.err.println("Ошибка при получении всех свиданий:");
            e.printStackTrace();
        }

        return visits;
    }

    // Получить свидания заключенного
    public List<VisitEntity> getVisitsByPrisonerId(int prisonerId) {
        List<VisitEntity> visits = new ArrayList<>();
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return visits;
        }

        String sql = "SELECT * FROM visits WHERE prisoner_id = ? ORDER BY visit_date DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prisonerId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                VisitEntity visit = mapResultSetToVisit(rs);
                visits.add(visit);
            }

            System.out.println("Загружено свиданий для заключенного #" + prisonerId + ": " + visits.size());

        } catch (SQLException e) {
            System.err.println("Ошибка при получении свиданий заключенного #" + prisonerId + ":");
            e.printStackTrace();
        }

        return visits;
    }

    // Получить свидание по ID
    public VisitEntity getVisitById(int visitId) {
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return null;
        }

        String sql = "SELECT * FROM visits WHERE visit_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, visitId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToVisit(rs);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при получении свидания #" + visitId + ":");
            e.printStackTrace();
        }

        return null;
    }

    // Отметить свидание как состоявшееся
    public boolean markVisitAsCompleted(int visitId) {
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return false;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Получаем информацию о свидании
            String getVisitSql = "SELECT prisoner_id, visit_type FROM visits WHERE visit_id = ?";
            int prisonerId = 0;
            String visitType = "";

            try (PreparedStatement pstmt = conn.prepareStatement(getVisitSql)) {
                pstmt.setInt(1, visitId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    prisonerId = rs.getInt("prisoner_id");
                    visitType = rs.getString("visit_type");
                } else {
                    conn.rollback();
                    return false;
                }
            }

            // 2. Обновляем статус свидания
            String updateVisitSql = "UPDATE visits SET status = 'состоялось' WHERE visit_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateVisitSql)) {
                pstmt.setInt(1, visitId);
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return false;
                }
                System.out.println("Статус свидания #" + visitId + " обновлен на 'состоялось'");
            }

            // 3. Увеличиваем счетчик в лимитах
            String updateLimitSql;
            if ("краткосрочное".equalsIgnoreCase(visitType)) {
                updateLimitSql = "UPDATE visit_limits SET short_used = short_used + 1 WHERE prisoner_id = ?";
                System.out.println("Увеличиваем счетчик краткосрочных свиданий для prisoner #" + prisonerId);
            } else {
                updateLimitSql = "UPDATE visit_limits SET long_used = long_used + 1 WHERE prisoner_id = ?";
                System.out.println("Увеличиваем счетчик длительных свиданий для prisoner #" + prisonerId);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(updateLimitSql)) {
                pstmt.setInt(1, prisonerId);
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    System.err.println("Не найден лимит для prisoner #" + prisonerId);
                    conn.rollback();
                    return false;
                }
                System.out.println("Лимиты обновлены для prisoner #" + prisonerId);
            }

            conn.commit();
            System.out.println("Свидание #" + visitId + " успешно отмечено как состоявшееся");
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            System.err.println("Ошибка при отметке свидания как состоявшегося #" + visitId + ":");
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {}
            }
        }
    }

    // Удалить свидание
    public boolean deleteVisit(int visitId) {
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return false;
        }

        String sql = "DELETE FROM visits WHERE visit_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, visitId);

            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("Удалено свиданий: " + rowsDeleted);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка при удалении свидания #" + visitId + ":");
            e.printStackTrace();
            return false;
        }
    }

    // Получить количество свиданий по статусу
    public int getVisitsCountByStatus(int prisonerId, String status) {
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return 0;
        }

        String sql = "SELECT COUNT(*) as count FROM visits WHERE prisoner_id = ? AND status = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prisonerId);
            pstmt.setString(2, status);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при подсчете свиданий:");
            e.printStackTrace();
        }

        return 0;
    }

    // Вспомогательный метод для маппинга ResultSet
    private VisitEntity mapResultSetToVisit(ResultSet rs) throws SQLException {
        VisitEntity visit = new VisitEntity();
        visit.setVisitId(rs.getInt("visit_id"));
        visit.setPrisonerId(rs.getInt("prisoner_id"));
        visit.setContactId(rs.getInt("contact_id"));

        Date visitDate = rs.getDate("visit_date");
        if (visitDate != null) {
            visit.setVisitDate(visitDate.toLocalDate());
        }

        visit.setVisitType(rs.getString("visit_type"));
        visit.setStatus(rs.getString("status"));

        return visit;
    }
}