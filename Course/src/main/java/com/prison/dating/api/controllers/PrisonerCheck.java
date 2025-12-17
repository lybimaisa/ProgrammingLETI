package main.java.com.prison.dating.api.controllers;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import main.java.com.prison.dating.server.database.PrisonerDAO;
import java.io.*;

@WebServlet("/api/prisoners/*/exists")
public class PrisonerCheck extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/plain;charset=UTF-8");

        try {
            // Извлекаем ID из URL: /api/prisoners/5/exists
            String pathInfo = request.getPathInfo(); // /5/exists
            String[] parts = pathInfo.split("/");
            int prisonerId = Integer.parseInt(parts[1]); // parts[0]="", parts[1]="5"

            PrisonerDAO prisonerDAO = new PrisonerDAO();
            boolean exists = prisonerDAO.getPrisonerById(prisonerId) != null;

            if (exists) {
                response.setStatus(200);
                response.getWriter().write("OK");
            } else {
                response.setStatus(404);
                response.getWriter().write("Not found");
            }

        } catch (Exception e) {
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
}