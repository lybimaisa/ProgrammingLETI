package main.java.com.prison.dating.client.api;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private static final String BASE_URL = "http://localhost:8080/prison/api";

    public static class ServerStatus {
        public boolean available = false;
        public String url = BASE_URL;
        public String error = "";
        public String response = "";

        public String getDiagnostics() {
            return "URL: " + url + "\n" +
                    "Доступен: " + (available ? "✓" : "✗") + "\n" +
                    (error.isEmpty() ? "" : "Ошибка: " + error + "\n") +
                    (available && !response.isEmpty() ? "Ответ: " + response.substring(0, Math.min(100, response.length())) + "..." : "");
        }
    }

    public static ServerStatus checkConnection() {
        ServerStatus status = new ServerStatus();

        try {
            System.out.println("Проверяем соединение с: " + BASE_URL + "/test");

            URL url = new URL(BASE_URL + "/test");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            System.out.println("Код ответа: " + responseCode);

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                status.available = true;
                status.response = response.toString();
                System.out.println("Сервер доступен!");
            } else {
                status.error = "HTTP " + responseCode + ": " + conn.getResponseMessage();
            }

        } catch (Exception e) {
            status.error = e.getMessage();
            System.err.println("Ошибка соединения: " + e.getMessage());

            if (e.getMessage().contains("Connection refused")) {
                status.error += "\n\nTomcat не запущен! Запустите Tomcat.";
            } else if (e.getMessage().contains("404")) {
                status.error += "\n\nПриложение не развернуто по пути: " + BASE_URL;
            }
        }

        return status;
    }

    public static boolean isServerAvailable() {
        return checkConnection().available;
    }

    public static String getAllPrisoners() throws IOException {
        return sendRequest("GET", "/prisoners", null);
    }

    public static String addPrisoner(String prisonerNumber, String fullName, String birthDate)
            throws IOException {
        String params = "prisonerNumber=" + encode(prisonerNumber) +
                "&fullName=" + encode(fullName) +
                "&birthDate=" + encode(birthDate != null ? birthDate : "");
        return sendRequest("POST", "/admin/add-prisoner", params);
    }

    public static String deletePrisonerWithRelations(int prisonerId) throws IOException {
        String params = "prisonerId=" + prisonerId;
        return sendRequest("POST", "/admin/delete-prisoner", params);
    }

    public static String getPrisonerById(int prisonerId) throws IOException {
        return sendRequest("GET", "/prisoners/" + prisonerId, null);
    }

    // В ApiClient.java добавьте (если ещё нет):
    public static String addContactToPrisoner(int prisonerId, int contactId) throws IOException {
        String params = "contactId=" + contactId;
        return sendRequest("POST", "/admin/prisoner/" + prisonerId + "/add-contact", params);
    }

    public static String createContactForPrisoner(int prisonerId, String fullName,
                                                  String birthDate, String relation) throws IOException {
        String params = "fullName=" + URLEncoder.encode(fullName, StandardCharsets.UTF_8.toString()) +
                "&birthDate=" + (birthDate != null ? URLEncoder.encode(birthDate, StandardCharsets.UTF_8.toString()) : "") +
                "&relation=" + (relation != null ? URLEncoder.encode(relation, StandardCharsets.UTF_8.toString()) : "");

        System.out.println("DEBUG: Отправляем контакт с кодировкой UTF-8");
        System.out.println("  Параметры: " + params);

        return sendRequest("POST", "/admin/prisoner/" + prisonerId + "/create-contact", params);
    }

    public static String approveContact(int prisonerId, int contactId, boolean approved) throws IOException {
        String params = "action=" + (approved ? "approve" : "reject");
        return sendRequest("POST",
                "/admin/prisoner/" + prisonerId + "/contact/" + contactId + "/approve",
                params);
    }

    public static String removeContactFromPrisoner(int prisonerId, int contactId) throws IOException {
        String params = "contactId=" + contactId;
        return sendRequest("POST", "/admin/prisoner/" + prisonerId + "/remove-contact", params);
    }

    public static String getPrisonerContacts(int prisonerId) throws IOException {
        return sendRequest("GET", "/admin/prisoner/" + prisonerId + "/contacts", null);
    }
    // ================ СВИДАНИЯ ================

    public static String getAllVisitRequests() throws IOException {
        return sendRequest("GET", "/visits/all", null);
    }

    public static String getPrisonerVisitRequests(int prisonerId) throws IOException {
        return sendRequest("GET", "/visits/prisoner/" + prisonerId, null);
    }

    public static String getVisitLimits(int prisonerId) throws IOException {
        return sendRequest("GET", "/visits/limits/" + prisonerId, null);
    }

    public static String createVisitRequest(int prisonerId, int contactId, String visitDate, String visitType) throws IOException {
        String params = "prisonerId=" + prisonerId +
                "&contactId=" + contactId +
                "&visitDate=" + encode(visitDate) +
                "&visitType=" + encode(visitType);
        return sendRequest("POST", "/visits/create", params);
    }

    public static String approveVisitRequest(int requestId) throws IOException {
        String endpoint = "/visits/approve/" + requestId;
        return sendRequest("POST", endpoint, "");

    }

    public static String rejectVisitRequest(int requestId, String reason) throws IOException {
        String params = "reason=" + encode(reason);
        return sendRequest("POST", "/visits/reject/" + requestId, params);
    }

    public static String cancelVisitRequest(int requestId) throws IOException {
        return sendRequest("POST", "/visits/cancel/" + requestId, "");
    }

    // Получить все свидания
    public static String getAllVisits() throws IOException {
        return sendRequest("GET", "/visits-management/all", null);
    }

    // Получить свидания заключенного
    public static String getPrisonerVisits(int prisonerId) throws IOException {
        return sendRequest("GET", "/visits-management/prisoner/" + prisonerId, null);
    }

    // Отметить свидание как состоявшееся
    public static String markVisitAsCompleted(int visitId) throws IOException {
        return sendRequest("POST", "/visits-management/complete/" + visitId, "");
    }

    // Отменить свидание
    public static String cancelVisit(int visitId) throws IOException {
        return sendRequest("POST", "/visits-management/cancel/" + visitId, "");
    }

    // ================ ОСНОВНЫЕ HTTP МЕТОДЫ ================

    private static String sendRequest(String method, String endpoint, String params) throws IOException {
        String urlString = BASE_URL + endpoint;
        System.out.println("FULL URL: " + urlString);
        System.out.println(method + " запрос: " + urlString + (params != null ? "?" + params : ""));

        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);

        if (params != null && method.equals("POST")) {
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(params.getBytes("UTF-8"));
            }
        }

        int responseCode = conn.getResponseCode();
        System.out.println("Код ответа: " + responseCode);

        InputStream inputStream = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();
    }

    public static boolean authenticatePrisoner(int prisonerId, String password) {
        try {
            // 1. Правильный endpoint (нашли диагностикой)
            String params = "prisonerId=" + prisonerId + "&password=" + encode(password);
            String response = sendRequest("POST", "/auth/prisoner", params);

            System.out.println("=== ДЕТАЛЬНЫЙ ДЕБАГ АВТОРИЗАЦИИ ===");
            System.out.println("Полный ответ: " + response);

            // 2. ПРАВИЛЬНАЯ проверка ответа
            boolean success = false;

            // Вариант 1: Проверяем разные форматы
            if (response.contains("\"status\": \"success\"")) { // ← С ПРОБЕЛОМ!
                success = true;
                System.out.println("Найден статус: \"status\": \"success\"");
            }
            else if (response.contains("\"success\": true")) {
                success = true;
                System.out.println("Найден статус: \"success\": true");
            }
            else if (response.contains("\"status\":\"success\"")) { // без пробела
                success = true;
                System.out.println("Найден статус: \"status\":\"success\"");
            }
            else if (response.toLowerCase().contains("success")) {
                // Общая проверка
                success = true;
                System.out.println("Найдено слово: success");
            }

            // 3. Дополнительная диагностика
            System.out.println("JSON анализ:");
            System.out.println("- Длина ответа: " + response.length() + " символов");
            System.out.println("- Содержит 'success': " + response.contains("success"));
            System.out.println("- Содержит '\"status\":': " + response.contains("\"status\":"));
            System.out.println("- Содержит 'prisonerId': " + response.contains("prisonerId"));

            // 4. Выводим результат
            System.out.println("Итог авторизации: " + (success ? "УСПЕХ" : "ОШИБКА"));

            return success;

        } catch (Exception e) {
            System.err.println("Ошибка авторизации: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    public static String findCorrectEndpoint() {
        System.out.println("=== ПОИСК ПРАВИЛЬНОГО ENDPOINT ===");

        // Все возможные комбинации BASE_URL и endpoint
        String[][] combinations = {
                // {BASE_URL, endpoint, описание}
                {"http://localhost:8080", "/prison/auth/prisoner", "Context: /prison, Servlet: /auth/prisoner"},
                {"http://localhost:8080", "/auth/prisoner", "Context: /, Servlet: /auth/prisoner"},
                {"http://localhost:8080/prison", "/auth/prisoner", "Context: /prison в BASE, Servlet: /auth/prisoner"},
                {"http://localhost:8080/prison/api", "/auth/prisoner", "Context: /prison/api в BASE, Servlet: /auth/prisoner"},

                // Проверяем также GET запрос (иногда сервлеты отвечают на GET)
                {"http://localhost:8080", "/prison/auth/prisoner", "GET тест"},
                {"http://localhost:8080", "/auth/prisoner", "GET тест"},
        };

        for (String[] combo : combinations) {
            String baseUrl = combo[0];
            String endpoint = combo[1];
            String description = combo[2];

            String fullUrl = baseUrl + endpoint + "?prisonerId=1&password=123456";

            System.out.println("\n--- Тест: " + description + " ---");
            System.out.println("URL: " + fullUrl);

            try {
                // Пробуем POST
                HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                int responseCode = conn.getResponseCode();
                System.out.println("POST → Код: " + responseCode);

                if (responseCode == 200) {
                    // Читаем ответ
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = in.readLine();
                    in.close();

                    System.out.println("Ответ: " + response);
                    System.out.println("НАЙДЕН РАБОЧИЙ URL: " + baseUrl + endpoint);

                    // Обновляем BASE_URL
                    System.out.println("Установите BASE_URL = \"" + baseUrl + "\"");
                    System.out.println("И endpoint = \"" + endpoint + "\"");

                    return baseUrl + "|" + endpoint;
                }

                // Если POST не работает, пробуем GET
                conn = (HttpURLConnection) new URL(fullUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);

                responseCode = conn.getResponseCode();
                System.out.println("GET → Код: " + responseCode);

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = in.readLine();
                    in.close();

                    System.out.println("Ответ: " + response);
                    System.out.println("НАЙДЕН РАБОЧИЙ URL (GET): " + baseUrl + endpoint);
                    return baseUrl + "|" + endpoint;
                }

            } catch (Exception e) {
                System.out.println("Ошибка: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }

        System.out.println("\nНи одна комбинация не сработала!");
        return null;
    }
}