package main.java.com.prison.dating.api.services;

import main.java.com.prison.dating.server.database.PrisonerDAO;
import main.java.com.prison.dating.server.database.VisitLimitDAO;
import main.java.com.prison.dating.server.database.ContactDAO;
import main.java.com.prison.dating.server.database.VisitRequestDAO;
import main.java.com.prison.dating.server.entities.PrisonerEntity;
import main.java.com.prison.dating.server.entities.ContactEntity;
import main.java.com.prison.dating.api.models.Prisoner;
import main.java.com.prison.dating.api.models.Contact;
import main.java.com.prison.dating.api.models.VisitLimit;

import java.util.List;
import java.util.stream.Collectors;

public class PrisonerService {
    private PrisonerDAO prisonerDAO;
    private ContactDAO contactDAO;
    private VisitLimitDAO limitDAO;
    private VisitRequestDAO visitRequestDAO;

    public PrisonerService() {
        this.prisonerDAO = new PrisonerDAO();
        this.contactDAO = new ContactDAO();
        this.limitDAO = new VisitLimitDAO();
        this.visitRequestDAO = new VisitRequestDAO();
    }

    // Получить контакты заключенного (возвращаем DTO)
    public List<Contact> getPrisonerContacts(int prisonerId) {
        List<ContactEntity> entities = contactDAO.getContactsByPrisonerId(prisonerId);
        return entities.stream()
                .map(this::convertToContactDTO)
                .collect(Collectors.toList());
    }


    public String getRemainingVisits(int prisonerId) {
        VisitLimit limit = limitDAO.getLimitByPrisonerId(prisonerId);
        if (limit == null) {
            return "Лимиты не найдены";
        }

        System.out.println("=== ДЕБАГ В PrisonerService.getRemainingVisits() ===");
        System.out.println("Prisoner ID: " + prisonerId);
        System.out.println("Limit object: " + limit);
        System.out.println("shortAllowed: " + limit.getShortAllowed());
        System.out.println("shortUsed: " + limit.getShortUsed());
        System.out.println("longAllowed: " + limit.getLongAllowed());
        System.out.println("longUsed: " + limit.getLongUsed());
        System.out.println("shortRemaining (calculated): " + limit.getShortRemaining());
        System.out.println("longRemaining (calculated): " + limit.getLongRemaining());

        int shortRemaining = limit.getShortAllowed() - limit.getShortUsed();
        int longRemaining = limit.getLongAllowed() - limit.getLongUsed();

        return "Краткосрочных осталось: " + shortRemaining +
                ", Длительных осталось: " + longRemaining;
    }

    // Проверить возможность свидания
    public boolean canRequestVisit(int prisonerId, String visitType) {
        return limitDAO.canRequestVisit(prisonerId, visitType);
    }

    private Contact convertToContactDTO(ContactEntity entity) {
        System.out.println("=== DEBUG convertToContactDTO ===");
        System.out.println("Entity: id=" + entity.getContactId() +
                ", name=" + entity.getFullName() +
                ", approved=" + entity.isApproved());

        Contact dto = new Contact();
        dto.setContactId(entity.getContactId());  // Вот здесь может быть проблема!
        dto.setFullName(entity.getFullName());
        dto.setBirthDate(entity.getBirthDate());
        dto.setRelation(entity.getRelation());
        dto.setApproved(entity.isApproved());

        System.out.println("DTO: id=" + dto.getContactId() +
                ", approved=" + dto.isApproved());

        return dto;
    }

