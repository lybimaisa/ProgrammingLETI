package main.java.com.prison.dating.server.database;

import main.java.com.prison.dating.server.entities.VisitEntity;
import main.java.com.prison.dating.server.entities.VisitRequestEntity;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VisitRequestDAO {

    public boolean hasExistingRequest(int prisonerId, int contactId, LocalDate visitDate) {
        String sql = """
            SELECT COUNT(*) as count FROM visit_requests 
            WHERE prisoner_id = ? AND contact_id = ? AND visit_date = ? 
            AND status IN ('ожидает', 'одобрена')
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prisonerId);
            pstmt.setInt(2, contactId);
            pstmt.setDate(3, java.sql.Date.valueOf(visitDate));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при проверке существующего запроса: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean createVisitRequest(VisitRequestEntity request) {
        String sql = """
            INSERT INTO visit_requests 
            (prisoner_id, contact_id, request_date, visit_date, visit_type, status) 
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, request.getPrisonerId());
            pstmt.setInt(2, request.getContactId());
            pstmt.setDate(3, java.sql.Date.valueOf(request.getRequestDate()));
            pstmt.setDate(4, java.sql.Date.valueOf(request.getVisitDate()));
            pstmt.setString(5, request.getVisitType());
            pstmt.setString(6, request.getStatus());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка при создании запроса на свидание: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public List<VisitRequestEntity> getAllRequests() {
        List<VisitRequestEntity> requests = new ArrayList<>();
        String sql = "SELECT * FROM visit_requests ORDER BY request_date DESC";

        System.out.println("=== DEBUG: VisitRequestDAO.getAllRequests() ===");
        System.out.println("SQL: " + sql);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                count++;
                VisitRequestEntity request = new VisitRequestEntity();
                request.setRequestId(rs.getInt("request_id"));
                request.setPrisonerId(rs.getInt("prisoner_id"));
                request.setContactId(rs.getInt("contact_id"));
                request.setRequestDate(rs.getDate("request_date").toLocalDate());
                request.setVisitDate(rs.getDate("visit_date").toLocalDate());
                request.setVisitType(rs.getString("visit_type"));
                request.setStatus(rs.getString("status"));

                // Логируем каждую запись
                System.out.println("Запись #" + count + ": ID=" + request.getRequestId() +
                        ", prisoner=" + request.getPrisonerId() +
                        ", date=" + request.getVisitDate() +
                        ", type=" + request.getVisitType() +
                        ", status=" + request.getStatus());

                requests.add(request);
            }

            System.out.println("Всего найдено записей: " + count);

        } catch (SQLException e) {
            System.err.println("Ошибка при получении всех запросов: " + e.getMessage());
            e.printStackTrace();
        }

        return requests;
    }

    public List<VisitRequestEntity> getRequestsByPrisonerId(int prisonerId) {
        List<VisitRequestEntity> requests = new ArrayList<>();
        String sql = "SELECT * FROM visit_requests WHERE prisoner_id = ? ORDER BY visit_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prisonerId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VisitRequestEntity request = new VisitRequestEntity();
                    request.setRequestId(rs.getInt("request_id"));
                    request.setPrisonerId(rs.getInt("prisoner_id"));
                    request.setContactId(rs.getInt("contact_id"));
                    request.setRequestDate(rs.getDate("request_date").toLocalDate());
                    request.setVisitDate(rs.getDate("visit_date").toLocalDate());
                    request.setVisitType(rs.getString("visit_type"));
                    request.setStatus(rs.getString("status"));

                    requests.add(request);
                }
            }

            System.out.println("=== DEBUG getRequestsByPrisonerId ===");
            System.out.println("prisonerId: " + prisonerId);
            System.out.println("Найдено заявок: " + requests.size());
            for (VisitRequestEntity req : requests) {
                System.out.println("  Заявка #" + req.getRequestId() +
                        ", статус: " + req.getStatus() +
                        ", дата: " + req.getVisitDate());
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при получении запросов: " + e.getMessage());
            e.printStackTrace();
        }

        return requests;
    }

    public boolean cancelRequest(int requestId) {
        String sql = "UPDATE visit_requests SET status = 'отменена' WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, requestId);
            int rows = pstmt.executeUpdate();

            System.out.println("Отменена заявка #" + requestId + ", затронуто строк: " + rows);
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка отмены заявки #" + requestId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean updateRequestStatus(int requestId, String status) {
        String sql = "UPDATE visit_requests SET status = ? WHERE request_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении статуса запроса: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean approveVisitRequestWithLimitUpdate(int requestId, String visitType) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            System.out.println("=== ОДОБРЕНИЕ ЗАПРОСА #" + requestId + " (БЕЗ ИЗМЕНЕНИЯ ЛИМИТОВ) ===");

            String getRequestSql = "SELECT prisoner_id, contact_id, visit_date FROM visit_requests WHERE request_id = ?";
            int prisonerId = 0;
            int contactId = 0;
            Date visitDate = null;

            try (PreparedStatement pstmt = conn.prepareStatement(getRequestSql)) {
                pstmt.setInt(1, requestId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    prisonerId = rs.getInt("prisoner_id");
                    contactId = rs.getInt("contact_id");
                    visitDate = rs.getDate("visit_date");
                    System.out.println("Данные запроса: prisoner=" + prisonerId +
                            ", contact=" + contactId + ", date=" + visitDate);
                } else {
                    System.err.println("Запрос #" + requestId + " не найден!");
                    conn.rollback();
                    return false;
                }
            }

            String updateRequestSql = "UPDATE visit_requests SET status = 'одобрена' WHERE request_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateRequestSql)) {
                pstmt.setInt(1, requestId);
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    System.err.println("Не удалось обновить статус запроса #" + requestId);
                    conn.rollback();
                    return false;
                }
                System.out.println("Статус запроса обновлен на 'одобрена'");
            }

            String createVisitSql = """
            INSERT INTO visits (prisoner_id, contact_id, visit_date, visit_type, status)
            VALUES (?, ?, ?, ?, 'подтверждено')
            """;
            try (PreparedStatement pstmt = conn.prepareStatement(createVisitSql)) {
                pstmt.setInt(1, prisonerId);
                pstmt.setInt(2, contactId);
                pstmt.setDate(3, visitDate);
                pstmt.setString(4, visitType);
                int rowsInserted = pstmt.executeUpdate();
                System.out.println("Создано свидание в visits (статус: подтверждено): " + rowsInserted);

                if (rowsInserted == 0) {
                    System.err.println("Не удалось создать свидание!");
                    conn.rollback();
                    return false;
                }
            }
            System.out.println("Лимиты НЕ изменены (будут изменены только при отметке 'состоялось')");

            conn.commit();
            System.out.println("=== ЗАПРОС #" + requestId + " ОДОБРЕН БЕЗ ИЗМЕНЕНИЯ ЛИМИТОВ ===");
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) {}
            }
            System.err.println("Ошибка при одобрении запроса #" + requestId + ":");
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
}