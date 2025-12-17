package main.java.com.prison.dating.api.controllers;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.*;
import java.net.URLDecoder;

@WebServlet("/api/login")
public class LoginController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Устанавливаем кодировку
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        try {
            // Получаем параметры ДВУМЯ способами
            String username = request.getParameter("username");
            String password = request.getParameter("password");

            System.out.println("[DEBUG] Получен запрос на логин:");
            System.out.println("[DEBUG] username: " + username);
            System.out.println("[DEBUG] password: " + password);

            // Если параметры пустые, пробуем прочитать из тела
            if (username == null || password == null) {
                BufferedReader reader = request.getReader();
                StringBuilder body = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
                System.out.println("[DEBUG] Тело запроса: " + body.toString());

                // Простой парсинг параметров из тела
                if (body.toString().contains("username=")) {
                    String[] pairs = body.toString().split("&");
                    for (String pair : pairs) {
                        String[] keyValue = pair.split("=");
                        if (keyValue.length == 2) {
                            if ("username".equals(keyValue[0])) {
                                username = URLDecoder.decode(keyValue[1], "UTF-8");
                            } else if ("password".equals(keyValue[0])) {
                                password = URLDecoder.decode(keyValue[1], "UTF-8");
                            }
                        }
                    }
                }
            }

            // Проверяем логин/пароль
            boolean success = checkCredentials(username, password);

            if (success) {
                out.println("{");
                out.println("  \"success\": true,");
                out.println("  \"role\": \"admin\",");
                out.println("  \"message\": \"Успешный вход\"");
                out.println("}");
                System.out.println("[DEBUG] Успешный вход для пользователя: " + username);
            } else {
                out.println("{");
                out.println("  \"success\": false,");
                out.println("  \"message\": \"Неверный логин или пароль. Получено: " +
                        (username != null ? username : "null") + "/" +
                        (password != null ? password : "null") + "\"");
                out.println("}");
                System.out.println("[DEBUG] Ошибка входа для: " + username);
            }

        } catch (Exception e) {
            out.println("{");
            out.println("  \"success\": false,");
            out.println("  \"message\": \"Ошибка сервера: " + e.getMessage() + "\"");
            out.println("}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json; charset=UTF-8");
        PrintWriter out = response.getWriter();

        // Для тестирования через браузер
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        System.out.println("[DEBUG GET] username: " + username + ", password: " + password);

        boolean success = checkCredentials(username, password);

        out.println("{");
        out.println("  \"success\": " + success + ",");
        out.println("  \"method\": \"GET\",");
        out.println("  \"username\": \"" + username + "\",");
        out.println("  \"password\": \"" + password + "\"");
        out.println("}");
    }

    private boolean checkCredentials(String username, String password) {
        // Тестовые учетные данные
        return "admin".equals(username) && "admin123".equals(password);
    }
}