    public List<Prisoner> getAllPrisoners() {
        List<PrisonerEntity> entities = prisonerDAO.getAllPrisoners();

        System.out.println("=== DEBUG PrisonerService ===");
        System.out.println("Entities from DAO: " + entities.size());
        for (PrisonerEntity entity : entities) {
            System.out.println("ID: " + entity.getPrisonerId() +
                    ", Name: " + entity.getFullName() +
                    ", BirthDate (LocalDate): " + entity.getBirthDate() +
                    ", BirthDate toString: " +
                    (entity.getBirthDate() != null ? entity.getBirthDate().toString() : "NULL"));
        }

        List<Prisoner> result = entities.stream()
                .map(this::convertToPrisonerDTO)
                .collect(Collectors.toList());

        System.out.println("After conversion to DTO:");
        for (Prisoner p : result) {
            System.out.println("ID: " + p.getPrisonerId() +
                    ", BirthDate (String): [" + p.getBirthDate() + "]");
        }

        return result;
    }

    public String approveContact(int prisonerId, int contactId, boolean approved) {
        System.out.println("=== PrisonerService.approveContact ===");
        System.out.println("prisonerId: " + prisonerId + ", contactId: " + contactId + ", approved: " + approved);

        try {
            // Проверяем существование контакта и связь
            ContactEntity contact = contactDAO.getContactById(contactId);
            if (contact == null) {
                return "Ошибка: Контакт не найден";
            }

            // Проверяем связь с заключённым
            if (!contactDAO.contactExistsForPrisoner(prisonerId, contactId)) {
                return "Ошибка: Контакт не связан с этим заключённым";
            }

            // Обновляем статус
            boolean updated = contactDAO.updateContactApproval(prisonerId, contactId, approved);

            if (updated) {
                return approved ? "Успех: Контакт одобрен" : "Успех: Контакт отклонён";
            } else {
                return "Ошибка: Не удалось обновить статус";
            }

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    // Получить заключённого по ID
    public Prisoner getPrisonerById(int prisonerId) {
        // Используем DAO для получения Entity
        PrisonerEntity entity = prisonerDAO.getPrisonerById(prisonerId);

        if (entity == null) {
            return null;
        }

        // Конвертируем Entity в DTO
        return convertToPrisonerDTO(entity);
    }

    // Конвертация Entity → DTO
    private Prisoner convertToPrisonerDTO(PrisonerEntity entity) {
        Prisoner dto = new Prisoner();
        dto.setPrisonerId(entity.getPrisonerId());
        dto.setFullName(entity.getFullName() != null ? entity.getFullName() : "");
        dto.setPrisonerNumber(entity.getPrisonerNumber() != null ? entity.getPrisonerNumber() : "");

        // Просто передаем LocalDate как есть
        dto.setBirthDate(entity.getBirthDate());

        return dto;
    }

    public VisitLimit getVisitLimits(int prisonerId) {
        try {
            System.out.println("PrisonerService.getVisitLimits(" + prisonerId + ") вызван");
            VisitLimit limit = limitDAO.getLimitByPrisonerId(prisonerId);
            System.out.println("DAO вернул лимит: " + (limit != null ? "не null" : "null"));
            return limit;
        } catch (Exception e) {
            System.err.println("Ошибка в getVisitLimits: " + e.getMessage());
            return null;
        }
    }

    public boolean deletePrisonerWithRelations(int prisonerId) {
        System.out.println("PrisonerService.deletePrisonerWithRelations(" + prisonerId + ") вызван");

        try {
            // Сначала проверяем существование
            PrisonerEntity prisoner = prisonerDAO.getPrisonerById(prisonerId);
            if (prisoner == null) {
                System.out.println("Заключённый ID " + prisonerId + " не найден в БД");
                return false;
            }

            System.out.println("Удаляем заключённого со всеми связанными записями: " +
                    prisoner.getFullName() + " (ID: " + prisonerId + ")");

            // Используем метод с транзакцией
            boolean deleted = prisonerDAO.deletePrisonerWithRelations(prisonerId);

            if (deleted) {
                System.out.println("Заключённый ID " + prisonerId + " и все связанные данные удалены из БД");
                return true;
            } else {
                System.err.println("Ошибка удаления заключённого ID " + prisonerId + " из БД");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Ошибка в deletePrisonerWithRelations: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Добавить существующий контакт заключённому
    public String addContactToPrisoner(int prisonerId, int contactId) {
        try {
            // Проверяем существование заключённого
            PrisonerEntity prisoner = prisonerDAO.getPrisonerById(prisonerId);
            if (prisoner == null) {
                return "Ошибка: Заключённый не найден";
            }

            // Проверяем существование контакта
            ContactEntity contact = contactDAO.getContactById(contactId);
            if (contact == null) {
                return "Ошибка: Контакт не найден";
            }

            // Проверяем, нет ли уже такой связи
            if (contactDAO.contactExistsForPrisoner(prisonerId, contactId)) {
                return "Ошибка: Этот контакт уже добавлен к заключённому";
            }

            // Добавляем связь
            boolean added = contactDAO.addPrisonerContact(prisonerId, contactId);

            if (added) {
                return "Успех: Контакт добавлен к заключённому";
            } else {
                return "Ошибка: Не удалось добавить связь";
            }

        } catch (Exception e) {
            System.err.println("Ошибка в addContactToPrisoner: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    // Удалить контакт у заключённого
    public String removeContactFromPrisoner(int prisonerId, int contactId) {
        System.out.println("=== PrisonerService.removeContactFromPrisoner ===");
        System.out.println("prisonerId: " + prisonerId + ", contactId: " + contactId);

        try {
            // 1. Проверяем существование связи
            if (!contactDAO.contactExistsForPrisoner(prisonerId, contactId)) {
                return "Ошибка: У заключённого нет такого контакта";
            }

            // 2. Удаляем связь
            boolean linkRemoved = contactDAO.removePrisonerContact(prisonerId, contactId);
            System.out.println("Связь удалена: " + linkRemoved);

            if (!linkRemoved) {
                return "Ошибка: Не удалось удалить связь";
            }

            // 3. Проверяем, связан ли контакт с другими заключёнными
            boolean hasOtherLinks = contactDAO.hasOtherPrisonerLinks(contactId, prisonerId);
            System.out.println("Контакт имеет другие связи: " + hasOtherLinks);

            // 4. Если нет других связей - удаляем контакт полностью
            if (!hasOtherLinks) {
                System.out.println("Удаляем контакт полностью из contacts...");
                boolean contactDeleted = contactDAO.deleteContact(contactId);
                System.out.println("Контакт удалён из contacts: " + contactDeleted);

                if (contactDeleted) {
                    return "Успех: Контакт полностью удалён из системы";
                } else {
                    return "Успех: Связь удалена, но контакт остался в БД (ошибка удаления)";
                }
            } else {
                return "Успех: Связь удалена, контакт остался (связан с другими заключёнными)";
            }

        } catch (Exception e) {
            System.err.println("Ошибка в removeContactFromPrisoner: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }

    // Создать новый контакт и сразу добавить к заключённому
    public String createAndAddContact(int prisonerId, String fullName, String birthDateStr,
                                      String relation, String phone, String address) {
        try {
            // Проверяем заключённого
            PrisonerEntity prisoner = prisonerDAO.getPrisonerById(prisonerId);
            if (prisoner == null) {
                return "Ошибка: Заключённый не найден";
            }

            // Создаём контакт
            ContactEntity newContact = new ContactEntity();
            newContact.setFullName(fullName);

            if (birthDateStr != null && !birthDateStr.isEmpty()) {
                newContact.setBirthDate(java.time.LocalDate.parse(birthDateStr));
            }

            newContact.setRelation(relation);

            boolean contactCreated = contactDAO.addContact(newContact);
            if (!contactCreated) {
                return "Ошибка: Не удалось создать контакт";
            }

            ContactEntity savedContact = contactDAO.getContactByFullName(fullName);
            if (savedContact == null) {
                return "Ошибка: Созданный контакт не найден";
            }

            // Добавляем связь
            return addContactToPrisoner(prisonerId, savedContact.getContactId());

        } catch (Exception e) {
            System.err.println("Ошибка в createAndAddContact: " + e.getMessage());
            e.printStackTrace();
            return "Ошибка: " + e.getMessage();
        }
    }
}