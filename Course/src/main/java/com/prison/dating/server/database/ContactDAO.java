package main.java.com.prison.dating.server.database;

import main.java.com.prison.dating.server.entities.ContactEntity;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {

    public List<ContactEntity> getContactsByPrisonerId(int prisonerId) {
        List<ContactEntity> contacts = new ArrayList<>();

        String sql = """
        SELECT c.*, pc.prisoner_id as link_prisoner_id
        FROM contacts c
        JOIN prisoner_contacts pc ON c.contact_id = pc.contact_id
        WHERE pc.prisoner_id = ?
        ORDER BY c.full_name
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prisonerId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("=== DEBUG: Загружаем контакты для prisonerId=" + prisonerId + " ===");

            while (rs.next()) {
                ContactEntity contact = new ContactEntity();

                // ДЕБАГ: Показываем все значения из ResultSet
                System.out.println("--- Строка ResultSet ---");
                int contactId = rs.getInt("contact_id");
                System.out.println("contact_id из БД: " + contactId);
                System.out.println("full_name из БД: " + rs.getString("full_name"));

                // Проверяем is_approved
                try {
                    boolean isApproved = rs.getBoolean("is_approved");
                    System.out.println("is_approved из БД: " + isApproved);
                    contact.setApproved(isApproved);
                } catch (SQLException e) {
                    System.out.println("Колонка is_approved не найдена, используем false");
                    contact.setApproved(false);
                }

                contact.setContactId(contactId);
                contact.setFullName(rs.getString("full_name"));

                java.sql.Date birthDate = rs.getDate("birth_date");
                if (birthDate != null) {
                    contact.setBirthDate(birthDate.toLocalDate());
                }

                contact.setRelation(rs.getString("relation"));

                contacts.add(contact);
                System.out.println("Создан ContactEntity: id=" + contact.getContactId() +
                        ", approved=" + contact.isApproved());
            }

            System.out.println("Итого контактов: " + contacts.size());

        } catch (SQLException e) {
            System.err.println("Ошибка получения контактов: " + e.getMessage());
            e.printStackTrace();
        }

        return contacts;
    }

    public ContactEntity getContactById(int contactId) {
        String sql = "SELECT * FROM contacts WHERE contact_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, contactId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ContactEntity contact = new ContactEntity();
                    contact.setContactId(rs.getInt("contact_id"));
                    contact.setFullName(rs.getString("full_name"));
                    contact.setRelation(rs.getString("relation"));
                    contact.setApproved(rs.getBoolean("is_approved"));

                    java.sql.Date birthDate = rs.getDate("birth_date");
                    if (birthDate != null) {
                        contact.setBirthDate(birthDate.toLocalDate());
                    }

                    return contact;
                }
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при получении контакта по ID " + contactId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateContactApproval(int prisonerId, int contactId, boolean approved) {
        System.out.println("=== ContactDAO.updateContactApproval ===");
        System.out.println("prisonerId: " + prisonerId + ", contactId: " + contactId + ", approved: " + approved);

        // Обновляем глобально в таблице contacts (но проверяем связь)
        String sql = """
        UPDATE contacts c 
        SET is_approved = ? 
        WHERE c.contact_id = ? 
        AND EXISTS (
            SELECT 1 FROM prisoner_contacts pc 
            WHERE pc.contact_id = c.contact_id 
            AND pc.prisoner_id = ?
        )
    """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setBoolean(1, approved);
                pstmt.setInt(2, contactId);
                pstmt.setInt(3, prisonerId); // Проверяем связь

                int rowsUpdated = pstmt.executeUpdate();
                System.out.println("Строк обновлено: " + rowsUpdated);

                return rowsUpdated > 0;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public ContactEntity getContactDetails(int contactId, int prisonerId) {
        String sql = """
        SELECT c.*, pc.is_approved, pc.prisoner_id
        FROM contacts c
        JOIN prisoner_contacts pc ON c.contact_id = pc.contact_id
        WHERE c.contact_id = ? AND pc.prisoner_id = ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, contactId);
            pstmt.setInt(2, prisonerId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ContactEntity contact = new ContactEntity();
                contact.setContactId(rs.getInt("contact_id"));
                contact.setFullName(rs.getString("full_name"));

                java.sql.Date birthDate = rs.getDate("birth_date");
                if (birthDate != null) {
                    contact.setBirthDate(birthDate.toLocalDate());
                }

                contact.setRelation(rs.getString("relation"));
                contact.setApproved(rs.getBoolean("is_approved"));

                return contact;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка получения деталей контакта: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    public boolean isContactApprovedForPrisoner(int contactId, int prisonerId) {
        System.out.println("=== ContactDAO.isContactApprovedForPrisoner ===");
        System.out.println("contactId: " + contactId + ", prisonerId: " + prisonerId);

        // Теперь проверяем глобальное одобрение в contacts
        String sql = "SELECT c.is_approved FROM contacts c " +
                "WHERE c.contact_id = ? AND " +
                "EXISTS (SELECT 1 FROM prisoner_contacts pc " +
                "        WHERE pc.contact_id = c.contact_id AND pc.prisoner_id = ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, contactId);
            pstmt.setInt(2, prisonerId);

            System.out.println("SQL: " + sql);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                boolean isApproved = rs.getBoolean("is_approved");
                System.out.println("Контакт одобрен: " + isApproved);
                return isApproved;
            } else {
                System.out.println("Контакт не найден или не связан с заключённым");
                return false;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка проверки одобрения: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<ContactEntity> getAllContacts() {
        List<ContactEntity> contacts = new ArrayList<>();
        String sql = "SELECT * FROM contacts ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                ContactEntity contact = new ContactEntity();
                contact.setContactId(rs.getInt("contact_id"));
                contact.setFullName(rs.getString("full_name"));
                contact.setRelation(rs.getString("relation"));
                contact.setApproved(rs.getBoolean("is_approved"));

                java.sql.Date birthDate = rs.getDate("birth_date");
                if (birthDate != null) {
                    contact.setBirthDate(birthDate.toLocalDate());
                }

                contacts.add(contact);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при получении всех контактов: " + e.getMessage());
            e.printStackTrace();
        }

        return contacts;
    }



    public boolean addContact(ContactEntity contact) {
        String sql = """
            INSERT INTO contacts (full_name, birth_date, relation, is_approved) 
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, contact.getFullName());

            if (contact.getBirthDate() != null) {
                pstmt.setDate(2, java.sql.Date.valueOf(contact.getBirthDate()));
            } else {
                pstmt.setNull(2, java.sql.Types.DATE);
            }

            pstmt.setString(3, contact.getRelation());
            pstmt.setBoolean(4, contact.isApproved());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка при добавлении контакта: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteContact(int contactId) {
        System.out.println("=== DEBUG ContactDAO.deleteContact(" + contactId + ") ===");

        String sql = "DELETE FROM contacts WHERE contact_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("1. Подключение к БД установлено");
            System.out.println("2. Connection URL: " + conn.getMetaData().getURL());
            System.out.println("3. Auto-commit режим: " + conn.getAutoCommit());

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                System.out.println("4. PreparedStatement создан");
                System.out.println("5. SQL: " + sql);
                System.out.println("6. Параметр contactId: " + contactId);

                stmt.setInt(1, contactId);
                System.out.println("7. Параметр установлен");

                int rowsAffected = stmt.executeUpdate();
                System.out.println("8. Выполнено executeUpdate()");
                System.out.println("9. Строк удалено: " + rowsAffected);

                if (rowsAffected > 0) {
                    System.out.println("Контакт удалён из БД");
                } else {
                    System.out.println("⚠Контакт не найден в БД (0 строк удалено)");
                }

                return rowsAffected > 0;

            } catch (SQLException e) {
                System.err.println("   Ошибка в PreparedStatement:");
                System.err.println("   SQLState: " + e.getSQLState());
                System.err.println("   Error Code: " + e.getErrorCode());
                System.err.println("   Message: " + e.getMessage());
                e.printStackTrace();
                return false;
            }

        } catch (SQLException e) {
            System.err.println("   Ошибка подключения к БД:");
            System.err.println("   Message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public ContactEntity getContactByFullName(String fullName) {
        String sql = "SELECT * FROM contacts WHERE full_name = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, fullName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                ContactEntity contact = new ContactEntity();
                contact.setContactId(rs.getInt("contact_id"));
                contact.setFullName(rs.getString("full_name"));

                // Обрабатываем дату рождения
                java.sql.Date sqlDate = rs.getDate("birth_date");
                if (sqlDate != null) {
                    contact.setBirthDate(sqlDate.toLocalDate());
                }

                contact.setRelation(rs.getString("relation"));
                // Добавьте другие поля если есть: phone, address и т.д.

                return contact;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка поиска контакта по имени: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Добавить связь заключённый-контакт
    public boolean addPrisonerContact(int prisonerId, int contactId) {
        String sql = "INSERT INTO prisoner_contacts (prisoner_id, contact_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prisonerId);
            pstmt.setInt(2, contactId);

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("Добавлена связь: prisoner_id=" + prisonerId + ", contact_id=" + contactId);
            return rowsInserted > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка добавления связи prisoner-contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Удалить связь заключённый-контакт
    public boolean removePrisonerContact(int prisonerId, int contactId) {
        String sql = "DELETE FROM prisoner_contacts WHERE prisoner_id = ? AND contact_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prisonerId);
            pstmt.setInt(2, contactId);

            int rowsDeleted = pstmt.executeUpdate();
            System.out.println("Удалена связь: prisoner_id=" + prisonerId + ", contact_id=" + contactId);
            return rowsDeleted > 0;

        } catch (SQLException e) {
            System.err.println("Ошибка удаления связи prisoner-contact: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Проверить, есть ли у контакта связи с другими заключёнными
    public boolean hasOtherPrisonerLinks(int contactId, int excludePrisonerId) {
        String sql = "SELECT COUNT(*) FROM prisoner_contacts WHERE contact_id = ? AND prisoner_id != ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, contactId);
            pstmt.setInt(2, excludePrisonerId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("DEBUG: Контакт " + contactId + " имеет " + count +
                        " других связей кроме prisonerId=" + excludePrisonerId);
                return count > 0;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка проверки других связей: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Проверить, существует ли связь
    public boolean contactExistsForPrisoner(int prisonerId, int contactId) {
        String sql = "SELECT COUNT(*) FROM prisoner_contacts WHERE prisoner_id = ? AND contact_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, prisonerId);
            pstmt.setInt(2, contactId);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Ошибка проверки связи: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}