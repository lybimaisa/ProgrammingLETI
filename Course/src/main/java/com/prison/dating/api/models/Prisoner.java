package main.java.com.prison.dating.api.models;

import java.time.LocalDate;

public class Prisoner {
    private int prisonerId;
    private String fullName;
    private LocalDate birthDate;
    private String prisonerNumber;
    private String visitLimitsInfo;     // Информация о лимитах

    // Конструкторы
    public Prisoner() {}

    public int getPrisonerId() { return prisonerId; }
    public void setPrisonerId(int prisonerId) { this.prisonerId = prisonerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPrisonerNumber() { return prisonerNumber; }
    public void setPrisonerNumber(String prisonerNumber) { this.prisonerNumber = prisonerNumber; }

    public String getVisitLimitsInfo() { return visitLimitsInfo; }
    public void setVisitLimitsInfo(String visitLimitsInfo) {
        this.visitLimitsInfo = visitLimitsInfo;
    }

}