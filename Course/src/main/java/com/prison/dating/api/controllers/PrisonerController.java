package main.java.com.prison.dating.api.controllers;

import main.java.com.prison.dating.api.services.PrisonerService;
import main.java.com.prison.dating.api.models.Prisoner;
import main.java.com.prison.dating.api.models.Contact;
import main.java.com.prison.dating.api.models.VisitLimit;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/api/prisoners/*")
public class PrisonerController extends HttpServlet {

    private PrisonerService prisonerService = new PrisonerService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        System.out.println("PrisonerController.doGet() вызван, pathInfo: " + pathInfo); // Отладка

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/prisoners - все заключённые
                List<Prisoner> prisoners = prisonerService.getAllPrisoners();
                writePrisonersJSON(out, prisoners);

            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/prisoners/{id} - конкретный заключённый
                int prisonerId = Integer.parseInt(pathInfo.substring(1));
                Prisoner prisoner = prisonerService.getPrisonerById(prisonerId);

                if (prisoner != null) {
                    ///writePrisonerJSON(out, prisoner);
                } else {
                    response.setStatus(404);
                    out.println("{\"error\": \"Заключённый не найден\"}");
                }

            } else if (pathInfo.matches("/\\d+/contacts")) {
                // GET /api/prisoners/{id}/contacts - контакты заключённого
                System.out.println("Обработка /contacts endpoint");

                try {
                    // Извлекаем prisonerId из пути: /1/contacts → prisonerId = 1
                    String idStr = pathInfo.substring(1); // "1/contacts"
                    int slashIndex = idStr.indexOf("/");
                    int prisonerId = Integer.parseInt(idStr.substring(0, slashIndex));

                    System.out.println("Запрошены контакты для prisonerId: " + prisonerId);

                    List<Contact> contacts = prisonerService.getPrisonerContacts(prisonerId);
                    System.out.println("Найдено контактов: " + contacts.size());

                    ///writeContactsJSON(out, contacts);

                } catch (Exception e) {
                    System.err.println("Ошибка в /contacts: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(500);
                    out.println("{\"error\": \"Ошибка получения контактов: " + e.getMessage() + "\"}");
                }

            } else if (pathInfo.matches("/\\d+/limits")) {
                // GET /api/prisoners/{id}/limits - лимиты свиданий
                System.out.println("Обработка /limits endpoint");

                try {
                    String idStr = pathInfo.substring(1); // "1/limits"
                    int slashIndex = idStr.indexOf("/");
                    int prisonerId = Integer.parseInt(idStr.substring(0, slashIndex));

                    System.out.println("Запрошены лимиты для prisonerId: " + prisonerId);

                    // Нужно добавить метод getVisitLimits в PrisonerService
                    VisitLimit limit = prisonerService.getVisitLimits(prisonerId);

                    if (limit != null) {
                        writeLimitsJSON(out, limit);
                    } else {
                        out.println("{\"error\": \"Лимиты не найдены\"}");
                    }

                } catch (Exception e) {
                    System.err.println("Ошибка в /limits: " + e.getMessage());
                    e.printStackTrace();
                    response.setStatus(500);
                    out.println("{\"error\": \"Ошибка получения лимитов: " + e.getMessage() + "\"}");
                }

            } else {
                // Неизвестный endpoint
                response.setStatus(404);
                out.println("{\"error\": \"Endpoint не найден\"}");
            }

        } catch (Exception e) {
            System.err.println("Общая ошибка в PrisonerController: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(500);
            out.println("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private void writePrisonersJSON(PrintWriter out, List<Prisoner> prisoners) {
        out.println("[");
        for (int i = 0; i < prisoners.size(); i++) {
            Prisoner p = prisoners.get(i);
            out.println("  {");
            out.println("    \"id\": " + p.getPrisonerId() + ",");
            out.println("    \"fullName\": \"" + escapeJson(p.getFullName()) + "\",");
            out.println("    \"prisonerNumber\": \"" + escapeJson(p.getPrisonerNumber()) + "\",");

            // Только birthDate - остальные поля убраны
            LocalDate birthDate = p.getBirthDate();
            out.println("    \"birthDate\": " +
                    (birthDate != null ? "\"" + birthDate.toString() + "\"" : "null"));

            out.println("  }" + (i < prisoners.size() - 1 ? "," : ""));
        }
        out.println("]");
    }

    private void writeLimitsJSON(PrintWriter out, VisitLimit limit) {
        if (limit == null) {
            out.println("{\"error\": \"Лимиты не найдены\"}");
            return;
        }

        out.println("{");
        out.println("  \"shortAllowed\": " + limit.getShortAllowed() + ",");
        out.println("  \"shortUsed\": " + limit.getShortUsed() + ",");
        out.println("  \"longAllowed\": " + limit.getLongAllowed() + ",");
        out.println("  \"longUsed\": " + limit.getLongUsed());
        out.println("}");
    }

    // Метод для экранирования спецсимволов в JSON (добавьте если нет)
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}