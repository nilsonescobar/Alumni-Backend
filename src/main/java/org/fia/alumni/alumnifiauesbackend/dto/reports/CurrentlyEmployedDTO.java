package org.fia.alumni.alumnifiauesbackend.dto.reports;

import java.time.LocalDate;

public interface CurrentlyEmployedDTO {
    Long getUserId();
    String getFirstName();
    String getLastName();
    String getStudentId();
    String getCompanyName();
    String getPosition();
    String getCountryName();
    LocalDate getStartDate();
}