package main.java.com.prison.dating.api.controllers;

import main.java.com.prison.dating.api.services.PrisonerService;
import main.java.com.prison.dating.server.database.*;
import main.java.com.prison.dating.api.models.*;
import main.java.com.prison.dating.server.entities.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@WebServlet("/api/admin/*")
public class AdminController extends HttpServlet {

    private PrisonerService prisonerService;
    private PrisonerDAO prisonerDAO;
    private ContactDAO contactDAO;
    private VisitLimitDAO limitDAO;

    @Override
    public void init() throws ServletException {
        try {
            // Инициализируем DAO
            this.prisonerDAO = new PrisonerDAO();
            this.contactDAO = new ContactDAO();
            this.limitDAO = new VisitLimitDAO();

            // Инициализируем сервис
            this.prisonerService = new PrisonerService();

        } catch (Exception e) {
            throw new ServletException("Ошибка инициализации контроллера", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        System.out.println("\n=== AdminController.doPost() ===");
        System.out.println("Request URL: " + request.getRequestURL());
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Context Path: " + request.getContextPath());
        System.out.println("Servlet Path: " + request.getServletPath());
        System.out.println("Path Info: " + pathInfo);
        System.out.println("Query String: " + request.getQueryString());

        // Выводим все параметры
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String param = params.nextElement();
            System.out.println("  " + param + " = " + request.getParameter(param));
        }
        try {
            if (pathInfo == null || "/".equals(pathInfo)) {
                // GET /api/admin - основная информация
                out.println("{\"message\": \"Admin API is running\"}");

            } else if ("/visit-requests".equals(pathInfo)) {
                // GET /api/admin/visit-requests
                out.println("{\"note\": \"Метод в разработке\"}");

            } else if ("/prisoners".equals(pathInfo)) {
                // GET /api/admin/prisoners
                List<Prisoner> prisoners = getAllPrisonersWithDetails();
                writePrisonersJSON(out, prisoners);

            } else if (pathInfo != null && pathInfo.matches("/prisoner/\\d+")) {
                // GET /api/admin/prisoner/{id}
                int prisonerId = Integer.parseInt(pathInfo.substring("/prisoner/".length()));
                Prisoner prisoner = getPrisonerDetails(prisonerId);
                if (prisoner != null) {
                    writePrisonerJSON(out, prisoner);
                } else {
                    response.setStatus(404);
                    out.println("{\"error\": \"Заключенный не найден\"}");
                }

            } else if (pathInfo != null && pathInfo.matches("/prisoner/\\d+/limits")) {
                // GET /api/admin/prisoner/{id}/limits
                int prisonerId = Integer.parseInt(
                        pathInfo.substring("/prisoner/".length(), pathInfo.indexOf("/limits"))
                );
                String limitsInfo = getVisitLimitsInfo(prisonerId);
                out.println("{\"limitsInfo\": \"" + escapeJson(limitsInfo) + "\"}");

            } else if (pathInfo != null && pathInfo.matches("/prisoner/\\d+/contacts")) {
                // GET /api/admin/prisoner/{id}/contacts
                int prisonerId = Integer.parseInt(
                        pathInfo.substring("/prisoner/".length(), pathInfo.indexOf("/contacts"))
                );
                List<Contact> contacts = getPrisonerContacts(prisonerId);
                writeContactsJSON(out, contacts);

            } else if ("/contacts".equals(pathInfo)) {
                // GET /api/admin/contacts - все контакты
                List<Contact> contacts = getAllContacts();
                writeContactsJSON(out, contacts);

            } else if (pathInfo != null && pathInfo.matches("/prisoner/\\d+/contact/\\d+")) {
                // GET /api/admin/prisoner/{prisonerId}/contact/{contactId}
                // Детальная информация о контакте

                String[] parts = pathInfo.split("/");
                int prisonerId = Integer.parseInt(parts[2]); // prisoner/{id}
                int contactId = Integer.parseInt(parts[4]); // contact/{id}

                try {
                    ContactEntity entity = contactDAO.getContactDetails(contactId, prisonerId);

                    if (entity != null) {
                        out.println("{");
                        out.println("  \"contactId\": " + entity.getContactId() + ",");
                        out.println("  \"fullName\": \"" + escapeJson(entity.getFullName()) + "\",");
                        out.println("  \"birthDate\": \"" + (entity.getBirthDate() != null ? entity.getBirthDate() : "") + "\",");
                        out.println("  \"relation\": \"" + escapeJson(entity.getRelation()) + "\",");
                        out.println("  \"approved\": " + entity.isApproved() + ",");
                        out.println("  \"approvedText\": \"" + (entity.isApproved() ? "Одобрен" : "Не одобрен") + "\",");
                        out.println("  \"prisonerId\": " + prisonerId);
                        out.println("}");
                    } else {
                        response.setStatus(404);
                        out.println("{\"error\": \"Контакт не найден для данного заключенного\"}");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    out.println("{\"error\": \"Внутренняя ошибка\"}");
                }

            } else {
                response.setStatus(404);
                out.println("{\"error\": \"Эндпоинт не найден\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(400);
            out.println("{\"error\": \"Неверный формат ID\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.println("{\"error\": \"Внутренняя ошибка сервера: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || "/".equals(pathInfo)) {
                response.setStatus(400);
                out.println("{\"error\": \"Требуется действие\"}");

            } else if (pathInfo.matches("/visit-request/\\d+/approve")) {
                // POST /api/admin/visit-request/{id}/approve
                int requestId = Integer.parseInt(
                        pathInfo.substring("/visit-request/".length(), pathInfo.indexOf("/approve"))
                );
                out.println("{\"success\": true, \"message\": \"Запрос " + requestId + " одобрен\"}");

            } else if (pathInfo.matches("/visit-request/\\d+/reject")) {
                // POST /api/admin/visit-request/{id}/reject
                int requestId = Integer.parseInt(
                        pathInfo.substring("/visit-request/".length(), pathInfo.indexOf("/reject"))
                );
                String reason = request.getParameter("reason");
                out.println("{\"success\": true, \"message\": \"Запрос " + requestId + " отклонен. Причина: " + escapeJson(reason) + "\"}");

            } else if ("/add-prisoner".equals(pathInfo)) {
                // POST /api/admin/add-prisoner - РЕАЛЬНАЯ ЛОГИКА
                try {
                    String fullName = request.getParameter("fullName");
                    String birthDateStr = request.getParameter("birthDate");
                    String prisonerNumber = request.getParameter("prisonerNumber");

                    System.out.println("=== REAL: Добавление заключенного ===");
                    System.out.println("prisonerNumber: " + prisonerNumber);
                    System.out.println("fullName: " + fullName);
                    System.out.println("birthDate: " + birthDateStr);

                    // Валидация
                    if (fullName == null || fullName.trim().isEmpty()) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.println("{\"error\": \"Не указано ФИО заключенного\"}");
                        return;
                    }

                    if (prisonerNumber == null || prisonerNumber.trim().isEmpty()) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        out.println("{\"error\": \"Не указан номер заключенного\"}");
                        return;
                    }

                    // Создаем сущность
                    PrisonerEntity prisoner = new PrisonerEntity();
                    prisoner.setPrisonerNumber(prisonerNumber.trim());
                    prisoner.setFullName(fullName.trim());

                    // Парсим дату рождения
                    if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
                        try {
                            LocalDate birthDate = LocalDate.parse(birthDateStr.trim());
                            prisoner.setBirthDate(birthDate);
                            System.out.println("Дата рождения установлена: " + birthDate);
                        } catch (Exception e) {
                            System.err.println("Неверный формат даты рождения: " + birthDateStr);
                            // Можно продолжить без даты рождения
                        }
                    }

                    System.out.println("Создан PrisonerEntity: " + prisoner);

                    // Сохраняем через DAO
                    boolean success = prisonerDAO.addPrisoner(prisoner);

                    if (success) {
                        System.out.println("✅ Заключенный успешно добавлен в БД");
                        out.println("{\"success\": true, \"message\": \"Заключенный '" + escapeJson(fullName) + "' успешно добавлен\"}");
                    } else {
                        System.err.println("❌ Ошибка при добавлении в БД");
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        out.println("{\"error\": \"Не удалось сохранить заключенного в базе данных\"}");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Ошибка в add-prisoner: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println("{\"error\": \"Внутренняя ошибка сервера: " + escapeJson(e.getMessage()) + "\"}");
                }
            } else if (pathInfo.matches("/delete-prisoner")) {
                // POST /api/admin/delete-prisoner
                String prisonerIdStr = request.getParameter("prisonerId");

                if (prisonerIdStr == null || prisonerIdStr.trim().isEmpty()) {
                    response.setStatus(400);
                    out.println("{\"error\": \"ID заключенного обязателен\"}");
                    return;
                }

                try {
                    int prisonerId = Integer.parseInt(prisonerIdStr.trim());

                    // Удаляем со всеми зависимостями
                    boolean deleted = prisonerService.deletePrisonerWithRelations(prisonerId);

                    if (deleted) {
                        out.println("{\"success\": true, \"message\": \"Заключенный успешно удален\"}");
                    } else {
                        response.setStatus(500);
                        out.println("{\"error\": \"Ошибка при удалении. Возможно, заключенный не найден или есть проблемы с БД.\"}");
                    }

                } catch (NumberFormatException e) {
                    response.setStatus(400);
                    out.println("{\"error\": \"Неверный формат ID\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/add-contact")) {

                int prisonerId = Integer.parseInt(
                        pathInfo.substring("/prisoner/".length(), pathInfo.indexOf("/add-contact"))
                );

                String contactIdStr = request.getParameter("contactId");
                if (contactIdStr == null || contactIdStr.trim().isEmpty()) {
                    response.setStatus(400);
                    out.println("{\"error\": \"ID контакта обязателен\"}");
                    return;
                }

                try {
                    int contactId = Integer.parseInt(contactIdStr.trim());
                    String result = prisonerService.addContactToPrisoner(prisonerId, contactId);

                    if (result.startsWith("Успех")) {
                        out.println("{\"success\": true, \"message\": \"" + escapeJson(result) + "\"}");
                    } else {
                        out.println("{\"success\": false, \"error\": \"" + escapeJson(result) + "\"}");
                    }

                } catch (NumberFormatException e) {
                    response.setStatus(400);
                    out.println("{\"error\": \"Неверный формат ID контакта\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/remove-contact")) {

                int prisonerId = Integer.parseInt(
                        pathInfo.substring("/prisoner/".length(), pathInfo.indexOf("/remove-contact"))
                );

                String contactIdStr = request.getParameter("contactId");
                if (contactIdStr == null || contactIdStr.trim().isEmpty()) {
                    response.setStatus(400);
                    out.println("{\"error\": \"ID контакта обязателен\"}");
                    return;
                }

                try {
                    int contactId = Integer.parseInt(contactIdStr.trim());
                    String result = prisonerService.removeContactFromPrisoner(prisonerId, contactId);

                    if (result.startsWith("Успех")) {
                        out.println("{\"success\": true, \"message\": \"" + escapeJson(result) + "\"}");
                    } else {
                        out.println("{\"success\": false, \"error\": \"" + escapeJson(result) + "\"}");
                    }

                } catch (NumberFormatException e) {
                    response.setStatus(400);
                    out.println("{\"error\": \"Неверный формат ID контакта\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/create-contact")) {

                int prisonerId = Integer.parseInt(
                        pathInfo.substring("/prisoner/".length(), pathInfo.indexOf("/create-contact"))
                );

                String fullName = request.getParameter("fullName");
                String birthDate = request.getParameter("birthDate");
                String relation = request.getParameter("relation");

                if (fullName == null || fullName.trim().isEmpty()) {
                    response.setStatus(400);
                    out.println("{\"error\": \"ФИО контакта обязательно\"}");
                    return;
                }

                String result = prisonerService.createAndAddContact(
                        prisonerId,
                        fullName.trim(),
                        birthDate,
                        relation,
                        request.getParameter("phone"),
                        request.getParameter("address")
                );

                if (result.startsWith("Успех")) {
                    out.println("{\"success\": true, \"message\": \"" + escapeJson(result) + "\"}");
                } else {
                    out.println("{\"success\": false, \"error\": \"" + escapeJson(result) + "\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/contact/\\d+/approve")) {

                System.out.println("DEBUG: Обработка approve, pathInfo: " + pathInfo);

                try {
                    String[] parts = pathInfo.split("/");
                    System.out.println("DEBUG: parts array: " + java.util.Arrays.toString(parts));

                    int prisonerId = Integer.parseInt(parts[2]);
                    int contactId = Integer.parseInt(parts[4]);

                    System.out.println("DEBUG: prisonerId=" + prisonerId + ", contactId=" + contactId);

                    String action = request.getParameter("action");
                    boolean approved = "approve".equalsIgnoreCase(action);

                    System.out.println("DEBUG: action=" + action + ", approved=" + approved);

                    String result = prisonerService.approveContact(prisonerId, contactId, approved);

                    if (result.startsWith("Успех")) {
                        out.println("{\"success\": true, \"message\": \"" + escapeJson(result) + "\"}");
                    } else {
                        out.println("{\"success\": false, \"error\": \"" + escapeJson(result) + "\"}");
                    }

                } catch (NumberFormatException e) {
                    System.err.println("ERROR parsing IDs: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(400);
                    out.println("{\"error\": \"Неверный формат ID\"}");
                } catch (Exception e) {
                    System.err.println("ERROR: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(500);
                    out.println("{\"error\": \"Внутренняя ошибка сервера\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/contact/\\d+/reject")) {

                System.out.println("WARNING: /reject endpoint deprecated, use /approve with action=reject");

                try {
                    String[] parts = pathInfo.split("/");
                    int prisonerId = Integer.parseInt(parts[2]); // индекс 2
                    int contactId = Integer.parseInt(parts[4]);  // индекс 4

                    String result = prisonerService.approveContact(prisonerId, contactId, false);

                    if (result.startsWith("Успех")) {
                        out.println("{\"success\": true, \"message\": \"" + escapeJson(result) + "\"}");
                    } else {
                        out.println("{\"success\": false, \"error\": \"" + escapeJson(result) + "\"}");
                    }

                } catch (Exception e) {
                    response.setStatus(500);
                    out.println("{\"error\": \"Ошибка: " + escapeJson(e.getMessage()) + "\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/contacts/pending")) {

                try {
                    String idPart = pathInfo.substring("/prisoner/".length());
                    int slashIndex = idPart.indexOf("/");
                    int prisonerId = Integer.parseInt(idPart.substring(0, slashIndex));

                    List<Contact> pendingContacts = getPendingContacts(prisonerId);
                    writeContactsJSON(out, pendingContacts);

                } catch (NumberFormatException e) {
                    response.setStatus(400);
                    out.println("{\"error\": \"Неверный формат ID\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    out.println("{\"error\": \"Внутренняя ошибка сервера: " + escapeJson(e.getMessage()) + "\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/contacts/pending")) {

                try {
                    String idPart = pathInfo.substring("/prisoner/".length());
                    int slashIndex = idPart.indexOf("/");
                    int prisonerId = Integer.parseInt(idPart.substring(0, slashIndex));

                    List<Contact> pendingContacts = getPendingContacts(prisonerId);
                    writeContactsJSON(out, pendingContacts);

                } catch (NumberFormatException e) {
                    response.setStatus(400);
                    out.println("{\"error\": \"Неверный формат ID\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(500);
                    out.println("{\"error\": \"Внутренняя ошибка сервера: " + escapeJson(e.getMessage()) + "\"}");
                }

            } else if (pathInfo.matches("/prisoner/\\d+/contacts/pending")) {

                int prisonerId = Integer.parseInt(
                        pathInfo.substring("/prisoner/".length(), pathInfo.indexOf("/contacts/pending"))
                );

                List<Contact> pendingContacts = getPendingContacts(prisonerId);
                writeContactsJSON(out, pendingContacts);

            } else {
                response.setStatus(404);
                out.println("{\"error\": \"Действие не найдено\"}");
            }

        } catch (NumberFormatException e) {
            response.setStatus(400);
            out.println("{\"error\": \"Неверный формат ID\"}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.println("{\"error\": \"Внутренняя ошибка сервера: " + escapeJson(e.getMessage()) + "\"}");
        }
    }

    private List<Contact> getPendingContacts(int prisonerId) {
        try {
            // Получаем все контакты заключенного
            List<ContactEntity> allContacts = contactDAO.getContactsByPrisonerId(prisonerId);

            // Фильтруем только неподтверждённые
            return allContacts.stream()
                    .filter(contact -> !contact.isApproved())
                    .map(this::convertToContactDTO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Получить всех заключённых с полными данными
    private List<Prisoner> getAllPrisonersWithDetails() {
        try {
            List<PrisonerEntity> entities = prisonerDAO.getAllPrisoners();
            if (entities == null) return new ArrayList<>();

            return entities.stream()
                    .map(this::convertToPrisonerDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Конвертация Entity в DTO
    private Prisoner convertToPrisonerDTO(PrisonerEntity entity) {
        if (entity == null) return null;

        Prisoner prisoner = new Prisoner();
        prisoner.setPrisonerId(entity.getPrisonerId());
        prisoner.setFullName(entity.getFullName());
        prisoner.setPrisonerNumber(entity.getPrisonerNumber());
        prisoner.setBirthDate(entity.getBirthDate());

        // Добавляем информацию о лимитах
        prisoner.setVisitLimitsInfo(getVisitLimitsInfo(entity.getPrisonerId()));

        return prisoner;
    }

    // Получить информацию о лимитах
    private String getVisitLimitsInfo(int prisonerId) {
        try {
            return "Лимиты: краткосрочные 2/4, длительные 1/2";

        } catch (Exception e) {
            e.printStackTrace();
            return "Ошибка при получении лимитов";
        }
    }

    // Получить детальную информацию о заключённом
    private Prisoner getPrisonerDetails(int prisonerId) {
        try {
            PrisonerEntity entity = prisonerDAO.getPrisonerById(prisonerId);
            return convertToPrisonerDTO(entity);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Получить контакты заключенного
    private List<Contact> getPrisonerContacts(int prisonerId) {
        try {
            List<ContactEntity> contactEntities = contactDAO.getContactsByPrisonerId(prisonerId);
            if (contactEntities == null) return new ArrayList<>();

            return contactEntities.stream()
                    .map(this::convertToContactDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Получить все контакты
    private List<Contact> getAllContacts() {
        try {
            List<ContactEntity> contactEntities = contactDAO.getAllContacts();
            if (contactEntities == null) return new ArrayList<>();

            return contactEntities.stream()
                    .map(this::convertToContactDTO)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // Конвертация ContactEntity в Contact
    private Contact convertToContactDTO(ContactEntity entity) {
        if (entity == null) return null;

        Contact contact = new Contact();
        contact.setContactId(entity.getContactId());
        contact.setFullName(entity.getFullName());
        contact.setBirthDate(entity.getBirthDate());

        if (entity.getRelation() != null) {
            contact.setRelation(entity.getRelation());
        }

        contact.setApproved(entity.isApproved());

        boolean entityApproved = entity.isApproved();
        System.out.println("Конвертация: Entity.approved = " + entityApproved);
        contact.setApproved(entityApproved);
        System.out.println("После setApproved: Contact.isApproved() = " + contact.isApproved());

        return contact;
    }

    // JSON методы
    private void writePrisonersJSON(PrintWriter out, List<Prisoner> prisoners) {
        out.println("[");
        for (int i = 0; i < prisoners.size(); i++) {
            Prisoner p = prisoners.get(i);
            out.println("  {");
            out.println("    \"id\": " + p.getPrisonerId() + ",");
            out.println("    \"fullName\": \"" + escapeJson(p.getFullName()) + "\",");
            out.println("    \"prisonerNumber\": \"" + escapeJson(p.getPrisonerNumber()) + "\",");
            out.println("    \"birthDate\": \"" + (p.getBirthDate() != null ? p.getBirthDate() : "") + "\",");
            out.println("    \"limitsInfo\": \"" + escapeJson(p.getVisitLimitsInfo()) + "\"");
            out.println("  }" + (i < prisoners.size() - 1 ? "," : ""));
        }
        out.println("]");
    }

    private void writePrisonerJSON(PrintWriter out, Prisoner prisoner) {
        out.println("{");
        out.println("  \"id\": " + prisoner.getPrisonerId() + ",");
        out.println("  \"fullName\": \"" + escapeJson(prisoner.getFullName()) + "\",");
        out.println("  \"prisonerNumber\": \"" + escapeJson(prisoner.getPrisonerNumber()) + "\",");
        out.println("  \"birthDate\": \"" + (prisoner.getBirthDate() != null ? prisoner.getBirthDate() : "") + "\",");
        out.println("  \"limitsInfo\": \"" + escapeJson(prisoner.getVisitLimitsInfo()) + "\"");
        out.println("}");
    }

    private void writeContactsJSON(PrintWriter out, List<Contact> contacts) {
        out.println("[");
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);

            boolean viaIsApproved = c.isApproved();
            System.out.println("isApproved() = " + viaIsApproved);

            try {
                Method getApproved = c.getClass().getMethod("getApproved");
                Object viaGetApproved = getApproved.invoke(c);
                System.out.println("getApproved() = " + viaGetApproved);
            } catch (Exception e) {
                System.out.println("Метода getApproved() нет");
            }

            out.println("  {");
            out.println("    \"contactId\": " + c.getContactId() + ",");
            out.println("    \"fullName\": \"" + escapeJson(c.getFullName()) + "\",");
            out.println("    \"relation\": \"" + escapeJson(c.getRelation()) + "\",");
            out.println("    \"birthDate\": \"" + (c.getBirthDate() != null ? c.getBirthDate() : "") + "\",");

            out.println("    \"approved\": " + c.isApproved());

            out.println("  }" + (i < contacts.size() - 1 ? "," : ""));
        }
        out.println("]");
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}