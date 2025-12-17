package main.java.com.prison.dating.api.controllers;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import main.java.com.prison.dating.server.database.PrisonerDAO;
import java.io.*;

@WebServlet("/api/auth/prisoner")
public class AuthServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            // Получаем параметры из формы
            String prisonerIdParam = request.getParameter("prisonerId");
            String password = request.getParameter("password");

            System.out.println("Запрос авторизации: prisonerId=" + prisonerIdParam + ", password=" + password);

            if (prisonerIdParam == null || prisonerIdParam.isEmpty() || password == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.println("{\"status\":\"error\",\"message\":\"Отсутствуют параметры prisonerId или password\"}");
                return;
            }

            int prisonerId = Integer.parseInt(prisonerIdParam);

            // Проверяем пароль через DAO
            PrisonerDAO prisonerDAO = new PrisonerDAO();
            boolean authenticated = prisonerDAO.checkPassword(prisonerId, password);

            if (authenticated) {
                response.setStatus(HttpServletResponse.SC_OK);
                out.println("{");
                out.println("  \"status\": \"success\",");
                out.println("  \"message\": \"Авторизация успешна\",");
                out.println("  \"prisonerId\": " + prisonerId);
                out.println("}");

                System.out.println("Авторизация УСПЕШНА для prisonerId=" + prisonerId);
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                out.println("{");
                out.println("  \"status\": \"error\",");
                out.println("  \"message\": \"Неверный ID заключённого или пароль\"");
                out.println("}");

                System.out.println("Авторизация ОШИБКА для prisonerId=" + prisonerId);
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.println("{\"status\":\"error\",\"message\":\"Неверный формат prisonerId\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.println("{\"status\":\"error\",\"message\":\"Ошибка сервера: " + e.getMessage().replace("\"", "'") + "\"}");
            e.printStackTrace();
        }
    }
}