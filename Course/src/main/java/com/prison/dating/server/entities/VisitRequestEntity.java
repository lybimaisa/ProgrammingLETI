package main.java.com.prison.dating.server.entities;

import java.time.LocalDate;

public class VisitRequestEntity {
    private int requestId;
    private int prisonerId;
    private int contactId;
    private LocalDate requestDate;
    private LocalDate visitDate;
    private String status;  // "ожидает", "одобрена", "отклонена"
    private String visitType; // "краткосрочные", "длительное"

    // Конструкторы
    public VisitRequestEntity() {}

    // Геттеры и сеттеры
    public int getRequestId() { return requestId; }
    public void setRequestId(int requestId) { this.requestId = requestId; }

    public int getPrisonerId() { return prisonerId; }
    public void setPrisonerId(int prisonerId) { this.prisonerId = prisonerId; }

    public int getContactId() { return contactId; }
    public void setContactId(int contactId) { this.contactId = contactId; }

    public LocalDate getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDate requestDate) { this.requestDate = requestDate; }

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVisitType() { return visitType; }
    public void setVisitType(String visitType) { this.visitType = visitType; }

}