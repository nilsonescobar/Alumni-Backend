package org.fia.alumni.alumnifiauesbackend.dto.reports;

public interface UserDetailReportDTO {
    Long getUserId();
    String getFirstName();
    String getLastName();
    String getStudentId();
    String getEmail();
    String getCareerName();
    Integer getGraduationYear();
    Integer getAdmissionYear();
}