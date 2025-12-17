package main.java.com.prison.dating.server.database;

import main.java.com.prison.dating.api.models.VisitLimit;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

public class VisitLimitDAO {
    private Connection connection;

    public VisitLimitDAO() {
        try {
            this.connection = DatabaseConnection.getConnection();
        } catch (SQLException e) {
            System.err.println("Ошибка подключения к базе данных в VisitLimitDAO:");
            e.printStackTrace();
            this.connection = null;
        }
    }

    public VisitLimit getLimitByPrisonerId(int prisonerId) {
        if (connection == null) {
            System.err.println("Нет подключения к базе данных!");
            return null;
        }

        String sql = "SELECT * FROM visit_limits WHERE prisoner_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, prisonerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                VisitLimit limit = new VisitLimit();
                limit.setLimitId(rs.getInt("limit_id"));
                limit.setPrisonerId(rs.getInt("prisoner_id"));
                limit.setShortAllowed(rs.getInt("short_allowed"));
                limit.setLongAllowed(rs.getInt("long_allowed"));
                limit.setShortUsed(rs.getInt("short_used"));
                limit.setLongUsed(rs.getInt("long_used"));
                return limit;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении лимитов для заключенного " + prisonerId + ":");
            e.printStackTrace();
        }
        return null;
    }

    public boolean canRequestVisit(int prisonerId, String visitType) {
        VisitLimitDAO limitDAO = new VisitLimitDAO();
        VisitLimit limit = limitDAO.getLimitByPrisonerId(prisonerId);

        if (limit == null) return false;

        if ("краткосрочное".equalsIgnoreCase(visitType)) {
            // Проверяем, что short_allowed > short_used
            return limit.getShortAllowed() > limit.getShortUsed();
        } else if ("длительное".equalsIgnoreCase(visitType)) {
            // Проверяем, что long_allowed > long_used
            return limit.getLongAllowed() > limit.getLongUsed();
        }

        return false;
    }

    // Обновить использованные краткосрочные свидания
    public boolean updateShortUsed(int prisonerId, int newShortUsed) {
        if (connection == null) {
            System.err.println("Нет подключения к базе данных!");
            return false;
        }

        String sql = "UPDATE visit_limits SET short_used = ? WHERE prisoner_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newShortUsed);
            pstmt.setInt(2, prisonerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении краткосрочных свиданий:");
            e.printStackTrace();
        }
        return false;
    }

    // Обновить использованные длительные свидания
    public boolean updateLongUsed(int prisonerId, int newLongUsed) {
        if (connection == null) {
            System.err.println("Нет подключения к базе данных!");
            return false;
        }

        String sql = "UPDATE visit_limits SET long_used = ? WHERE prisoner_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, newLongUsed);
            pstmt.setInt(2, prisonerId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении длительных свиданий:");
            e.printStackTrace();
        }
        return false;
    }

    // Создать новый лимит
    public boolean createVisitLimit(VisitLimit limit) {
        if (connection == null) {
            System.err.println("Нет подключения к БД!");
            return false;
        }

        String sql = """
        INSERT INTO visit_limits 
        (prisoner_id, short_allowed, long_allowed, short_used, long_used) 
        VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            System.out.println("Создание лимитов в БД:");
            System.out.println("  prisoner_id: " + limit.getPrisonerId());
            System.out.println("  short_allowed: " + limit.getShortAllowed());
            System.out.println("  long_allowed: " + limit.getLongAllowed());
            System.out.println("  short_used: " + limit.getShortUsed());
            System.out.println("  long_used: " + limit.getLongUsed());

            pstmt.setInt(1, limit.getPrisonerId());
            pstmt.setInt(2, limit.getShortAllowed());
            pstmt.setInt(3, limit.getLongAllowed());
            pstmt.setInt(4, limit.getShortUsed());
            pstmt.setInt(5, limit.getLongUsed());

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Добавлено строк лимитов: " + rowsInserted);
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("❌ Ошибка при создании лимитов для prisonerId " + limit.getPrisonerId() + ":");
            System.err.println("SQLState: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}