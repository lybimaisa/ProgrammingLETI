package main.java.com.prison.dating.api.controllers;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/test/*")
public class TestController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        if (pathInfo == null || pathInfo.equals("/")) {
            // Все заключенные
            out.println("[");
            out.println("  {\"id\":1,\"prisonerNumber\":\"A-001\",\"fullName\":\"Иванов Иван Иванович\",\"birthDate\":\"1990-05-15\"},");
            out.println("  {\"id\":2,\"prisonerNumber\":\"A-002\",\"fullName\":\"Петров Петр Петрович\",\"birthDate\":\"1985-10-20\"},");
            out.println("  {\"id\":3,\"prisonerNumber\":\"A-003\",\"fullName\":\"Сидоров Сидор Сидорович\",\"birthDate\":\"1995-03-10\"}");
            out.println("]");
        } else {
            out.println("{\"error\": \"Неизвестный запрос\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        String pathInfo = request.getPathInfo();

        if (pathInfo != null && pathInfo.contains("add")) {
            // Добавление заключенного
            String prisonerNumber = request.getParameter("prisonerNumber");
            String fullName = request.getParameter("fullName");
            String birthDate = request.getParameter("birthDate");

            out.println("{");
            out.println("  \"success\": true,");
            out.println("  \"message\": \"Заключенный добавлен: " + fullName + "\",");
            out.println("  \"prisonerNumber\": \"" + prisonerNumber + "\",");
            out.println("  \"timestamp\": \"" + java.time.LocalDateTime.now() + "\"");
            out.println("}");

        } else if (pathInfo != null && pathInfo.contains("delete")) {
            // Удаление заключенного
            String prisonerId = request.getParameter("prisonerId");

            out.println("{");
            out.println("  \"success\": true,");
            out.println("  \"message\": \"Заключенный ID " + prisonerId + " удален\"");
            out.println("}");

        } else {
            out.println("{\"success\": false, \"error\": \"Неизвестное действие\"}");
        }
    }
}