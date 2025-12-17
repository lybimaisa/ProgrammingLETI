package main.java.com.prison.dating.api.models;

import java.time.LocalDate;

public class Visit {
    private int visitId;
    private int prisonerId;
    private int contactId;
    private LocalDate visitDate;
    private String visitType;
    private String status;

    // Конструкторы
    public Visit() {}

    // Геттеры и сеттеры
    public int getVisitId() { return visitId; }
    public void setVisitId(int visitId) { this.visitId = visitId; }

    public int getPrisonerId() { return prisonerId; }
    public void setPrisonerId(int prisonerId) { this.prisonerId = prisonerId; }

    public int getContactId() { return contactId; }
    public void setContactId(int contactId) { this.contactId = contactId; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}