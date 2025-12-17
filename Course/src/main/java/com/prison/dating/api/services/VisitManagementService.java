package main.java.com.prison.dating.api.services;

import main.java.com.prison.dating.server.database.VisitDAO;
import main.java.com.prison.dating.server.entities.VisitEntity;
import main.java.com.prison.dating.api.models.Visit;
import java.util.List;
import java.util.stream.Collectors;

public class VisitManagementService {
    private VisitDAO visitDAO;

    public VisitManagementService() {
        this.visitDAO = new VisitDAO();
    }

    // Получить все свидания
    public List<Visit> getAllVisits() {
        List<VisitEntity> entities = visitDAO.getAllVisits();
        System.out.println("Сервис: получено " + entities.size() + " свиданий из DAO");
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Получить свидания заключенного
    public List<Visit> getVisitsByPrisoner(int prisonerId) {
        List<VisitEntity> entities = visitDAO.getVisitsByPrisonerId(prisonerId);
        return entities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Отметить свидание как состоявшееся
    public String markVisitAsCompleted(int visitId) {
        boolean success = visitDAO.markVisitAsCompleted(visitId);
        return success ? "Успех: Свидание отмечено как состоявшееся"
                : "Ошибка: Не удалось обновить статус";
    }

    // Отменить свидание
    public String cancelVisit(int visitId) {
        boolean success = visitDAO.updateVisitStatus(visitId, "отменено");
        return success ? "Успех: Свидание отменено"
                : "Ошибка: Не удалось отменить свидание";
    }

    // Удалить свидание
    public String deleteVisit(int visitId) {
        boolean success = visitDAO.deleteVisit(visitId);
        return success ? "Успех: Свидание удалено"
                : "Ошибка: Не удалось удалить свидание";
    }

    // Получить статистику свиданий
    public String getVisitStats(int prisonerId) {
        int confirmed = visitDAO.getVisitsCountByStatus(prisonerId, "подтверждено");
        int completed = visitDAO.getVisitsCountByStatus(prisonerId, "состоялось");
        int cancelled = visitDAO.getVisitsCountByStatus(prisonerId, "отменено");

        return String.format(
                "Статистика свиданий для заключенного #%d:%n" +
                        "Подтверждено: %d%n" +
                        "Состоялось: %d%n" +
                        "Отменено: %d%n" +
                        "Всего: %d",
                prisonerId, confirmed, completed, cancelled, confirmed + completed + cancelled
        );
    }

    // Конвертация Entity → DTO
    private Visit convertToDTO(VisitEntity entity) {
        Visit dto = new Visit();
        dto.setVisitId(entity.getVisitId());
        dto.setPrisonerId(entity.getPrisonerId());
        dto.setContactId(entity.getContactId());
        dto.setVisitDate(entity.getVisitDate());
        dto.setVisitType(entity.getVisitType());
        dto.setStatus(entity.getStatus());
        return dto;
    }
}