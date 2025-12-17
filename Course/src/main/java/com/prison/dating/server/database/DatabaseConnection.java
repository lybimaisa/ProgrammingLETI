package main.java.com.prison.dating.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Настройки подключения
    private static final String URL = "jdbc:postgresql://localhost:5432/prison_dating_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "admin";

    String url = "jdbc:postgresql://localhost:5432/prison_dating_db" +
            "?useUnicode=true" +
            "&characterEncoding=UTF-8" +
            "&charSet=UTF-8";

    static {
        try {
            // Регистрируем драйвер при загрузке класса
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL драйвер не найден!");
            e.printStackTrace();
        }
    }

    // Создаем новое соединение каждый раз
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        return conn;
    }
}