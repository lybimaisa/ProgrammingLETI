package main.java.com.prison.dating.client.ui;

import main.java.com.prison.dating.api.models.Contact;
import main.java.com.prison.dating.api.models.Visit;
import main.java.com.prison.dating.api.models.VisitRequest;
import main.java.com.prison.dating.client.api.ApiClient;
import main.java.com.prison.dating.client.api.ApiClient.ServerStatus;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdminPanel extends JFrame {
    private JTable prisonersTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JLabel statusLabel;

    public AdminPanel() {
        setTitle("Панель администратора - Тюремный организатор свиданий");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        initComponents();
        checkServerAndLoadData();
        setVisible(true);
    }

    private void initComponents() {
        // Меню
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(e -> {
            new LoginFrame();
            dispose();
        });
        fileMenu.add(exitItem);

        JMenu prisonersMenu = new JMenu("Заключенные");
        JMenuItem addPrisonerItem = new JMenuItem("Добавить заключенного");
        addPrisonerItem.addActionListener(e -> showAddPrisonerDialog());
        prisonersMenu.add(addPrisonerItem);

        JMenu contactsMenu = new JMenu("Контакты");
        JMenuItem addContactItem = new JMenuItem("Добавить контакт");
        addContactItem.addActionListener(e -> addContactToSelectedPrisoner());
        JMenuItem removeContactItem = new JMenuItem("Удалить контакт");
        removeContactItem.addActionListener(e -> removeContactFromPrisoner());
        JMenuItem manageApprovalItem = new JMenuItem("Управление одобрением");
        manageApprovalItem.addActionListener(e -> manageContactApproval());

        contactsMenu.add(addContactItem);
        contactsMenu.add(removeContactItem);
        contactsMenu.addSeparator();
        contactsMenu.add(manageApprovalItem);

        JMenu visitsMenu = new JMenu("Свидания");
        JMenuItem manageVisitsItem = new JMenuItem("Управление свиданиями");
        manageVisitsItem.addActionListener(e -> openVisitManagement());
        visitsMenu.add(manageVisitsItem);

        menuBar.add(fileMenu);
        menuBar.add(prisonersMenu);
        menuBar.add(contactsMenu);
        menuBar.add(visitsMenu);
        setJMenuBar(menuBar);

        // Основная панель
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Заголовок с состоянием
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Управление заключенными");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));

        statusLabel = new JLabel("Проверка сервера...", SwingConstants.RIGHT);
        statusLabel.setForeground(Color.GRAY);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Таблица заключенных со ВСЕМИ колонками
        String[] columns = {"ID", "Номер", "ФИО", "Дата рождения", "Контакты", "Лимиты свиданий"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Integer.class : String.class;
            }
        };

        prisonersTable = new JTable(tableModel);
        prisonersTable.setRowHeight(30);
        prisonersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(prisonersTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        refreshButton = new JButton("Обновить");
        refreshButton.addActionListener(e -> checkServerAndLoadData());

        JButton addButton = new JButton("Добавить заключенного");
        addButton.addActionListener(e -> showAddPrisonerDialog());

        JButton deleteButton = new JButton("Удалить Заключенного");
        deleteButton.addActionListener(e -> deleteSelectedPrisoner());

        JButton detailsButton = new JButton("Подробнее о заключенном");
        detailsButton.addActionListener(e -> showPrisonerDetails());

        JButton visitsButton = new JButton("Управление свиданиями");
        visitsButton.addActionListener(e -> openVisitManagement());

        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);
        buttonPanel.add(detailsButton);
        buttonPanel.add(visitsButton);
        buttonPanel.add(deleteButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void checkServerAndLoadData() {
        statusLabel.setText("Проверка соединения...");
        refreshButton.setEnabled(false);
        tableModel.setRowCount(0);
        statusLabel.setText("Обновление...");

        new Thread(() -> {
            ServerStatus status = ApiClient.checkConnection();

            SwingUtilities.invokeLater(() -> {
                if (status.available) {
                    statusLabel.setText("Сервер доступен - загрузка...");
                    loadPrisonersData();
                } else {
                    statusLabel.setText("Сервер недоступен!");
                    refreshButton.setEnabled(true);
                    showConnectionError(status);
                }
            });
        }).start();
    }

    private void loadPrisonersData() {
        tableModel.setRowCount(0);

        new Thread(() -> {
            try {
                String jsonResponse = ApiClient.getAllPrisoners();

                SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);

                    try {
                        List<PrisonerData> prisoners = parsePrisonersJson(jsonResponse);

                        // Добавляем в таблицу информацию
                        for (PrisonerData prisoner : prisoners) {
                            tableModel.addRow(new Object[]{
                                    prisoner.id,
                                    prisoner.number,
                                    prisoner.name,
                                    formatDate(prisoner.birthDate),
                                    "Загрузка...", // Контакты
                                    "Загрузка..."  // Лимиты
                            });
                        }

                        statusLabel.setText("Загружено: " + prisoners.size() + " заключенных");

                        loadAdditionalInfoForAllPrisoners();

                    } catch (Exception e) {
                        statusLabel.setText("Ошибка обработки");
                        JOptionPane.showMessageDialog(this,
                                "Ошибка парсинга данных: " + e.getMessage(),
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    refreshButton.setEnabled(true);
                    statusLabel.setText("Ошибка загрузки");
                    JOptionPane.showMessageDialog(this,
                            "Не удалось загрузить данные: " + e.getMessage(),
                            "Ошибка сети",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void manageContactApproval() {
        int prisonerRow = prisonersTable.getSelectedRow();
        if (prisonerRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заключенного из таблицы",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int prisonerId = (int) tableModel.getValueAt(prisonerRow, 0); // ДОБАВЬТЕ final
        String prisonerName = (String) tableModel.getValueAt(prisonerRow, 2);

        // Получаем контакты этого заключенного
        List<Contact> contacts = getContactsForSelectedPrisoner();
        if (contacts == null || contacts.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "У выбранного заключенного нет контактов",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Создаем диалог выбора контакта
        String[] contactOptions = contacts.stream()
                .map(c -> {
                    String status = c.isApproved() ? "✓ Одобрен" : "✗ Не одобрен";
                    return c.getContactId() + " - " + c.getFullName() +
                            " (" + c.getRelation() + ") - " + status;
                })
                .toArray(String[]::new);

        String selectedContact = (String) JOptionPane.showInputDialog(this,
                "Выберите контакт для управления одобрением:\nЗаключенный: " + prisonerName,
                "Управление одобрением контакта",
                JOptionPane.QUESTION_MESSAGE,
                null,
                contactOptions,
                contactOptions[0]);

        if (selectedContact == null) return;

        // Извлекаем ID контакта
        final int contactId = Integer.parseInt(selectedContact.split(" - ")[0]); // ДОБАВЬТЕ final

        // Определяем текущий статус
        Contact selected = contacts.stream()
                .filter(c -> c.getContactId() == contactId)
                .findFirst()
                .orElse(null);

        if (selected == null) return;

        // Диалог выбора действия
        Object[] options = selected.isApproved() ?
                new Object[]{"Отклонить", "Отмена"} :
                new Object[]{"Одобрить", "Отмена"};

        int action = JOptionPane.showOptionDialog(this,
                "Управление контактом:\n" +
                        selected.getFullName() + " (" + selected.getRelation() + ")\n" +
                        "Текущий статус: " + (selected.isApproved() ? "Одобрен" : "Не одобрен"),
                "Управление одобрением",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        if (action == 1 || action == -1) return; // Отмена или закрытие

        final boolean approve = !selected.isApproved();

        new Thread(() -> {
            try {
                System.out.println("Отправляем запрос: prisonerId=" + prisonerId +
                        ", contactId=" + contactId + ", approve=" + approve);
                String result = ApiClient.approveContact(prisonerId, contactId, approve);

                SwingUtilities.invokeLater(() -> {
                    System.out.println("Ответ сервера: " + result);
                    if (result.contains("\"success\":true") || result.contains("\"success\": true")) {
                        JOptionPane.showMessageDialog(this,
                                "Статус контакта обновлён",
                                "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        checkServerAndLoadData(); // Обновить всю таблицу
                    } else {
                        String errorMsg = extractErrorMessage(result);
                        JOptionPane.showMessageDialog(this,
                                "Ошибка: " + errorMsg,
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Ошибка: " + e.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void loadAdditionalInfoForAllPrisoners() {
        new Thread(() -> {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                try {
                    int prisonerId = (int) tableModel.getValueAt(row, 0);

                    // Загружаем контакты
                    String contactsJson = ApiClient.getPrisonerContacts(prisonerId);
                    String contactsInfo = parseContactsCount(contactsJson);

                    // Загружаем лимиты
                    String limitsJson = ApiClient.getVisitLimits(prisonerId);
                    String limitsInfo = parseLimitsInfo(limitsJson);

                    final int currentRow = row;
                    SwingUtilities.invokeLater(() -> {
                        tableModel.setValueAt(contactsInfo, currentRow, 4); // Контакты
                        tableModel.setValueAt(limitsInfo, currentRow, 5);   // Лимиты
                    });

                    // Пауза между запросами
                    Thread.sleep(100);

                } catch (Exception e) {
                    System.err.println("Ошибка загрузки доп. информации: " + e.getMessage());
                }
            }
        }).start();
    }

    private List<PrisonerData> parsePrisonersJson(String json) {
        List<PrisonerData> prisoners = new ArrayList<>();

        try {
            if (json == null || json.trim().isEmpty()) {
                throw new Exception("Пустой ответ от сервера");
            }

            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                throw new Exception("Некорректный JSON формат");
            }

            String content = json.substring(1, json.length() - 1).trim();
            if (content.isEmpty()) {
                return prisoners;
            }

            String[] objects = content.split("\\},\\s*\\{");

            for (String obj : objects) {
                obj = obj.replace("{", "").replace("}", "").trim();

                PrisonerData prisoner = new PrisonerData();

                String[] fields = obj.split(",");
                for (String field : fields) {
                    field = field.trim();
                    String[] parts = field.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim().replace("\"", "");
                        String value = parts[1].trim().replace("\"", "");

                        switch (key) {
                            case "id":
                            case "prisonerId":
                                prisoner.id = Integer.parseInt(value);
                                break;
                            case "prisonerNumber":
                            case "number":
                                prisoner.number = value;
                                break;
                            case "fullName":
                            case "name":
                                prisoner.name = value;
                                break;
                            case "birthDate":
                                prisoner.birthDate = value.equals("null") ? "" : value;
                                break;
                        }
                    }
                }

                if (prisoner.id > 0 && prisoner.name != null) {
                    prisoners.add(prisoner);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Ошибка парсинга заключенных: " + e.getMessage());
        }

        return prisoners;
    }

    private String parseContactsCount(String json) {
        try {
            if (json == null || json.trim().isEmpty() || json.equals("[]") || json.contains("\"error\"")) {
                return "Нет контактов";
            }

            // Считаем объекты в массиве
            int count = 0;
            int index = 0;
            while ((index = json.indexOf("{", index)) != -1) {
                count++;
                index++;
            }

            return count + " контакт" + (count % 10 == 1 && count % 100 != 11 ? "" :
                    (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20) ? "а" : "ов"));

        } catch (Exception e) {
            return "Ошибка";
        }
    }

    private String parseLimitsInfo(String html) {
        try {
            if (html == null || html.trim().isEmpty()) {
                return "0/4 кр., 0/2 дл.";
            }

            System.out.println("Парсим HTML лимитов: " + html.substring(0, Math.min(100, html.length())) + "...");

            String htmlLower = html.toLowerCase();

            Pattern pattern = Pattern.compile("краткосрочные:\\s*(\\d+)/(\\d+).*?длительные:\\s*(\\d+)/(\\d+)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(htmlLower);

            if (matcher.find()) {
                int shortUsed = Integer.parseInt(matcher.group(1));
                int shortAllowed = Integer.parseInt(matcher.group(2));
                int longUsed = Integer.parseInt(matcher.group(3));
                int longAllowed = Integer.parseInt(matcher.group(4));

                // Гарантируем корректные значения
                shortUsed = Math.min(shortUsed, shortAllowed);
                longUsed = Math.min(longUsed, longAllowed);

                return String.format("%d/%d кр., %d/%d дл.",
                        shortUsed, shortAllowed, longUsed, longAllowed);
            }

            // Альтернативный паттерн
            Pattern pattern2 = Pattern.compile("(\\d+)/(\\d+).*?кр[^\\d]*(\\d+)/(\\d+).*?дл");
            Matcher matcher2 = pattern2.matcher(htmlLower);

            if (matcher2.find()) {
                int shortUsed = Integer.parseInt(matcher2.group(1));
                int shortAllowed = Integer.parseInt(matcher2.group(2));
                int longUsed = Integer.parseInt(matcher2.group(3));
                int longAllowed = Integer.parseInt(matcher2.group(4));

                shortUsed = Math.min(shortUsed, shortAllowed);
                longUsed = Math.min(longUsed, longAllowed);

                return String.format("%d/%d кр., %d/%d дл.",
                        shortUsed, shortAllowed, longUsed, longAllowed);
            }

            return "0/4 кр., 0/2 дл.";

        } catch (Exception e) {
            System.err.println("Ошибка парсинга лимитов из HTML: " + e.getMessage());
            return "0/4 кр., 0/2 дл.";
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || dateStr.equals("null")) {
            return "Не указана";
        }

        try {
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = dateStr.split("-");
                return parts[2] + "." + parts[1] + "." + parts[0];
            }
            return dateStr;
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void showPrisonerDetails() {
        int selectedRow = prisonersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заключенного для просмотра деталей",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int prisonerId = (int) tableModel.getValueAt(selectedRow, 0);
        String prisonerName = (String) tableModel.getValueAt(selectedRow, 2);

        JDialog dialog = new JDialog(this, "Детали заключенного: " + prisonerName, true);
        dialog.setSize(700, 500);
        dialog.setLocationRelativeTo(this);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Вкладка 1: Основная информация
        JPanel infoPanel = createBasicInfoPanel(selectedRow);
        tabbedPane.addTab("Основное", infoPanel);

        // Вкладка 2: Контакты
        JPanel contactsPanel = createContactsPanel(prisonerId);
        tabbedPane.addTab("Контакты", contactsPanel);

        // Вкладка 3: Свидания
        JPanel visitsPanel = createVisitsPanel(prisonerId);
        tabbedPane.addTab("Свидания", visitsPanel);

        dialog.add(tabbedPane);
        dialog.setVisible(true);
    }

    private JPanel createBasicInfoPanel(int row) {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        textArea.append("=== ОСНОВНАЯ ИНФОРМАЦИЯ ===\n\n");
        textArea.append("ID: " + tableModel.getValueAt(row, 0) + "\n");
        textArea.append("Номер: " + tableModel.getValueAt(row, 1) + "\n");
        textArea.append("ФИО: " + tableModel.getValueAt(row, 2) + "\n");
        textArea.append("Дата рождения: " + tableModel.getValueAt(row, 3) + "\n");
        textArea.append("Контакты: " + tableModel.getValueAt(row, 4) + "\n");
        textArea.append("Лимиты свиданий: " + tableModel.getValueAt(row, 5) + "\n");

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createContactsPanel(int prisonerId) {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea textArea = new JTextArea("Загрузка контактов...");
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Загружаем контакты
        new Thread(() -> {
            try {
                String contactsJson = ApiClient.getPrisonerContacts(prisonerId);
                String contactsText = formatContacts(contactsJson);

                SwingUtilities.invokeLater(() -> {
                    textArea.setText("=== КОНТАКТЫ ЗАКЛЮЧЕННОГО ===\n\n" + contactsText);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    textArea.setText("Ошибка загрузки контактов: " + e.getMessage());
                });
            }
        }).start();

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createVisitsPanel(int prisonerId) {
        JPanel panel = new JPanel(new BorderLayout());

        JTextArea textArea = new JTextArea("Загрузка запросов на свидания...");
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Загружаем запросы на свидания
        new Thread(() -> {
            try {
                String visitsJson = ApiClient.getPrisonerVisitRequests(prisonerId);
                String visitsText = formatVisits(visitsJson);

                SwingUtilities.invokeLater(() -> {
                    textArea.setText("=== ЗАПРОСЫ НА СВИДАНИЯ ===\n\n" + visitsText);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    textArea.setText("Ошибка загрузки свиданий: " + e.getMessage());
                });
            }
        }).start();

        panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return panel;
    }

    private String formatContacts(String json) {
        try {
            if (json == null || json.trim().isEmpty() || json.equals("[]")) {
                return "Нет контактов";
            }

            StringBuilder result = new StringBuilder();
            String content = json.substring(1, json.length() - 1).trim();
            String[] contacts = content.split("\\},\\s*\\{");

            for (int i = 0; i < contacts.length; i++) {
                String contact = contacts[i].replace("{", "").replace("}", "").trim();
                String[] fields = contact.split(",");

                String id = "?";
                String name = "Неизвестно";
                String relation = "Не указано";
                String approved = "Нет";

                for (String field : fields) {
                    field = field.trim();
                    if (field.startsWith("\"contactId\":")) {
                        id = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"fullName\":")) {
                        name = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"relation\":")) {
                        relation = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"approved\":")) {
                        approved = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    }
                }

                result.append(i + 1).append(". ").append(name)
                        .append(" (").append(relation).append(")\n")
                        .append("   ID: ").append(id)
                        .append(", Одобрен: ").append("true".equals(approved) ? "Да" : "Нет")
                        .append("\n\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "Ошибка форматирования: " + e.getMessage();
        }
    }

    private String formatVisits(String json) {
        try {
            if (json == null || json.trim().isEmpty() || json.equals("[]")) {
                return "Нет запросов на свидания";
            }

            StringBuilder result = new StringBuilder();
            String content = json.substring(1, json.length() - 1).trim();
            String[] visits = content.split("\\},\\s*\\{");

            for (int i = 0; i < visits.length; i++) {
                String visit = visits[i].replace("{", "").replace("}", "").trim();
                String[] fields = visit.split(",");

                String id = "?";
                String date = "Не указана";
                String type = "Неизвестно";
                String status = "Ожидает";
                String contactId = "?";

                for (String field : fields) {
                    field = field.trim();
                    if (field.startsWith("\"requestId\":")) {
                        id = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"visitDate\":")) {
                        date = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"visitType\":")) {
                        type = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"status\":")) {
                        status = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    } else if (field.startsWith("\"contactId\":")) {
                        contactId = field.substring(field.indexOf(":") + 1).trim().replace("\"", "");
                    }
                }

                result.append(i + 1).append(". Запрос #").append(id).append("\n")
                        .append("   Дата: ").append(date).append("\n")
                        .append("   Тип: ").append(type).append("\n")
                        .append("   Контакт ID: ").append(contactId).append("\n")
                        .append("   Статус: ").append(status).append("\n\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "Ошибка форматирования: " + e.getMessage();
        }
    }

    private JPanel createAllVisitsTab() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"ID", "Заключенный", "Контакт", "Дата", "Тип", "Статус", "Действия"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Обновить");
        JButton approveButton = new JButton("Одобрить");
        JButton rejectButton = new JButton("Отклонить");

        refreshButton.addActionListener(e -> loadVisitRequests(model));

        // Обработчик "Одобрить"
        approveButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int requestId = (int) model.getValueAt(row, 0);
                approveVisitRequest(requestId, model); // ← передаем model для обновления
            }
        });

        // Обработчик "Отклонить"
        rejectButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int requestId = (int) model.getValueAt(row, 0);
                rejectVisitRequest(requestId, model); // ← передаем model для обновления
            }
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(approveButton);
        buttonPanel.add(rejectButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Загружаем данные
        loadVisitRequests(model);

        return panel;
    }

    private JPanel createNewVisitTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Поля формы
        JTextField prisonerField = new JTextField(10);
        JTextField contactField = new JTextField(10);
        JTextField dateField = new JTextField(15);
        dateField.setText(LocalDate.now().plusDays(7).format(DateTimeFormatter.ISO_DATE));
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"краткосрочное", "длительное"});

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("ID заключенного:"), gbc);
        gbc.gridx = 1;
        panel.add(prisonerField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("ID контакта:"), gbc);
        gbc.gridx = 1;
        panel.add(contactField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Дата свидания:"), gbc);
        gbc.gridx = 1;
        panel.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Тип:"), gbc);
        gbc.gridx = 1;
        panel.add(typeCombo, gbc);

        JButton createButton = new JButton("Создать запрос");
        createButton.addActionListener(e -> {
            try {
                int prisonerId = Integer.parseInt(prisonerField.getText());
                int contactId = Integer.parseInt(contactField.getText());
                String date = dateField.getText();
                String type = (String) typeCombo.getSelectedItem();

                new Thread(() -> {
                    try {
                        String result = ApiClient.createVisitRequest(prisonerId, contactId, date, type);
                        JOptionPane.showMessageDialog(panel, result, "Результат",
                                result.contains("Успех") ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(panel, "Ошибка: " + ex.getMessage(),
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                }).start();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Введите корректные ID", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        panel.add(createButton, gbc);

        return panel;
    }

    private void loadVisitRequests(DefaultTableModel model) {
        new Thread(() -> {
            try {
                String html = ApiClient.getAllVisitRequests();

                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0);

                    try {
                        // Парсим HTML и создаем объекты VisitRequest
                        List<VisitRequest> requests = parseVisitRequestsFromHtml(html);

                        for (VisitRequest req : requests) {
                            model.addRow(new Object[]{
                                    req.getRequestId(),
                                    req.getPrisonerId(),
                                    req.getContactId(),
                                    req.getVisitDate(),
                                    req.getVisitType(),
                                    req.getStatus(),
                                    "Действия"
                            });
                        }

                        System.out.println("Загружено запросов из HTML: " + requests.size());

                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга: " + e.getMessage());
                        // Для отладки покажем часть HTML
                        JOptionPane.showMessageDialog(null,
                                "Первые 300 символов HTML:\n" +
                                        html.substring(0, Math.min(300, html.length())),
                                "Отладка", JOptionPane.INFORMATION_MESSAGE);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<VisitRequest> parseVisitRequestsFromHtml(String html) {
        List<VisitRequest> requests = new ArrayList<>();

        try {
            // Ищем таблицу
            int tableStart = html.indexOf("<table");
            if (tableStart == -1) {
                System.out.println("Таблица не найдена");
                return requests;
            }

            // Извлекаем содержимое таблицы
            tableStart = html.indexOf(">", tableStart) + 1;
            int tableEnd = html.indexOf("</table>", tableStart);
            String tableContent = html.substring(tableStart, tableEnd);

            // Разбиваем на строки
            String[] rows = tableContent.split("</tr>");

            for (String row : rows) {
                if (!row.contains("<td>")) continue; // Пропускаем не-строки

                // Извлекаем ячейки с помощью общего метода
                List<String> cells = extractTableCells(row);

                // В таблице 7 колонок: ID, prisoner, contact, requestDate, visitDate, type, status
                if (cells.size() >= 7) {
                    try {
                        VisitRequest req = new VisitRequest();
                        req.setRequestId(Integer.parseInt(cells.get(0)));
                        req.setPrisonerId(Integer.parseInt(cells.get(1)));
                        req.setContactId(Integer.parseInt(cells.get(2)));

                        // Даты - нужно конвертировать из String в LocalDate
                        if (!cells.get(4).isEmpty()) {
                            try {
                                req.setVisitDate(java.time.LocalDate.parse(cells.get(4)));
                            } catch (Exception e) {
                                System.err.println("Ошибка парсинга даты: " + cells.get(4));
                            }
                        }

                        req.setVisitType(cells.get(5));
                        req.setStatus(cells.get(6));

                        requests.add(req);

                    } catch (NumberFormatException e) {
                        // Пропускаем строку заголовка или некорректные данные
                    } catch (Exception e) {
                        System.err.println("Ошибка создания VisitRequest: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка парсинга HTML: " + e.getMessage());
        }

        return requests;
    }

    private void approveVisitRequest(int requestId, DefaultTableModel model) {
        new Thread(() -> {
            try {
                String result = ApiClient.approveVisitRequest(requestId);

                SwingUtilities.invokeLater(() -> {
                    if (result.contains("Успех") || result.contains("успех")) {
                        JOptionPane.showMessageDialog(null, "Запрос одобрен", "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadVisitRequests(model); // Обновляем таблицу после успеха
                    } else {
                        JOptionPane.showMessageDialog(null, "Ошибка: " + result, "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Ошибка: " + e.getMessage(), "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void rejectVisitRequest(int requestId, DefaultTableModel model) {
        new Thread(() -> {
            try {
                String result = ApiClient.rejectVisitRequest(requestId, ""); // Пустая причина

                SwingUtilities.invokeLater(() -> {
                    if (result.contains("Успех") || result.contains("успех")) {
                        JOptionPane.showMessageDialog(null, "Запрос отклонен", "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadVisitRequests(model); // Обновляем таблицу после успеха
                    } else {
                        JOptionPane.showMessageDialog(null, "Ошибка: " + result, "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Ошибка: " + e.getMessage(), "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void showAddPrisonerDialog() {
        JDialog dialog = new JDialog(this, "Добавить заключенного", true);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JTextField numberField = new JTextField(15);
        JTextField nameField = new JTextField(25);
        JTextField dateField = new JTextField(10);
        dateField.setText("гггг-мм-дд");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Номер:*"), gbc);
        gbc.gridx = 1;
        panel.add(numberField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("ФИО:*"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Дата рождения:"), gbc);
        gbc.gridx = 1;
        panel.add(dateField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        saveButton.addActionListener(e -> {
            String number = numberField.getText().trim();
            String name = nameField.getText().trim();
            String date = dateField.getText().trim();

            if (date.equals("гггг-мм-дд")) {
                date = "";
            }

            if (number.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Заполните обязательные поля (*)", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            savePrisoner(number, name, date, dialog);
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(buttonPanel, gbc);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private void savePrisoner(String number, String name, String date, JDialog dialog) {
        saveButton.setEnabled(false);
        saveButton.setText("Сохранение...");

        new Thread(() -> {
            try {
                String result = ApiClient.addPrisoner(number, name, date);

                SwingUtilities.invokeLater(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Сохранить");

                    if (result.contains("\"success\"") || result.contains("успех")) {
                        JOptionPane.showMessageDialog(dialog,
                                "Заключенный успешно добавлен!",
                                "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        dialog.dispose();
                        checkServerAndLoadData();
                    } else {
                        JOptionPane.showMessageDialog(dialog,
                                "Ошибка: " + result,
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    saveButton.setEnabled(true);
                    saveButton.setText("Сохранить");
                    JOptionPane.showMessageDialog(dialog,
                            "Ошибка соединения: " + e.getMessage(),
                            "Ошибка сети",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void deleteSelectedPrisoner() {
        int row = prisonersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите заключенного", "Внимание", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int prisonerId = (int) tableModel.getValueAt(row, 0);
        String prisonerName = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить заключенного: " + prisonerName + "?",
                "Подтверждение",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    String result = ApiClient.deletePrisonerWithRelations(prisonerId);

                    SwingUtilities.invokeLater(() -> {
                        if (result.contains("\"success\"") || result.contains("успех")) {
                            tableModel.removeRow(row);
                            statusLabel.setText("Удалено: " + prisonerName);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Ошибка: " + result,
                                    "Ошибка",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                                "Ошибка соединения: " + e.getMessage(),
                                "Ошибка сети",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            }).start();
        }
    }

    private void addContactToSelectedPrisoner() {
        int row = prisonersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заключенного из таблицы",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int prisonerId = (int) tableModel.getValueAt(row, 0);
        final String prisonerName = (String) tableModel.getValueAt(row, 2);

        // Создаем диалог
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JTextField inputField = new JTextField(20);
        JTextField relationField = new JTextField("родственник");
        JTextField birthDateField = new JTextField("2000-01-01");

        panel.add(new JLabel("ID контакта или ФИО:"));
        panel.add(inputField);
        panel.add(new JLabel("Отношение:"));
        panel.add(relationField);
        panel.add(new JLabel("Дата рождения:"));
        panel.add(birthDateField);

        int option = JOptionPane.showConfirmDialog(this, panel,
                "Добавить контакт для: " + prisonerName,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        final String input = inputField.getText().trim();
        final String relation = relationField.getText().trim();
        final String birthDate = birthDateField.getText().trim();

        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите ID или ФИО", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            try {
                String result;

                // Пробуем как ID
                try {
                    int contactId = Integer.parseInt(input);
                    result = ApiClient.addContactToPrisoner(prisonerId, contactId);
                } catch (NumberFormatException e) {
                    // Создаем новый контакт
                    result = ApiClient.createContactForPrisoner(prisonerId, input, birthDate, relation);
                }

                final String finalResult = result;

                SwingUtilities.invokeLater(() -> {
                    handleAddContactResponse(finalResult, prisonerId);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(AdminPanel.this,
                            "Ошибка: " + e.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    // Отдельный метод для обработки ответа
    private void handleAddContactResponse(String result, int prisonerId) {
        System.out.println("Ответ сервера: " + result);

        if (result != null && (result.contains("\"success\":true") ||
                result.contains("\"success\": true") ||
                result.contains("Успех"))) {

            JOptionPane.showMessageDialog(this,
                    "Контакт успешно добавлен!",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);

            // Обновляем интерфейс
            showAndRefreshContacts();

        } else if (result != null) {
            // Извлекаем сообщение об ошибке
            String errorMsg = extractErrorMessage(result);
            JOptionPane.showMessageDialog(this,
                    "Ошибка: " + errorMsg,
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showAndRefreshContacts() {
        int row = prisonersTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Выберите заключенного");
            return;
        }

        int prisonerId = (int) tableModel.getValueAt(row, 0);
        String prisonerName = (String) tableModel.getValueAt(row, 2);

        new Thread(() -> {
            try {
                String json = ApiClient.getPrisonerContacts(prisonerId);

                SwingUtilities.invokeLater(() -> {
                    // Создаём диалог с контактами
                    JDialog dialog = new JDialog(this, "Контакты: " + prisonerName, true);
                    dialog.setSize(500, 400);

                    // Парсим JSON и создаём таблицу
                    JTextArea textArea = new JTextArea(json);
                    textArea.setEditable(false);

                    dialog.add(new JScrollPane(textArea));
                    dialog.setLocationRelativeTo(this);
                    dialog.setVisible(true);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Ошибка загрузки контактов: " + e.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void removeContactFromPrisoner() {
        int prisonerRow = prisonersTable.getSelectedRow();
        if (prisonerRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Выберите заключенного из таблицы",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int prisonerId = (int) tableModel.getValueAt(prisonerRow, 0);
        String prisonerName = (String) tableModel.getValueAt(prisonerRow, 2);

        // Получаем контакты этого заключенного
        List<Contact> contacts = getContactsForSelectedPrisoner();
        if (contacts == null || contacts.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "У выбранного заключенного нет контактов",
                    "Внимание",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Создаем диалог выбора контакта
        String[] contactOptions = contacts.stream()
                .map(c -> c.getContactId() + " - " + c.getFullName() + " (" + c.getRelation() + ")")
                .toArray(String[]::new);

        String selectedContact = (String) JOptionPane.showInputDialog(this,
                "Выберите контакт для удаления у заключенного: " + prisonerName,
                "Удалить контакт",
                JOptionPane.QUESTION_MESSAGE,
                null,
                contactOptions,
                contactOptions[0]);

        if (selectedContact == null) return;

        int contactId = Integer.parseInt(selectedContact.split(" - ")[0]);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить контакт " + selectedContact + " у заключенного " + prisonerName + "?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try {
                String result = ApiClient.removeContactFromPrisoner(prisonerId, contactId);

                SwingUtilities.invokeLater(() -> {
                    if (result.contains("\"success\":true") || result.contains("\"success\": true")) {
                        JOptionPane.showMessageDialog(this,
                                "Контакт успешно удалён",
                                "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        refreshContactsTable(); // Обновить таблицу контактов
                    } else {
                        String errorMsg = extractErrorMessage(result);
                        JOptionPane.showMessageDialog(this,
                                "Ошибка: " + errorMsg,
                                "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Ошибка соединения: " + e.getMessage(),
                            "Ошибка сети",
                            JOptionPane.ERROR_MESSAGE);
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Неизвестная ошибка: " + e.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                });
            }
        }).start();
    }

    // Вспомогательный метод для извлечения ошибки из JSON
    private String extractErrorMessage(String json) {
        try {
            if (json.contains("\"error\"")) {
                int start = json.indexOf("\"error\":\"") + 9;
                int end = json.indexOf("\"", start);
                if (start > 8 && end > start) {
                    return json.substring(start, end);
                }
            } else if (json.contains("error")) {
                // Альтернативный формат
                return json.replaceAll("\"", "").replace("{", "").replace("}", "");
            }
            return json;
        } catch (Exception e) {
            return json;
        }
    }

    // Вспомогательный метод для получения контактов выбранного заключенного
    private List<Contact> getContactsForSelectedPrisoner() {
        int row = prisonersTable.getSelectedRow();
        if (row == -1) return new ArrayList<>();

        int prisonerId = (int) tableModel.getValueAt(row, 0);

        try {
            String json = ApiClient.getPrisonerContacts(prisonerId);
            return parseContactsFromJson(json);

        } catch (Exception e) {
            System.err.println("Ошибка получения контактов: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Contact> parseContactsFromJson(String json) {
        List<Contact> contacts = new ArrayList<>();

        try {
            if (json == null || json.trim().isEmpty() || json.equals("[]") || json.contains("\"error\"")) {
                return contacts;
            }

            json = json.trim();
            if (!json.startsWith("[") || !json.endsWith("]")) {
                return contacts;
            }

            String content = json.substring(1, json.length() - 1).trim();
            if (content.isEmpty()) {
                return contacts;
            }

            String[] contactStrings = content.split("\\},\\s*\\{");

            for (String contactStr : contactStrings) {
                contactStr = contactStr.replace("{", "").replace("}", "").trim();

                Contact contact = new Contact();
                String[] fields = contactStr.split(",");

                for (String field : fields) {
                    field = field.trim();
                    String[] parts = field.split(":", 2);
                    if (parts.length == 2) {
                        String key = parts[0].trim().replace("\"", "");
                        String value = parts[1].trim().replace("\"", "");

                        switch (key) {
                            case "contactId":
                            case "id":
                                contact.setContactId(Integer.parseInt(value));
                                break;
                            case "fullName":
                            case "name":
                                contact.setFullName(value);
                                break;
                            case "relation":
                            case "relationship":
                                contact.setRelation(value);
                                break;
                            case "approved":
                                contact.setApproved("true".equalsIgnoreCase(value));
                                break;
                            case "birthDate":
                                if (!value.equals("null") && !value.isEmpty()) {
                                    try {
                                        contact.setBirthDate(java.time.LocalDate.parse(value));
                                    } catch (Exception e) {
                                    }
                                }
                                break;
                        }
                    }
                }

                if (contact.getContactId() > 0 && contact.getFullName() != null) {
                    contacts.add(contact);
                }
            }

        } catch (Exception e) {
            System.err.println("Ошибка парсинга контактов: " + e.getMessage());
        }

        return contacts;
    }

    private void openVisitManagement() {
        JDialog dialog = new JDialog(this, "Управление свиданиями", true);
        dialog.setSize(1000, 600);
        dialog.setLocationRelativeTo(this);

        JTabbedPane tabbedPane = new JTabbedPane();

        // Вкладка 1: Все запросы (уже есть)
        tabbedPane.addTab("Запросы на свидания", createAllVisitsTab());

        // Вкладка 2: Состоявшиеся свидания (НОВАЯ!)
        tabbedPane.addTab("Свидания", createVisitsManagementTab());

        // Вкладка 3: Новый запрос
        tabbedPane.addTab("Новый запрос", createNewVisitTab());

        dialog.add(tabbedPane);
        dialog.setVisible(true);
    }

    // Новая вкладка для управления свиданиями
    private JPanel createVisitsManagementTab() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"ID", "Заключенный", "Контакт", "Дата", "Тип", "Статус", "Действия"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        JTable table = new JTable(model);

        // Загрузка данных
        loadVisitsData(model);

        // Кнопки
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Обновить");
        JButton completeButton = new JButton("Отметить состоявшимся");
        JButton cancelButton = new JButton("Отменить");

        refreshButton.addActionListener(e -> loadVisitsData(model));
        completeButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int visitId = (int) model.getValueAt(row, 0);
                markVisitAsCompleted(visitId, model);
            }
        });
        cancelButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                int visitId = (int) model.getValueAt(row, 0);
                cancelVisit(visitId, model);
            }
        });

        buttonPanel.add(refreshButton);
        buttonPanel.add(completeButton);
        buttonPanel.add(cancelButton);

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // Загрузка данных о свиданиях
    private void loadVisitsData(DefaultTableModel model) {
        new Thread(() -> {
            try {
                String html = ApiClient.getAllVisits(); // Это будет HTML

                SwingUtilities.invokeLater(() -> {
                    model.setRowCount(0);

                    try {
                        // Парсим HTML таблицу (аналогично parseVisitRequestsFromHtml)
                        List<Visit> visits = parseVisitsFromHtml(html);

                        for (Visit visit : visits) {
                            model.addRow(new Object[]{
                                    visit.getVisitId(),
                                    visit.getPrisonerId(),
                                    visit.getContactId(),
                                    visit.getVisitDate(),
                                    visit.getVisitType(),
                                    visit.getStatus(),
                                    "Действия"
                            });
                        }

                        System.out.println("Загружено свиданий: " + visits.size());

                    } catch (Exception e) {
                        System.err.println("Ошибка парсинга свиданий: " + e.getMessage());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Методы для работы со свиданиями
    private void markVisitAsCompleted(int visitId, DefaultTableModel model) {
        new Thread(() -> {
            try {
                String result = ApiClient.markVisitAsCompleted(visitId);

                SwingUtilities.invokeLater(() -> {
                    if (result.contains("Успех") || result.contains("успех")) {
                        JOptionPane.showMessageDialog(null, "Свидание отмечено как состоявшееся", "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadVisitsData(model); // Обновляем таблицу
                    } else {
                        JOptionPane.showMessageDialog(null, "Ошибка: " + result, "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Ошибка: " + e.getMessage(), "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void cancelVisit(int visitId, DefaultTableModel model) {
        new Thread(() -> {
            try {
                String result = ApiClient.cancelVisit(visitId);

                SwingUtilities.invokeLater(() -> {
                    if (result.contains("Успех") || result.contains("успех")) {
                        JOptionPane.showMessageDialog(null, "Свидание отменено", "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadVisitsData(model); // Обновляем таблицу
                    } else {
                        JOptionPane.showMessageDialog(null, "Ошибка: " + result, "Ошибка",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Ошибка: " + e.getMessage(), "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    // Метод для обновления таблицы контактов
    private void refreshContactsTable() {
        statusLabel.setText("Контакты обновлены - " + new java.util.Date());
    }

    // Метод для парсинга HTML таблицы свиданий
    private List<Visit> parseVisitsFromHtml(String html) {
        List<Visit> visits = new ArrayList<>();

        try {
            // Ищем таблицу
            int tableStart = html.indexOf("<table");
            if (tableStart == -1) {
                System.out.println("Таблица свиданий не найдена в HTML");
                return visits;
            }

            // Извлекаем содержимое таблицы
            tableStart = html.indexOf(">", tableStart) + 1;
            int tableEnd = html.indexOf("</table>", tableStart);
            if (tableEnd == -1) {
                System.out.println("Не найден конец таблицы свиданий");
                return visits;
            }

            String tableContent = html.substring(tableStart, tableEnd);

            // Разбиваем на строки
            String[] rows = tableContent.split("</tr>");

            for (String row : rows) {
                if (!row.contains("<td>")) continue; // Пропускаем не-строки

                // Извлекаем ячейки
                List<String> cells = extractTableCells(row);
                if (cells.size() >= 6) {
                    try {
                        Visit visit = new Visit();
                        visit.setVisitId(Integer.parseInt(cells.get(0)));
                        visit.setPrisonerId(Integer.parseInt(cells.get(1)));
                        visit.setContactId(Integer.parseInt(cells.get(2)));

                        // Дата
                        if (!cells.get(3).isEmpty()) {
                            try {
                                visit.setVisitDate(java.time.LocalDate.parse(cells.get(3)));
                            } catch (Exception e) {
                                System.err.println("Ошибка парсинга даты: " + cells.get(3));
                            }
                        }

                        visit.setVisitType(cells.get(4));
                        visit.setStatus(cells.get(5));

                        visits.add(visit);

                    } catch (NumberFormatException e) {
                        // Пропускаем строку заголовка или некорректные данные
                    } catch (Exception e) {
                        System.err.println("Ошибка создания Visit: " + e.getMessage());
                    }
                }
            }

            System.out.println("Распарсено свиданий: " + visits.size());

        } catch (Exception e) {
            System.err.println("Ошибка парсинга HTML свиданий: " + e.getMessage());
            e.printStackTrace();
        }

        return visits;
    }

    // Вспомогательный метод для извлечения ячеек из строки таблицы HTML
    private List<String> extractTableCells(String rowHtml) {
        List<String> cells = new ArrayList<>();

        try {
            int start = 0;

            while (true) {
                // Ищем начало ячейки <td>
                int tdStart = rowHtml.indexOf("<td", start);
                if (tdStart == -1) break;

                // Пропускаем атрибуты и находим закрывающий >
                tdStart = rowHtml.indexOf(">", tdStart);
                if (tdStart == -1) break;
                tdStart++; // Переходим к содержимому

                // Ищем конец ячейки </td>
                int tdEnd = rowHtml.indexOf("</td>", tdStart);
                if (tdEnd == -1) break;

                // Извлекаем содержимое ячейки
                String cellContent = rowHtml.substring(tdStart, tdEnd);

                // Очищаем от HTML тегов
                cellContent = cellContent
                        .replaceAll("<[^>]+>", "")      // Удаляем все HTML теги
                        .replaceAll("&nbsp;", " ")       // Заменяем неразрывные пробелы
                        .replaceAll("\\s+", " ")         // Заменяем множественные пробелы на один
                        .replaceAll("\\n", " ")          // Удаляем переносы строк
                        .trim();

                // Удаляем HTML entity коды (если есть)
                cellContent = cellContent
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&amp;", "&")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'");

                cells.add(cellContent);

                start = tdEnd + 5;
            }

        } catch (Exception e) {
            System.err.println("Ошибка при извлечении ячеек: " + e.getMessage());
        }

        return cells;
    }

    private void showConnectionError(ServerStatus status) {
        JTextArea textArea = new JTextArea(status.getDiagnostics());
        textArea.setEditable(false);

        JOptionPane.showMessageDialog(this, textArea,
                "Ошибка соединения с сервером",
                JOptionPane.ERROR_MESSAGE);
    }

    private static class PrisonerData {
        int id;
        String number;
        String name;
        String birthDate;
    }

    // Переменные для кнопок
    private JButton saveButton;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new AdminPanel();
        });
    }
}