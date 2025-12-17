package main.java.com.prison.dating.api.models;

import java.time.LocalDate;

public class Contact {
    private int contactId;
    private String fullName;
    private LocalDate birthDate;
    private String relation;
    private boolean isApproved;
    private String status;

    // Конструкторы
    public Contact() {}

    // Геттеры и сеттеры
    public int getContactId() { return contactId; }
    public void setContactId(int contactId) { this.contactId = contactId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

}