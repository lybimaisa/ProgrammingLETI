package main.java.com.prison.dating.server.entities;

import java.time.LocalDate;

public class PrisonerEntity {
    private int prisonerId;
    private String fullName;
    private LocalDate birthDate;
    private String prisonerNumber;

    // Конструкторы
    public PrisonerEntity() {}

    // Геттеры и сеттеры
    public int getPrisonerId() { return prisonerId; }
    public void setPrisonerId(int prisonerId) { this.prisonerId = prisonerId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getPrisonerNumber() { return prisonerNumber; }
    public void setPrisonerNumber(String prisonerNumber) { this.prisonerNumber = prisonerNumber; }

    @Override
    public String toString() {
        return prisonerNumber + " - " + fullName;
    }
}