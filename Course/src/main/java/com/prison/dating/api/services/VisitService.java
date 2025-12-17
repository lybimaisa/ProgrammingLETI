package main.java.com.prison.dating.api.services;

import main.java.com.prison.dating.server.database.*;
import main.java.com.prison.dating.server.entities.VisitRequestEntity;
import main.java.com.prison.dating.api.models.VisitRequest;
import main.java.com.prison.dating.api.models.VisitLimit;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class VisitService {
    private VisitRequestDAO visitRequestDAO;
    private VisitLimitDAO visitLimitDAO;
    private PrisonerDAO prisonerDAO;
    private ContactDAO contactDAO;

    public VisitService() {
        this.visitRequestDAO = new VisitRequestDAO();
        this.visitLimitDAO = new VisitLimitDAO();
        this.prisonerDAO = new PrisonerDAO();
        this.contactDAO = new ContactDAO();
    }

    // Запрос свидания
    public String requestVisit(int prisonerId, int contactId,
                               LocalDate visitDate, String visitType) {
        System.out.println("=== VisitService.requestVisit() ===");
        System.out.println("prisonerId: " + prisonerId + ", contactId: " + contactId);
        System.out.println("visitDate: " + visitDate + ", visitType: " + visitType);

        // 1. Валидация типа
        if (!isValidVisitType(visitType)) {
            return "Ошибка: Неверный тип свидания. Допустимо: 'краткосрочное' или 'длительное'";
        }

        // 2. Проверка существования
        String existenceCheck = checkExistence(prisonerId, contactId);
        if (existenceCheck != null) return existenceCheck;

        // 3. Проверка одобрения контакта
        String approvalCheck = checkContactApproval(prisonerId, contactId);
        if (approvalCheck != null) return approvalCheck;

        // 4. Проверка даты
        if (visitDate.isBefore(LocalDate.now())) {
            return "Ошибка: Нельзя запланировать свидание на прошедшую дату";
        }

        // 5. Проверка дубликатов
        if (visitRequestDAO.hasExistingRequest(prisonerId, contactId, visitDate)) {
            return "Ошибка: На эту дату уже есть запрос свидания";
        }

        // 6. Проверка лимитов
        if (!visitLimitDAO.canRequestVisit(prisonerId, visitType)) {
            return "Ошибка: Превышен лимит " + visitType + " свиданий";
        }

        // 7. Создание запроса
        VisitRequestEntity request = createVisitRequestEntity(prisonerId, contactId, visitDate, visitType);
        boolean created = visitRequestDAO.createVisitRequest(request);

        if (created) {
            // 8. Обновление счетчиков
            return "Успех: Запрос на свидание создан и ожидает одобрения";
        } else {
            return "Ошибка: Не удалось создать запрос на свидание";
        }
    }

    // Одобрить запрос
    public String approveVisitRequest(int requestId) {
        System.out.println("=== VisitService.approveVisitRequest #" + requestId + " ===");

        VisitRequestEntity request = findRequestById(requestId);
        if (request == null) {
            return "Ошибка: Запрос #" + requestId + " не найден";
        }

        if ("одобрена".equals(request.getStatus())) {
            return "Ошибка: Запрос уже одобрен";
        }

        String visitType = request.getVisitType();
        if (visitType == null || visitType.isEmpty()) {
            System.out.println("Тип свидания не указан, используем 'краткосрочное'");
            visitType = "краткосрочное";
        }

        System.out.println("Данные запроса: ID=" + requestId +
                ", Тип=" + visitType +
                ", Prisoner=" + request.getPrisonerId() +
                ", Contact=" + request.getContactId());

        boolean updated = visitRequestDAO.approveVisitRequestWithLimitUpdate(requestId, visitType);

        if (updated) {
            System.out.println("✓ Запрос #" + requestId + " одобрен. Свидание создано.");
            return "Успех: Запрос одобрен и свидание создано";
        } else {
            System.err.println("✗ Ошибка при одобрении запроса #" + requestId);
            return "Ошибка: Не удалось одобрить запрос";
        }
    }

    // Отклонить запрос
    public String rejectVisitRequest(int requestId, String reason) {
        VisitRequestEntity request = findRequestById(requestId);
        if (request == null) {
            return "Ошибка: Запрос не найден";
        }

        // Проверяем, не отклонен ли уже
        if ("отклонена".equals(request.getStatus())) {
            return "Ошибка: Запрос уже отклонен";
        }

        // Обновляем статус
        boolean updated = visitRequestDAO.updateRequestStatus(requestId, "отклонена");

        if (updated) {
            // Возвращаем лимит
            returnVisitLimit(request.getPrisonerId(), request.getVisitType());

            // Логируем причину
            System.out.println("Запрос #" + requestId + " отклонен. Причина: " +
                    (reason != null ? reason : "не указана"));
            return "Успех: Запрос отклонен" + (reason != null ? " (Причина: " + reason + ")" : "");
        } else {
            return "Ошибка: Не удалось отклонить запрос";
        }
    }

    public String cancelVisitRequest(int requestId) {
        VisitRequestDAO dao = new VisitRequestDAO();
        boolean success = dao.cancelRequest(requestId);

        if (success) {
            return "Заявка #" + requestId + " успешно отменена";
        } else {
            return "Не удалось отменить заявку #" + requestId;
        }
    }

    // Все запросы
    public List<VisitRequest> getAllVisitRequests() {
        System.out.println("=== DEBUG: VisitService.getAllVisitRequests() ===");

        List<VisitRequestEntity> entities = visitRequestDAO.getAllRequests();
        System.out.println("Получено entities из DAO: " + entities.size());

        List<VisitRequest> result = entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        System.out.println("После конвертации в DTO: " + result.size());
        result.forEach(req -> {
            System.out.println("DTO: ID=" + req.getRequestId() +
                    ", status=" + req.getStatus() +
                    ", type=" + req.getVisitType());
        });

        return result;
    }

    // Запросы заключенного
    public List<VisitRequest> getVisitRequestsByPrisoner(int prisonerId) {
        return visitRequestDAO.getRequestsByPrisonerId(prisonerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Запросы по статусу
    public List<VisitRequest> getVisitRequestsByStatus(String status) {
        List<VisitRequest> allRequests = getAllVisitRequests();

        return allRequests.stream()
                .filter(req -> status.equalsIgnoreCase(req.getStatus()))
                .collect(Collectors.toList());
    }

    // Информация о лимитах
    public String getVisitLimitsInfo(int prisonerId) {
        VisitLimit limit = getOrCreateVisitLimit(prisonerId);
        if (limit == null) {
            return "Ошибка: Не удалось получить или создать лимиты";
        }

        int shortRemaining = Math.max(0, limit.getShortAllowed() - limit.getShortUsed());
        int longRemaining = Math.max(0, limit.getLongAllowed() - limit.getLongUsed());

        return String.format(
                "Лимиты свиданий для заключенного %d:%n" +
                        "Краткосрочные: %d/%d (осталось: %d)%n" +
                        "Длительные: %d/%d (осталось: %d)",
                prisonerId,
                limit.getShortUsed(), limit.getShortAllowed(), shortRemaining,
                limit.getLongUsed(), limit.getLongAllowed(), longRemaining
        );
    }

    // Полные данные о лимитах
    public VisitLimit getVisitLimits(int prisonerId) {
        return getOrCreateVisitLimit(prisonerId);
    }

    private boolean isValidVisitType(String visitType) {
        return "краткосрочное".equalsIgnoreCase(visitType) ||
                "длительное".equalsIgnoreCase(visitType);
    }

    private String checkExistence(int prisonerId, int contactId) {
        if (prisonerDAO.getPrisonerById(prisonerId) == null) {
            return "Ошибка: Заключенный не найден";
        }
        if (contactDAO.getContactById(contactId) == null) {
            return "Ошибка: Контакт не найден";
        }
        return null;
    }

    private String checkContactApproval(int prisonerId, int contactId) {
        try {
            if (!contactDAO.isContactApprovedForPrisoner(contactId, prisonerId)) {
                return "Ошибка: Контакт не одобрен для данного заключенного";
            }
        } catch (Exception e) {
            System.out.println("Метод проверки одобрения не доступен: " + e.getMessage());
        }
        return null;
    }

    private VisitRequestEntity createVisitRequestEntity(int prisonerId, int contactId,
                                                        LocalDate visitDate, String visitType) {
        VisitRequestEntity request = new VisitRequestEntity();
        request.setPrisonerId(prisonerId);
        request.setContactId(contactId);
        request.setRequestDate(LocalDate.now());
        request.setVisitDate(visitDate);
        request.setVisitType(visitType.toLowerCase());
        request.setStatus("ожидает");
        return request;
    }

    private void updateVisitCounters(int prisonerId, String visitType) {
        VisitLimit limit = getOrCreateVisitLimit(prisonerId);
        if (limit != null) {
            if ("краткосрочное".equalsIgnoreCase(visitType)) {
                int newShortUsed = limit.getShortUsed() + 1;
                visitLimitDAO.updateShortUsed(prisonerId, newShortUsed);
            } else if ("длительное".equalsIgnoreCase(visitType)) {
                int newLongUsed = limit.getLongUsed() + 1;
                visitLimitDAO.updateLongUsed(prisonerId, newLongUsed);
            }
        }
    }

    private VisitLimit getOrCreateVisitLimit(int prisonerId) {
        VisitLimit limit = visitLimitDAO.getLimitByPrisonerId(prisonerId);

        if (limit == null) {
            limit = new VisitLimit();
            limit.setPrisonerId(prisonerId);
            limit.setShortAllowed(4);
            limit.setLongAllowed(2);
            limit.setShortUsed(0);
            limit.setLongUsed(0);

            if (visitLimitDAO.createVisitLimit(limit)) {
                System.out.println("Созданы дефолтные лимиты для prisonerId: " + prisonerId);
                // Получаем созданный лимит обратно
                return visitLimitDAO.getLimitByPrisonerId(prisonerId);
            }
        }

        return limit;
    }

    private void returnVisitLimit(int prisonerId, String visitType) {
        VisitLimit limit = visitLimitDAO.getLimitByPrisonerId(prisonerId);
        if (limit != null) {
            if ("краткосрочное".equalsIgnoreCase(visitType) && limit.getShortUsed() > 0) {
                visitLimitDAO.updateShortUsed(prisonerId, limit.getShortUsed() - 1);
            } else if ("длительное".equalsIgnoreCase(visitType) && limit.getLongUsed() > 0) {
                visitLimitDAO.updateLongUsed(prisonerId, limit.getLongUsed() - 1);
            }
        }
    }

    private VisitRequestEntity findRequestById(int requestId) {
        List<VisitRequestEntity> allRequests = visitRequestDAO.getAllRequests();
        for (VisitRequestEntity request : allRequests) {
            if (request.getRequestId() == requestId) {
                return request;
            }
        }
        return null;
    }

    private VisitRequest convertToDTO(VisitRequestEntity entity) {
        VisitRequest dto = new VisitRequest();
        dto.setRequestId(entity.getRequestId());
        dto.setPrisonerId(entity.getPrisonerId());
        dto.setContactId(entity.getContactId());
        dto.setRequestDate(entity.getRequestDate());
        dto.setVisitDate(entity.getVisitDate());
        dto.setVisitType(entity.getVisitType());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    public String getVisitLimitsJson(int prisonerId) {
        VisitLimit limit = getOrCreateVisitLimit(prisonerId);
        if (limit == null) {
            return "{\"error\":\"Лимиты не найдены\"}";
        }

        return String.format(
                "{\"prisonerId\":%d,\"shortUsed\":%d,\"shortAllowed\":%d,\"longUsed\":%d,\"longAllowed\":%d}",
                prisonerId, limit.getShortUsed(), limit.getShortAllowed(),
                limit.getLongUsed(), limit.getLongAllowed()
        );
    }

    public String getAllVisitRequestsJson() {
        List<VisitRequest> requests = getAllVisitRequests();

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < requests.size(); i++) {
            VisitRequest req = requests.get(i);
            json.append(String.format(
                    "{\"requestId\":%d,\"prisonerId\":%d,\"contactId\":%d,\"visitDate\":\"%s\",\"visitType\":\"%s\",\"status\":\"%s\"}",
                    req.getRequestId(), req.getPrisonerId(), req.getContactId(),
                    req.getVisitDate(), req.getVisitType(), req.getStatus()
            ));
            if (i < requests.size() - 1) json.append(",");
        }
        json.append("]");

        return json.toString();
    }
}