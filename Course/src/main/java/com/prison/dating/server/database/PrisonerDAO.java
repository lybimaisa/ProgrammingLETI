package main.java.com.prison.dating.server.database;

import main.java.com.prison.dating.server.entities.PrisonerEntity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PrisonerDAO {

    // Получить всех заключенных
    public List<PrisonerEntity> getAllPrisoners() {
        List<PrisonerEntity> prisoners = new ArrayList<>();
        String sql = "SELECT prisoner_id, prisoner_number, full_name, birth_date FROM prisoners";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                PrisonerEntity prisoner = new PrisonerEntity();
                prisoner.setPrisonerId(rs.getInt("prisoner_id"));
                prisoner.setPrisonerNumber(rs.getString("prisoner_number"));
                prisoner.setFullName(rs.getString("full_name"));
                java.sql.Date sqlDate = rs.getDate("birth_date");
                if (sqlDate != null) {
                    prisoner.setBirthDate(sqlDate.toLocalDate());
                } else {
                    prisoner.setBirthDate(null);
                }

                prisoners.add(prisoner);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prisoners;
    }

    // Получить заключенного по ID
    public PrisonerEntity getPrisonerById(int id) {
        String sql = "SELECT * FROM prisoners WHERE prisoner_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                PrisonerEntity prisoner = new PrisonerEntity();
                prisoner.setPrisonerId(rs.getInt("prisoner_id"));
                prisoner.setFullName(rs.getString("full_name"));
                prisoner.setBirthDate(rs.getDate("birth_date").toLocalDate());
                prisoner.setPrisonerNumber(rs.getString("prisoner_number"));
                return prisoner;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при поиске заключенного: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    // Добавить нового заключенного
    public boolean addPrisoner(PrisonerEntity prisoner) {
        System.out.println("=== PrisonerDAO.addPrisoner() ===");
        System.out.println("Данные для сохранения:");
        System.out.println("  Номер: " + prisoner.getPrisonerNumber());
        System.out.println("  ФИО: " + prisoner.getFullName());
        System.out.println("  Дата рождения: " + prisoner.getBirthDate());

        String sql = "INSERT INTO prisoners (prisoner_number, full_name, birth_date) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Соединение с БД получено");

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                System.out.println("Подготавливаем SQL: " + sql);

                stmt.setString(1, prisoner.getPrisonerNumber());
                stmt.setString(2, prisoner.getFullName());

                if (prisoner.getBirthDate() != null) {
                    java.sql.Date sqlDate = java.sql.Date.valueOf(prisoner.getBirthDate());
                    stmt.setDate(3, sqlDate);
                    System.out.println("Дата рождения: " + sqlDate);
                } else {
                    stmt.setNull(3, java.sql.Types.DATE);
                    System.out.println("Дата рождения: NULL");
                }

                System.out.println("Выполняем SQL...");
                int rowsAffected = stmt.executeUpdate();
                System.out.println("Затронуто строк: " + rowsAffected);

                return rowsAffected > 0;

            } catch (SQLException e) {
                System.err.println("SQLException в PreparedStatement: " + e.getMessage());
                System.err.println("SQLState: " + e.getSQLState());
                System.err.println("VendorError: " + e.getErrorCode());
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("SQLException при подключении к БД: " + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Общая ошибка в addPrisoner: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean deletePrisonerWithRelations(int prisonerId) {
        System.out.println("PrisonerDAO.deletePrisonerWithRelations(" + prisonerId + ")");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            System.out.println("Удаляем все связанные записи для prisonerId=" + prisonerId);

            deleteVisitRequestsByPrisonerId(conn, prisonerId);

            deleteVisitLimitsByPrisonerId(conn, prisonerId);

            deletePrisonerContacts(conn, prisonerId);

            String sql = "DELETE FROM prisoners WHERE prisoner_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, prisonerId);
                int rows = stmt.executeUpdate();
                System.out.println("Удалено заключённых: " + rows);

                if (rows > 0) {
                    conn.commit();
                    System.out.println("Транзакция успешно завершена!");
                    return true;
                } else {
                    conn.rollback();
                    System.out.println("Заключённый не найден, транзакция откачена");
                    return false;
                }
            }

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            System.err.println("Ошибка в deletePrisonerWithRelations: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Удаление связей заключённый-контакт
    private void deletePrisonerContacts(Connection conn, int prisonerId) throws SQLException {
        String findContactsSql = """
        SELECT pc.contact_id 
        FROM prisoner_contacts pc
        LEFT JOIN prisoner_contacts pc2 ON pc.contact_id = pc2.contact_id AND pc2.prisoner_id != ?
        WHERE pc.prisoner_id = ? AND pc2.contact_id IS NULL
    """;

        List<Integer> contactsToDelete = new ArrayList<>();

        try (PreparedStatement findStmt = conn.prepareStatement(findContactsSql)) {
            findStmt.setInt(1, prisonerId);
            findStmt.setInt(2, prisonerId);

            try (ResultSet rs = findStmt.executeQuery()) {
                while (rs.next()) {
                    contactsToDelete.add(rs.getInt("contact_id"));
                }
            }
        }

        System.out.println("Найдено контактов для удаления: " + contactsToDelete.size());

        String deleteLinksSql = "DELETE FROM prisoner_contacts WHERE prisoner_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteLinksSql)) {
            stmt.setInt(1, prisonerId);
            int rows = stmt.executeUpdate();
            System.out.println("Удалено связей из prisoner_contact: " + rows);
        }

        if (!contactsToDelete.isEmpty()) {
            String deleteContactsSql = "DELETE FROM contacts WHERE contact_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteContactsSql)) {
                for (int contactId : contactsToDelete) {
                    stmt.setInt(1, contactId);
                    stmt.addBatch();
                }
                int[] rows = stmt.executeBatch();
                System.out.println("Удалено контактов: " + rows.length);
            }
        }
    }

    // Метод для удаления запросов на свидания
    private void deleteVisitRequestsByPrisonerId(Connection conn, int prisonerId) throws SQLException {
        String sql = "DELETE FROM visit_requests WHERE prisoner_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prisonerId);
            int rows = stmt.executeUpdate();
            System.out.println("Удалено запросов на свидания: " + rows);
        }
    }

    // Метод для удаления лимитов
    private void deleteVisitLimitsByPrisonerId(Connection conn, int prisonerId) throws SQLException {
        String sql = "DELETE FROM visit_limits WHERE prisoner_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, prisonerId);
            int rows = stmt.executeUpdate();
            System.out.println("Удалено лимитов: " + rows);
        }
    }

    public boolean checkPassword(int prisonerId, String password) {
        String sql = "SELECT COUNT(*) FROM prisoners WHERE prisoner_id = ? AND password = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, prisonerId);
            stmt.setString(2, password);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("DAO: Проверка пароля prisonerId=" + prisonerId +
                            ", результат=" + (count > 0));
                    return count > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Ошибка проверки пароля в DAO: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}
