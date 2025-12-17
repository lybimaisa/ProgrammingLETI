package main.java.com.prison.dating.api.models;

public class VisitLimit {
    private int limitId;
    private int prisonerId;
    private int shortAllowed;
    private int longAllowed;
    private int shortUsed;
    private int longUsed;

    // Конструкторы
    public VisitLimit() {}

    public VisitLimit(int prisonerId, int shortAllowed, int longAllowed) {
        this.prisonerId = prisonerId;
        this.shortAllowed = shortAllowed;
        this.longAllowed = longAllowed;
        this.shortUsed = 0;
        this.longUsed = 0;
    }

    // Геттеры и сеттеры
    public int getLimitId() { return limitId; }
    public void setLimitId(int limitId) { this.limitId = limitId; }

    public int getPrisonerId() { return prisonerId; }
    public void setPrisonerId(int prisonerId) { this.prisonerId = prisonerId; }

    public int getShortAllowed() { return shortAllowed; }
    public void setShortAllowed(int shortAllowed) { this.shortAllowed = shortAllowed; }

    public int getLongAllowed() { return longAllowed; }
    public void setLongAllowed(int longAllowed) { this.longAllowed = longAllowed; }

    public int getShortUsed() { return shortUsed; }
    public void setShortUsed(int shortUsed) { this.shortUsed = shortUsed; }

    public int getLongUsed() { return longUsed; }
    public void setLongUsed(int longUsed) { this.longUsed = longUsed; }

    // Методы для проверки лимитов
    public int getShortRemaining() {
        return shortAllowed - shortUsed;
    }

    public int getLongRemaining() {
        return longAllowed - longUsed;
    }
}