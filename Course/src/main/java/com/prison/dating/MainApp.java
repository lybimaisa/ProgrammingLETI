package main.java.com.prison.dating;

import main.java.com.prison.dating.client.api.ApiClient;
import main.java.com.prison.dating.client.ui.LoginFrame;

public class MainApp {
    public static void main(String[] args) {
        ApiClient.findCorrectEndpoint();
        System.out.println("Запуск Тюремного организатора свиданий...");

        // Запускаем графический интерфейс
        javax.swing.SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });
    }
}