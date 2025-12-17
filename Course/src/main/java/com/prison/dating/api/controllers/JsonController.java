package main.java.com.prison.dating.api.controllers;

import main.java.com.prison.dating.api.services.VisitService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/json-simple/*")
public class JsonController extends HttpServlet {
    private VisitService visitService;

    @Override
    public void init() {
        this.visitService = new VisitService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String pathInfo = request.getPathInfo();
        PrintWriter out = response.getWriter();

        if (pathInfo == null) {
            out.println("{\"error\":\"Укажите endpoint\"}");
            return;
        }

        try {
            if (pathInfo.equals("/visits")) {
                out.println(visitService.getAllVisitRequestsJson());

            } else if (pathInfo.startsWith("/limits/")) {
                String[] parts = pathInfo.split("/");
                if (parts.length >= 3) {
                    int prisonerId = Integer.parseInt(parts[2]);
                    out.println(visitService.getVisitLimitsJson(prisonerId));
                }
            }

        } catch (Exception e) {
            out.println("{\"error\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }
}