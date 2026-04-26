package org.fia.alumni.alumnifiauesbackend.service.reports;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fia.alumni.alumnifiauesbackend.dto.reports.*;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.repository.report.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ReportCareerDTO> getCareerReportData() {
        return reportRepository.countUsersByCareer(User.UserType.GRADUATE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDetailReportDTO> findUsersByGraduationYearRange(int startYear, int endYear) {
        return reportRepository.findUsersByGraduationYearRange(startYear, endYear, User.UserType.GRADUATE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDetailReportDTO> findUsersByAdmissionYearRange(int startYear, int endYear) {
        return reportRepository.findUsersByAdmissionYearRange(startYear, endYear, User.UserType.GRADUATE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrentlyEmployedDTO> findCurrentlyEmployedUsers() {
        return reportRepository.findCurrentlyEmployedUsers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopEmployerDTO> getTopEmployersReport() {
        return reportRepository.countUsersByCompany();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GeographicDistributionDTO> getGeographicDistributionReport() {
        return reportRepository.countCurrentlyEmployedUsersByCountry();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalaryAnalysisDTO> getSalaryAnalysisReport() {
        return reportRepository.countUsersByCareerAndSalaryRange();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSalaryDTO> getUserSalariesReport() {
        return reportRepository.findCurrentUserSalaries();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostgraduateStudyDTO> getPostgraduateStudiesReport() {
        return reportRepository.countUsersByPostgraduateDegree();
    }

    @Override
    public <T> ByteArrayInputStream generateGenericExcel(
            String sheetName, List<String> headers,
            List<T> data, Function<T, List<Object>> rowMapper) {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(sheetName);

            // Header row con estilo
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Data rows
            int rowIdx = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowIdx++);
                List<Object> values = rowMapper.apply(item);
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = values.get(i);
                    if (value == null) cell.setCellValue("");
                    else if (value instanceof String) cell.setCellValue((String) value);
                    else if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
                    else if (value instanceof LocalDateTime) cell.setCellValue(value.toString());
                    else if (value instanceof LocalDate) cell.setCellValue(value.toString());
                    else cell.setCellValue(value.toString());
                }
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel: " + e.getMessage());
        }
    }
}