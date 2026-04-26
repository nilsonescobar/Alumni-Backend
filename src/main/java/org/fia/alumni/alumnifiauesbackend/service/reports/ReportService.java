package org.fia.alumni.alumnifiauesbackend.service.reports;

import org.fia.alumni.alumnifiauesbackend.dto.reports.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Function;

public interface ReportService {
    List<ReportCareerDTO> getCareerReportData();
    List<UserDetailReportDTO> findUsersByGraduationYearRange(int startYear, int endYear);
    List<UserDetailReportDTO> findUsersByAdmissionYearRange(int startYear, int endYear);
    List<CurrentlyEmployedDTO> findCurrentlyEmployedUsers();
    List<TopEmployerDTO> getTopEmployersReport();
    List<GeographicDistributionDTO> getGeographicDistributionReport();
    List<SalaryAnalysisDTO> getSalaryAnalysisReport();
    List<UserSalaryDTO> getUserSalariesReport();
    List<PostgraduateStudyDTO> getPostgraduateStudiesReport();
    <T> ByteArrayInputStream generateGenericExcel(
            String sheetName,
            List<String> headers,
            List<T> data,
            Function<T, List<Object>> rowMapper
    );
}