package main.java.com.prison.dating.client.ui;

import main.java.com.prison.dating.client.api.ApiClient;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginFrame() {
        setTitle("Тюремный организатор свиданий - Вход");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Панель с логотипом/заголовком
        JPanel headerPanel = new JPanel();
        JLabel titleLabel = new JLabel("ТЮРЕМНЫЙ ОРГАНИЗАТОР СВИДАНИЙ");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(titleLabel);

        // Панель с полями ввода
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));

        formPanel.add(new JLabel("Логин:"));
        usernameField = new JTextField();
        formPanel.add(usernameField);

        formPanel.add(new JLabel("Пароль:"));
        passwordField = new JPasswordField();
        formPanel.add(passwordField);

        formPanel.add(new JLabel(""));
        loginButton = new JButton("Войти");
        formPanel.add(loginButton);

        // Панель с информацией
        JPanel infoPanel = new JPanel();
        infoPanel.add(new JLabel("Выберите роль для демонстрации:"));

        // Кнопки для демо-входа
        JPanel demoPanel = new JPanel(new FlowLayout());
        JButton adminDemoButton = new JButton("Войти как Администратор");

        demoPanel.add(adminDemoButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(infoPanel, BorderLayout.NORTH);
        southPanel.add(demoPanel, BorderLayout.SOUTH);
        mainPanel.add(southPanel, BorderLayout.SOUTH);

        // Обработчики событий
        loginButton.addActionListener(e -> attemptLogin());
        adminDemoButton.addActionListener(e -> {
            usernameField.setText("admin");
            passwordField.setText("admin123");
            attemptLogin();
        });

        // Enter для входа
        passwordField.addActionListener(e -> attemptLogin());

        add(mainPanel);
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        loginButton.setEnabled(false);
        loginButton.setText("Подключение...");

        new Thread(() -> {
            try {
                // ПРОСТАЯ ПРОВЕРКА - без вызова сервера
                boolean success = checkLogin(username, password);

                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Войти");

                    if (success) {
                        if ("admin".equals(username)) {
                            openAdminPanel();
                        } else {
                            openPrisonerPanel(username);
                        }
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Неверный логин или пароль\n\n" +
                                        "Тестовые аккаунты:\n" +
                                        "• Администратор: admin / admin123\n" +
                                        "• Заключенный: prisoner1 / 123456",
                                "Ошибка входа",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Войти");
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Ошибка: " + e.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void openAdminPanel() {
        try {
            new AdminPanel();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка открытия панели: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openPrisonerPanel(String username) {
        // Показываем загрузку
        loginButton.setEnabled(false);
        loginButton.setText("Открываю панель...");

        new Thread(() -> {
            try {
                String idStr = username.replace("prisoner", "");
                final int prisonerId = Integer.parseInt(idStr);

                // Проверяем доступность сервера перед открытием
                ApiClient.ServerStatus status = ApiClient.checkConnection();
                if (!status.available) {
                    SwingUtilities.invokeLater(() -> {
                        int choice = JOptionPane.showConfirmDialog(LoginFrame.this,
                                "Сервер недоступен!\n\n" + status.getDiagnostics() +
                                        "\n\nВсё равно продолжить?",
                                "Предупреждение",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);

                        if (choice != JOptionPane.YES_OPTION) {
                            loginButton.setEnabled(true);
                            loginButton.setText("Войти");
                            return;
                        }
                    });
                }
                SwingUtilities.invokeLater(() -> {
                    try {
                        PrisonerPanel prisonerPanel = new PrisonerPanel(prisonerId);
                        dispose();

                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "Не удалось открыть панель заключённого:\n" +
                                        e.getMessage() +
                                        "\n\nОткрываю AdminPanel вместо этого...",
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                        new AdminPanel();
                        dispose();
                    } finally {
                        loginButton.setEnabled(true);
                        loginButton.setText("Войти");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(LoginFrame.this,
                            "Ошибка: " + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                    loginButton.setEnabled(true);
                    loginButton.setText("Войти");
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });
    }

    private boolean checkLogin(String username, String password) {
        if ("admin".equals(username)) {
            return "admin123".equals(password);
        }

        if (username.startsWith("prisoner")) {
            try {
                String idStr = username.replace("prisoner", "");
                int prisonerId = Integer.parseInt(idStr);

                // Используем ApiClient для проверки
                return ApiClient.authenticatePrisoner(prisonerId, password);

            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }


}