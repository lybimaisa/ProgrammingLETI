package main.java.com.prison.dating.api.controllers;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

import main.java.com.prison.dating.api.models.VisitRequest;
import main.java.com.prison.dating.api.services.VisitService;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.List;

@WebServlet("/api/visits/*")
public class VisitController extends HttpServlet {
    private VisitService visitService;

    @Override
    public void init() {
        this.visitService = new VisitService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo() != null ? request.getPathInfo() : "";

        try {
            out.println("<html><body>");
            out.println("<h1>Система управления свиданиями</h1>");

            if (pathInfo.equals("/") || pathInfo.isEmpty()) {
                showMainMenu(out);
            } else if (pathInfo.startsWith("/prisoner/")) {
                handlePrisonerRequests(request, out, pathInfo);
            } else if (pathInfo.startsWith("/request/")) {
                handleSingleRequest(request, out, pathInfo);
            } else if (pathInfo.startsWith("/limits/")) {
                handleDirectLimits(request, out, pathInfo);
            } else if (pathInfo.equals("/all")) {
                showAllVisitRequests(out);
            } else if (pathInfo.equals("/pending")) {
                showPendingRequests(out);
            } else if (pathInfo.equals("/approved")) {
                showApprovedRequests(out);
            } else {
                out.println("<p style='color:red'>Неизвестный запрос: " + pathInfo + "</p>");
                showMainMenu(out);
            }

            out.println("</body></html>");

        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    private void handleDirectLimits(HttpServletRequest request, PrintWriter out, String pathInfo) {
        try {
            String[] pathParts = pathInfo.split("/");

            if (pathParts.length >= 2) {

                if (pathParts.length >= 3) {
                    int prisonerId = Integer.parseInt(pathParts[2]);
                    System.out.println("DEBUG: Прямой запрос лимитов для prisonerId=" + prisonerId);
                    showVisitLimits(out, prisonerId);
                } else {
                    out.println("<p style='color:red'>Ошибка: Укажите ID заключенного</p>");
                    out.println("<p>Используйте формат: /api/visits/limits/2</p>");
                }
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Ошибка: Неверный формат ID заключенного</p>");
            out.println("<p>ID должен быть числом</p>");
        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo() != null ? request.getPathInfo() : "";

        try {
            out.println("<html><body>");

            if (pathInfo.equals("/create")) {
                createVisitRequest(request, out);
            } else if (pathInfo.startsWith("/approve/")) {
                approveRequest(pathInfo, out);
            } else if (pathInfo.startsWith("/reject/")) {
                rejectRequest(request, pathInfo, out);
            } else if (pathInfo.equals("/cancel")) {
                cancelRequestWithParam(request, out);
            } else {
                out.println("<p style='color:red'>Неизвестный POST запрос</p>");
            }

            out.println("<br><a href='/prison/api/visits'>На главную</a>");
            out.println("</body></html>");

        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    private void cancelRequestWithParam(HttpServletRequest request, PrintWriter out) {
        try {
            String requestIdParam = request.getParameter("requestId");

            if (requestIdParam == null || requestIdParam.isEmpty()) {
                out.println("<p style='color:red'>Ошибка: Не указан requestId</p>");
                return;
            }

            int requestId = Integer.parseInt(requestIdParam);
            String result = visitService.cancelVisitRequest(requestId);
            out.println("<h2>Результат отмены</h2>");
            out.println("<p>" + result + "</p>");

        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Ошибка: Неверный формат ID запроса</p>");
        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка: " + e.getMessage() + "</p>");
        }
    }

    private void showMainMenu(PrintWriter out) {
        out.println("<form action='/prison/api/visits/create' method='post' accept-charset='UTF-8'>");
        out.println("<h2>Меню управления</h2>");
        out.println("<ul>");
        out.println("<li><a href='/prison/api/visits/all'>Все запросы на свидания</a></li>");
        out.println("<li><a href='/prison/api/visits/pending'>Ожидающие одобрения</a></li>");
        out.println("<li><a href='/prison/api/visits/approved'>Одобренные свидания</a></li>");
        out.println("<li><a href='#form' id='toggleForm'>+ Создать новый запрос</a></li>");
        out.println("<li><a href='/prison/api/visits/limits/1'>Тест: Лимиты заключенного 1</a></li>");
        out.println("<li><a href='/prison/api/visits/limits/2'>Тест: Лимиты заключенного 2</a></li>");
        out.println("</ul>");
        out.println("<div id='requestForm' style='display:none; border:1px solid #ccc; padding:20px; margin:20px 0;'>");
        out.println("<h3>Создание запроса на свидание</h3>");
        out.println("<form action='/prison/api/visits/create' method='post'>");
        out.println("ID заключенного: <input type='number' name='prisonerId' required><br>");
        out.println("ID контакта: <input type='number' name='contactId' required><br>");
        out.println("Дата свидания: <input type='date' name='visitDate' required><br>");
        out.println("Тип: <select name='visitType'>");
        out.println("<option value='short'>Краткосрочное</option>");
        out.println("<option value='long'>Длительное</option>");
        out.println("</select><br>");
        out.println("<input type='submit' value='Создать запрос'>");
        out.println("</form>");
        out.println("</div>");
        out.println("document.getElementById('toggleForm').addEventListener('click', function(e) {");
        out.println("  e.preventDefault();");
        out.println("  var form = document.getElementById('requestForm');");
        out.println("  form.style.display = form.style.display === 'none' ? 'block' : 'none';");
        out.println("});");
        out.println("</script>");
        out.println("<input type='hidden' name='_charset_' value='UTF-8' />");
    }

    private void handlePrisonerRequests(HttpServletRequest request, PrintWriter out, String pathInfo) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int prisonerId = Integer.parseInt(parts[2]);

                if (parts.length == 3) {
                    // Показать все запросы заключенного
                    showPrisonerVisitRequests(out, prisonerId);
                } else if (parts.length == 4 && "limits".equals(parts[3])) {
                    // Показать лимиты заключенного
                    showVisitLimits(out, prisonerId);
                }
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Ошибка: Неверный формат ID заключенного</p>");
        }
    }

    private void showVisitLimits(PrintWriter out, int prisonerId) {
        out.println("<h2>Лимиты свиданий для заключенного ID: " + prisonerId + "</h2>");
        System.out.println("=== DEBUG showVisitLimits ===");
        System.out.println("Получен prisonerId: " + prisonerId);
        System.out.println("visitService: " + visitService);

        try {
            String limitsInfo = visitService.getVisitLimitsInfo(prisonerId);
            System.out.println("Результат от сервиса: " + limitsInfo);

            out.println("<pre style='background-color:#f5f5f5; padding:15px; border-radius:5px;'>");
            out.println(limitsInfo);
            out.println("</pre>");

        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка при получении лимитов: " + e.getMessage() + "</p>");
            System.err.println("Ошибка в showVisitLimits: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAllVisitRequests(PrintWriter out) {
        System.out.println("=== DEBUG: showAllVisitRequests вызван ===");

        List<VisitRequest> requests = visitService.getAllVisitRequests();
        System.out.println("Для отображения получено: " + requests.size() + " записей");

        if (requests.isEmpty()) {
            out.println("<h2>Все запросы на свидания</h2>");
            out.println("<p style='color:red'>Нет запросов на свидания в базе данных</p>");
            out.println("<p>Проверка связи с БД... Если видите эту надпись, значит код работает, но данных нет.</p>");
            out.println("<p><a href='/prison/api/visits'>Вернуться на главную</a></p>");
            return;
        }
        out.println("<h2>Все запросы на свидания</h2>");
        out.println("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
        out.println("<tr style='background-color: #f2f2f2;'>");
        out.println("<th>ID</th><th>Заключенный</th><th>Контакт</th>");
        out.println("<th>Дата запроса</th><th>Дата свидания</th>");
        out.println("<th>Тип</th><th>Статус</th><th>Действия</th>");
        out.println("</tr>");

        visitService.getAllVisitRequests().forEach(req -> {
            out.println("<tr>");
            out.println("<td>" + req.getRequestId() + "</td>");
            out.println("<td>" + req.getPrisonerId() + "</td>");
            out.println("<td>" + req.getContactId() + "</td>");
            out.println("<td>" + req.getRequestDate() + "</td>");
            out.println("<td>" + req.getVisitDate() + "</td>");
            out.println("<td>" + req.getVisitType() + "</td>");
            out.println("<td>" + getStatusBadge(req.getStatus()) + "</td>");
            out.println("<td>");

            if ("ожидает".equals(req.getStatus())) {
                out.println("<a href='/prison/api/visits/approve/" + req.getRequestId() +
                        "' style='color:green; margin-right:10px;'>Одобрить</a>");
                out.println("<a href='/prison/api/visits/reject/" + req.getRequestId() +
                        "' style='color:red;'>Отклонить</a>");
            }
            out.println("<a href='/prison/api/visits/request/" + req.getRequestId() +
                    "' style='margin-left:10px;'>Подробнее</a>");
            out.println("</td>");
            out.println("</tr>");
        });

        out.println("</table>");
    }

    private void showPendingRequests(PrintWriter out) {
        out.println("<h2>Запросы, ожидающие одобрения</h2>");

        var pendingRequests = visitService.getVisitRequestsByStatus("ожидает");
        if (pendingRequests.isEmpty()) {
            out.println("<p>Нет запросов, ожидающих одобрения</p>");
            return;
        }

        out.println("<table border='1' cellpadding='5'>");
        out.println("<tr><th>ID</th><th>Заключенный</th><th>Контакт</th>");
        out.println("<th>Дата свидания</th><th>Тип</th><th>Действия</th></tr>");

        pendingRequests.forEach(req -> {
            out.println("<tr>");
            out.println("<td>" + req.getRequestId() + "</td>");
            out.println("<td>" + req.getPrisonerId() + "</td>");
            out.println("<td>" + req.getContactId() + "</td>");
            out.println("<td>" + req.getVisitDate() + "</td>");
            out.println("<td>" + req.getVisitType() + "</td>");
            out.println("<td>");
            out.println("<form action='/prison/api/visits/approve/" + req.getRequestId() +
                    "' method='post' style='display:inline;'>");
            out.println("<input type='submit' value='Одобрить' style='color:green;'>");
            out.println("</form> ");
            out.println("<form action='/prison/api/visits/reject/" + req.getRequestId() +
                    "' method='post' style='display:inline;'>");
            out.println("<input type='text' name='reason' placeholder='Причина' size='10'>");
            out.println("<input type='submit' value='Отклонить' style='color:red;'>");
            out.println("</form>");
            out.println("</td>");
            out.println("</tr>");
        });

        out.println("</table>");
    }

    private void showApprovedRequests(PrintWriter out) {
        out.println("<h2>Одобренные свидания</h2>");

        var approvedRequests = visitService.getVisitRequestsByStatus("одобрена");
        if (approvedRequests.isEmpty()) {
            out.println("<p>Нет одобренных свиданий</p>");
            return;
        }

        out.println("<table border='1' cellpadding='5'>");
        out.println("<tr><th>ID</th><th>Заключенный</th><th>Контакт</th>");
        out.println("<th>Дата свидания</th><th>Тип</th><th>Действия</th></tr>");

        approvedRequests.forEach(req -> {
            out.println("<tr>");
            out.println("<td>" + req.getRequestId() + "</td>");
            out.println("<td>" + req.getPrisonerId() + "</td>");
            out.println("<td>" + req.getContactId() + "</td>");
            out.println("<td>" + req.getVisitDate() + "</td>");
            out.println("<td>" + req.getVisitType() + "</td>");
            out.println("<td>");
            out.println("<form action='/prison/api/visits/cancel/" + req.getRequestId() +
                    "' method='post' style='display:inline;'>");
            out.println("<input type='submit' value='Отменить'>");
            out.println("</form>");
            out.println("</td>");
            out.println("</tr>");
        });

        out.println("</table>");
    }

    private void showPrisonerVisitRequests(PrintWriter out, int prisonerId) {
        out.println("<h2>Запросы на свидания для заключенного ID: " + prisonerId + "</h2>");

        // Информация о лимитах
        out.println("<h3>Лимиты свиданий:</h3>");
        out.println("<pre>" + visitService.getVisitLimitsInfo(prisonerId) + "</pre>");

        var requests = visitService.getVisitRequestsByPrisoner(prisonerId);
        if (requests.isEmpty()) {
            out.println("<p>У этого заключенного нет запросов на свидания</p>");
            return;
        }

        out.println("<table border='1' cellpadding='5'>");
        out.println("<tr><th>ID запроса</th><th>Контакт</th><th>Дата запроса</th>");
        out.println("<th>Дата свидания</th><th>Тип</th><th>Статус</th></tr>");

        requests.forEach(req -> {
            out.println("<tr>");
            out.println("<td>" + req.getRequestId() + "</td>");
            out.println("<td>" + req.getContactId() + "</td>");
            out.println("<td>" + req.getRequestDate() + "</td>");
            out.println("<td>" + req.getVisitDate() + "</td>");
            out.println("<td>" + req.getVisitType() + "</td>");
            out.println("<td>" + getStatusBadge(req.getStatus()) + "</td>");
            out.println("</tr>");
        });

        out.println("</table>");
    }

    private void handleSingleRequest(HttpServletRequest request, PrintWriter out, String pathInfo) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int requestId = Integer.parseInt(parts[2]);
                out.println("<h2>Детали запроса #" + requestId + "</h2>");
                out.println("<p>Функция просмотра деталей в разработке</p>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Ошибка: Неверный формат ID запроса</p>");
        }
    }

    private void createVisitRequest(HttpServletRequest request, PrintWriter out) {
        try {
            int prisonerId = Integer.parseInt(request.getParameter("prisonerId"));
            int contactId = Integer.parseInt(request.getParameter("contactId"));
            LocalDate visitDate = LocalDate.parse(request.getParameter("visitDate"));
            String visitType = request.getParameter("visitType");

            String result = visitService.requestVisit(prisonerId, contactId, visitDate, visitType);

            out.println("<h2>Результат создания запроса</h2>");
            if (result.startsWith("Успех")) {
                out.println("<p style='color:green; font-weight:bold;'>" + result + "</p>");
                out.println("<p><strong>Детали:</strong></p>");
                out.println("<ul>");
                out.println("<li>ID заключенного: " + prisonerId + "</li>");
                out.println("<li>ID контакта: " + contactId + "</li>");
                out.println("<li>Дата свидания: " + visitDate + "</li>");
                out.println("<li>Тип: " + visitType + "</li>");
                out.println("</ul>");
            } else {
                out.println("<p style='color:red; font-weight:bold;'>" + result + "</p>");
            }

        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка: " + e.getMessage() + "</p>");
        }
    }

    private void approveRequest(String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int requestId = Integer.parseInt(parts[2]);
                String result = visitService.approveVisitRequest(requestId);
                out.println("<h2>Результат одобрения</h2>");
                out.println("<p>" + result + "</p>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Ошибка: Неверный формат ID запроса</p>");
        }
    }

    private void rejectRequest(HttpServletRequest request, String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int requestId = Integer.parseInt(parts[2]);
                String reason = request.getParameter("reason");
                String result = visitService.rejectVisitRequest(requestId, reason);
                out.println("<h2>Результат отклонения</h2>");
                out.println("<p>" + result + "</p>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Ошибка: Неверный формат ID запроса</p>");
        }
    }

    private String getStatusBadge(String status) {
        String color;
        switch (status) {
            case "одобрена": color = "green"; break;
            case "отклонена": color = "red"; break;
            case "отменена": color = "gray"; break;
            case "ожидает": color = "orange"; break;
            default: color = "black";
        }
        return "<span style='color:" + color + "; font-weight:bold;'>" + status + "</span>";
    }
}