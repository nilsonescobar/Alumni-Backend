package org.fia.alumni.alumnifiauesbackend.controller.reports;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.reports.*;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.fia.alumni.alumnifiauesbackend.security.SecurityUtils;
import org.fia.alumni.alumnifiauesbackend.service.reports.ReportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.function.Function;

@RestController
@RequestMapping("/api/v1/director/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    @GetMapping("/by-career")
    public ResponseEntity<?> getCareerReport(@RequestParam(required = false) String format) {
        validateDirector();
        List<ReportCareerDTO> data = reportService.getCareerReportData();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "ByCareer",
                    List.of("Carrera", "Universidad", "Total Graduados"),
                    data,
                    d -> List.of(d.getCareerName(), d.getUniversityName(), d.getUserCount())
            ), "ReporteCarreras.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/by-graduation-year")
    public ResponseEntity<?> getByGraduationYear(
            @RequestParam int startYear,
            @RequestParam int endYear,
            @RequestParam(required = false) String format) {
        validateDirector();
        List<UserDetailReportDTO> data = reportService.findUsersByGraduationYearRange(startYear, endYear);
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "ByGraduationYear",
                    List.of("ID", "Nombres", "Apellidos", "Carnet", "Email", "Carrera", "Año Graduación", "Año Ingreso"),
                    data,
                    d -> List.of(d.getUserId(), d.getFirstName(), d.getLastName(),
                            d.getStudentId(), d.getEmail(), d.getCareerName(),
                            d.getGraduationYear(), d.getAdmissionYear())
            ), "ReporteGraduacion.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/by-admission-year")
    public ResponseEntity<?> getByAdmissionYear(
            @RequestParam int startYear,
            @RequestParam int endYear,
            @RequestParam(required = false) String format) {
        validateDirector();
        List<UserDetailReportDTO> data = reportService.findUsersByAdmissionYearRange(startYear, endYear);
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "ByAdmissionYear",
                    List.of("ID", "Nombres", "Apellidos", "Carnet", "Email", "Carrera", "Año Graduación", "Año Ingreso"),
                    data,
                    d -> List.of(d.getUserId(), d.getFirstName(), d.getLastName(),
                            d.getStudentId(), d.getEmail(), d.getCareerName(),
                            d.getGraduationYear(), d.getAdmissionYear())
            ), "ReporteIngreso.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/employed/current")
    public ResponseEntity<?> getCurrentlyEmployed(@RequestParam(required = false) String format) {
        validateDirector();
        List<CurrentlyEmployedDTO> data = reportService.findCurrentlyEmployedUsers();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "Employed",
                    List.of("ID", "Nombres", "Apellidos", "Carnet", "Empresa", "Cargo", "País", "Inicio"),
                    data,
                    d -> List.of(d.getUserId(), d.getFirstName(), d.getLastName(),
                            d.getStudentId(), d.getCompanyName(), d.getPosition(),
                            d.getCountryName(), d.getStartDate())
            ), "ReporteEmpleados.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/employers/top")
    public ResponseEntity<?> getTopEmployers(@RequestParam(required = false) String format) {
        validateDirector();
        List<TopEmployerDTO> data = reportService.getTopEmployersReport();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "TopEmployers",
                    List.of("Empresa", "Total Graduados"),
                    data,
                    d -> List.of(d.getCompanyName(), d.getGraduateCount())
            ), "ReporteEmpresas.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/geographic-distribution/current")
    public ResponseEntity<?> getGeographic(@RequestParam(required = false) String format) {
        validateDirector();
        List<GeographicDistributionDTO> data = reportService.getGeographicDistributionReport();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "Geographic",
                    List.of("País", "Total Graduados"),
                    data,
                    d -> List.of(d.getCountryName(), d.getGraduateCount())
            ), "ReporteGeografico.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/salaries/by-career")
    public ResponseEntity<?> getSalaryByCareer(@RequestParam(required = false) String format) {
        validateDirector();
        List<SalaryAnalysisDTO> data = reportService.getSalaryAnalysisReport();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "SalaryByCareer",
                    List.of("Carrera", "Rango Salarial", "Total"),
                    data,
                    d -> List.of(d.getCareerName(), d.getSalaryRange(), d.getGraduateCount())
            ), "ReporteSalarioCarrera.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/salaries/by-user")
    public ResponseEntity<?> getSalaryByUser(@RequestParam(required = false) String format) {
        validateDirector();
        List<UserSalaryDTO> data = reportService.getUserSalariesReport();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "SalaryByUser",
                    List.of("ID", "Nombres", "Apellidos", "Carrera", "Empresa", "Cargo", "Salario"),
                    data,
                    d -> List.of(d.getUserId(), d.getFirstName(), d.getLastName(),
                            d.getCareerName(), d.getCompanyName(), d.getPosition(), d.getSalaryRange())
            ), "ReporteSalarioUsuario.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    @GetMapping("/postgraduate/by-degree")
    public ResponseEntity<?> getPostgraduate(@RequestParam(required = false) String format) {
        validateDirector();
        List<PostgraduateStudyDTO> data = reportService.getPostgraduateStudiesReport();
        if ("excel".equalsIgnoreCase(format)) {
            return excel(reportService.generateGenericExcel(
                    "Postgraduate",
                    List.of("Título", "Total"),
                    data,
                    d -> List.of(d.getDegreeTitle(), d.getGraduateCount())
            ), "ReportePostgrado.xlsx");
        }
        return ResponseEntity.ok(data);
    }

    // ── Helpers ──────────────────────────────────────────────

    private void validateDirector() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Usuario no encontrado"));
        if (user.getUserType() != User.UserType.DIRECTOR &&
                user.getUserType() != User.UserType.ADMIN) {
            throw new BadRequestException("Acceso denegado — se requiere rol DIRECTOR o ADMIN");
        }
    }

    private ResponseEntity<InputStreamResource> excel(ByteArrayInputStream stream, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(stream));
    }
}