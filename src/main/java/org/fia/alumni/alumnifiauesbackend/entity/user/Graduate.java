package org.fia.alumni.alumnifiauesbackend.entity.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "graduates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Graduate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", unique = true, length = 20)
    private String studentId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "identity_document", unique = true, length = 20)
    private String identityDocument;

    @Column(name = "admission_year")
    private Integer admissionYear;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "total_years")
    private Integer totalYears;

    @Column(name = "gpa")
    private BigDecimal gpa;

    @Column(name = "career_id")
    private Long careerId;

    @Column(name = "degree_image", length = 255)
    private String degreeImage;

    @Column(name = "verified")
    private Boolean verified;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}