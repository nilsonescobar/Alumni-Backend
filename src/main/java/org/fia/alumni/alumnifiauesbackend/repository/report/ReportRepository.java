package org.fia.alumni.alumnifiauesbackend.repository.report;

import org.fia.alumni.alumnifiauesbackend.dto.reports.*;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;



import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT c.name AS careerName, u2.name AS universityName, COUNT(DISTINCT u.id) AS userCount
        FROM User u
        JOIN Profile p ON p.userId = u.id
        JOIN Career c ON c.id = p.careerId
        JOIN University u2 ON u2.id = c.universityId
        WHERE u.active = TRUE AND u.userType = :type
        GROUP BY c.name, u2.name
        ORDER BY userCount DESC
    """)
    List<ReportCareerDTO> countUsersByCareer(@Param("type") User.UserType type);

    @Query("""
        SELECT u.id AS userId, p.firstName AS firstName, p.lastName AS lastName,
               p.studentId AS studentId, u.email AS email,
               c.name AS careerName, p.graduationYear AS graduationYear,
               p.admissionYear AS admissionYear
        FROM User u
        JOIN Profile p ON p.userId = u.id
        LEFT JOIN Career c ON c.id = p.careerId
        WHERE u.active = TRUE AND u.userType = :type
        AND p.graduationYear BETWEEN :startYear AND :endYear
        ORDER BY p.graduationYear ASC
    """)
    List<UserDetailReportDTO> findUsersByGraduationYearRange(
            @Param("startYear") int startYear,
            @Param("endYear") int endYear,
            @Param("type") User.UserType type
    );

    @Query("""
        SELECT u.id AS userId, p.firstName AS firstName, p.lastName AS lastName,
               p.studentId AS studentId, u.email AS email,
               c.name AS careerName, p.graduationYear AS graduationYear,
               p.admissionYear AS admissionYear
        FROM User u
        JOIN Profile p ON p.userId = u.id
        LEFT JOIN Career c ON c.id = p.careerId
        WHERE u.active = TRUE AND u.userType = :type
        AND p.admissionYear BETWEEN :startYear AND :endYear
        ORDER BY p.admissionYear ASC
    """)
    List<UserDetailReportDTO> findUsersByAdmissionYearRange(
            @Param("startYear") int startYear,
            @Param("endYear") int endYear,
            @Param("type") User.UserType type
    );

    @Query("""
        SELECT u.id AS userId, p.firstName AS firstName, p.lastName AS lastName,
               p.studentId AS studentId, w.companyName AS companyName,
               w.position AS position, co.name AS countryName, w.startDate AS startDate
        FROM User u
        JOIN Profile p ON p.userId = u.id
        JOIN WorkExperience w ON w.userId = u.id
        LEFT JOIN Country co ON co.id = w.countryId
        WHERE u.active = TRUE AND w.isCurrent = TRUE
        ORDER BY p.lastName ASC
    """)
    List<CurrentlyEmployedDTO> findCurrentlyEmployedUsers();

    @Query("""
        SELECT w.companyName AS companyName, COUNT(DISTINCT w.userId) AS graduateCount
        FROM WorkExperience w
        JOIN User u ON u.id = w.userId
        WHERE u.active = TRUE AND w.isCurrent = TRUE
        GROUP BY w.companyName
        ORDER BY graduateCount DESC
    """)
    List<TopEmployerDTO> countUsersByCompany();

    @Query("""
        SELECT co.name AS countryName, COUNT(DISTINCT w.userId) AS graduateCount
        FROM WorkExperience w
        JOIN User u ON u.id = w.userId
        JOIN Country co ON co.id = w.countryId
        WHERE u.active = TRUE AND w.isCurrent = TRUE
        AND w.countryId IS NOT NULL
        GROUP BY co.name
        ORDER BY graduateCount DESC
    """)
    List<GeographicDistributionDTO> countCurrentlyEmployedUsersByCountry();

    @Query("""
        SELECT c.name AS careerName, w.salaryRange AS salaryRange,
               COUNT(DISTINCT w.userId) AS graduateCount
        FROM WorkExperience w
        JOIN User u ON u.id = w.userId
        JOIN Profile p ON p.userId = u.id
        LEFT JOIN Career c ON c.id = p.careerId
        WHERE u.active = TRUE AND w.isCurrent = TRUE
        AND w.salaryRange IS NOT NULL
        GROUP BY c.name, w.salaryRange
        ORDER BY c.name ASC, graduateCount DESC
    """)
    List<SalaryAnalysisDTO> countUsersByCareerAndSalaryRange();

    @Query("""
        SELECT u.id AS userId, p.firstName AS firstName, p.lastName AS lastName,
               c.name AS careerName, w.companyName AS companyName,
               w.position AS position, w.salaryRange AS salaryRange
        FROM WorkExperience w
        JOIN User u ON u.id = w.userId
        JOIN Profile p ON p.userId = u.id
        LEFT JOIN Career c ON c.id = p.careerId
        WHERE u.active = TRUE AND w.isCurrent = TRUE
        AND w.salaryRange IS NOT NULL
        ORDER BY p.lastName ASC
    """)
    List<UserSalaryDTO> findCurrentUserSalaries();

    @Query("""
        SELECT ps.degreeTitle AS degreeTitle, COUNT(DISTINCT ps.userId) AS graduateCount
        FROM PostgraduateStudy ps
        JOIN User u ON u.id = ps.userId
        WHERE u.active = TRUE
        GROUP BY ps.degreeTitle
        ORDER BY graduateCount DESC
    """)
    List<PostgraduateStudyDTO> countUsersByPostgraduateDegree();
}