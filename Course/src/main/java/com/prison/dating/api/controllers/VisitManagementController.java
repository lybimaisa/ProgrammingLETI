package main.java.com.prison.dating.api.controllers;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import main.java.com.prison.dating.api.services.VisitManagementService;
import main.java.com.prison.dating.api.models.Visit;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/visits-management/*")
public class VisitManagementController extends HttpServlet {
    private VisitManagementService visitService;

    @Override
    public void init() {
        this.visitService = new VisitManagementService();
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
            out.println("<h1>Управление свиданиями</h1>");

            if (pathInfo.equals("/") || pathInfo.isEmpty()) {
                showAllVisits(out);
            } else if (pathInfo.equals("/all")) {
                showAllVisits(out);
            } else if (pathInfo.startsWith("/prisoner/")) {
                handlePrisonerVisits(request, out, pathInfo);
            } else {
                out.println("<p style='color:red'>Неизвестный запрос: " + pathInfo + "</p>");
            }

            out.println("</body></html>");

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

            if (pathInfo.startsWith("/complete/")) {
                completeVisit(pathInfo, out);
            } else if (pathInfo.startsWith("/cancel/")) {
                cancelVisit(pathInfo, out);
            } else if (pathInfo.startsWith("/delete/")) {
                deleteVisit(pathInfo, out);
            } else {
                out.println("<p style='color:red'>Неизвестный POST запрос</p>");
            }

            out.println("<br><a href='/prison/api/visits-management'>Назад к списку свиданий</a>");
            out.println("</body></html>");

        } catch (Exception e) {
            out.println("<p style='color:red'>Ошибка: " + e.getMessage() + "</p>");
            e.printStackTrace();
        }
    }

    private void showAllVisits(PrintWriter out) {
        List<Visit> visits = visitService.getAllVisits();

        out.println("<h2>Все свидания</h2>");

        if (visits.isEmpty()) {
            out.println("<p>Нет зарегистрированных свиданий</p>");
            return;
        }

        out.println("<table border='1' cellpadding='5' cellspacing='0'>");
        out.println("<tr style='background-color: #f2f2f2;'>");
        out.println("<th>ID</th><th>Заключенный</th><th>Контакт</th>");
        out.println("<th>Дата</th><th>Тип</th><th>Статус</th><th>Действия</th>");
        out.println("</tr>");

        for (Visit visit : visits) {
            out.println("<tr>");
            out.println("<td>" + visit.getVisitId() + "</td>");
            out.println("<td>" + visit.getPrisonerId() + "</td>");
            out.println("<td>" + visit.getContactId() + "</td>");
            out.println("<td>" + visit.getVisitDate() + "</td>");
            out.println("<td>" + visit.getVisitType() + "</td>");
            out.println("<td>" + getStatusBadge(visit.getStatus()) + "</td>");
            out.println("<td>");

            if ("подтверждено".equals(visit.getStatus())) {
                out.println("<form action='/prison/api/visits-management/complete/" + visit.getVisitId() +
                        "' method='post' style='display:inline; margin-right:5px;'>");
                out.println("<input type='submit' value='Отметить состоявшимся' style='color:green;'>");
                out.println("</form>");

                out.println("<form action='/prison/api/visits-management/cancel/" + visit.getVisitId() +
                        "' method='post' style='display:inline;'>");
                out.println("<input type='submit' value='Отменить' style='color:red;'>");
                out.println("</form>");
            }

            out.println("<form action='/prison/api/visits-management/delete/" + visit.getVisitId() +
                    "' method='post' style='display:inline; margin-left:5px;'>");
            out.println("<input type='submit' value='Удалить' style='color:gray;'>");
            out.println("</form>");

            out.println("</td>");
            out.println("</tr>");
        }

        out.println("</table>");
    }

    private void completeVisit(String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int visitId = Integer.parseInt(parts[2]);
                String result = visitService.markVisitAsCompleted(visitId);
                out.println("<h2>Результат</h2>");
                out.println("<p>" + result + "</p>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Неверный формат ID свидания</p>");
        }
    }

    private void cancelVisit(String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int visitId = Integer.parseInt(parts[2]);
                String result = visitService.cancelVisit(visitId);
                out.println("<h2>Результат</h2>");
                out.println("<p>" + result + "</p>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Неверный формат ID свидания</p>");
        }
    }

    private void deleteVisit(String pathInfo, PrintWriter out) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int visitId = Integer.parseInt(parts[2]);
                String result = visitService.deleteVisit(visitId);
                out.println("<h2>Результат</h2>");
                out.println("<p>" + result + "</p>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Неверный формат ID свидания</p>");
        }
    }

    private String getStatusBadge(String status) {
        String color;
        switch (status) {
            case "состоялось": color = "green"; break;
            case "подтверждено": color = "blue"; break;
            case "отменено": color = "red"; break;
            default: color = "black";
        }
        return "<span style='color:" + color + "; font-weight:bold;'>" + status + "</span>";
    }

    private void handlePrisonerVisits(HttpServletRequest request, PrintWriter out, String pathInfo) {
        try {
            String[] parts = pathInfo.split("/");
            if (parts.length >= 3) {
                int prisonerId = Integer.parseInt(parts[2]);
                List<Visit> visits = visitService.getVisitsByPrisoner(prisonerId);

                out.println("<h2>Свидания заключенного #" + prisonerId + "</h2>");
                out.println("<p>" + visitService.getVisitStats(prisonerId).replace("\n", "<br>") + "</p>");

                if (visits.isEmpty()) {
                    out.println("<p>У заключенного нет свиданий</p>");
                    return;
                }

                out.println("<table border='1' cellpadding='5'>");
                out.println("<tr><th>Дата</th><th>Тип</th><th>Статус</th></tr>");

                for (Visit visit : visits) {
                    out.println("<tr>");
                    out.println("<td>" + visit.getVisitDate() + "</td>");
                    out.println("<td>" + visit.getVisitType() + "</td>");
                    out.println("<td>" + getStatusBadge(visit.getStatus()) + "</td>");
                    out.println("</tr>");
                }

                out.println("</table>");
            }
        } catch (NumberFormatException e) {
            out.println("<p style='color:red'>Неверный формат ID заключенного</p>");
        }
    }
}