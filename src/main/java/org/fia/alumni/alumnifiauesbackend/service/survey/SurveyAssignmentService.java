package org.fia.alumni.alumnifiauesbackend.service.survey;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.survey.AssignSurveyRequest;
import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyAssignment;
import org.fia.alumni.alumnifiauesbackend.entity.survey.SurveyAssignedUser;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyAssignmentRepository;
import org.fia.alumni.alumnifiauesbackend.repository.survey.SurveyAssignedUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SurveyAssignmentService {

    private final SurveyAssignmentRepository assignmentRepository;
    private final SurveyAssignedUserRepository assignedUserRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public SurveyAssignment assignSurvey(Long surveyId, AssignSurveyRequest request) {
        String typeStr = request.getAssignmentType() != null ? request.getAssignmentType().trim().toUpperCase() : "";

        if (typeStr.isEmpty()) {
            throw new IllegalArgumentException("Assignment type is required");
        }

        SurveyAssignment.AssignmentType aType;
        try {
            switch (typeStr) {
                case "TODOS": aType = SurveyAssignment.AssignmentType.ALL; break;
                case "CARRERA": aType = SurveyAssignment.AssignmentType.CAREER; break;
                case "ANIO_GRADUACION": aType = SurveyAssignment.AssignmentType.GRADUATION_YEAR; break;
                case "TIPO_USUARIO": aType = SurveyAssignment.AssignmentType.USER_TYPE; break;
                case "USUARIOS_ESPECIFICOS": aType = SurveyAssignment.AssignmentType.SPECIFIC_USERS; break;
                default: aType = SurveyAssignment.AssignmentType.valueOf(typeStr);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid assignment type: " + typeStr);
        }

        SurveyAssignment.UserType uType = null;
        if (request.getUserType() != null && !request.getUserType().trim().isEmpty()) {
            uType = SurveyAssignment.UserType.valueOf(request.getUserType().toUpperCase());
        }

        SurveyAssignment assignment = SurveyAssignment.builder()
                .surveyId(surveyId)
                .assignmentType(aType)
                .careerId(request.getCareerId())
                .graduationYearStart(request.getGraduationYearStart())
                .graduationYearEnd(request.getGraduationYearEnd())
                .userType(uType)
                .build();
        SurveyAssignment saved = assignmentRepository.save(assignment);

        List<Long> targetUserIds = getTargetUserIds(request, aType.name());

        if (!targetUserIds.isEmpty()) {
            List<SurveyAssignedUser> assignedUsers = targetUserIds.stream()
                    .map(userId -> SurveyAssignedUser.builder()
                            .assignmentId(saved.getId())
                            .userId(userId)
                            .notified(request.getSendNotification() != null ? request.getSendNotification() : false)
                            .build())
                    .toList();
            assignedUserRepository.saveAll(assignedUsers);

            if (Boolean.TRUE.equals(request.getSendNotification())) {
                List<String> targetEmails = getTargetEmails(targetUserIds);
                enviarNotificaciones(targetEmails);
            }
        }

        return saved;
    }

    @SuppressWarnings("unchecked")
    private List<Long> getTargetUserIds(AssignSurveyRequest request, String type) {
        if ("USUARIOS_ESPECIFICOS".equals(type) || "SPECIFIC_USERS".equals(type)) {
            return request.getUserIds() != null ? request.getUserIds() : new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder("SELECT u.id FROM users u ");
        if ("CARRERA".equals(type) || "ANIO_GRADUACION".equals(type)) {
            sql.append("JOIN profiles p ON u.id = p.user_id ");
        }
        sql.append("WHERE u.active = true AND u.account_deactivated_at IS NULL ");

        if ("CARRERA".equals(type) && request.getCareerId() != null) {
            sql.append("AND p.career_id = ").append(request.getCareerId()).append(" ");
        } else if ("ANIO_GRADUACION".equals(type) && request.getGraduationYearStart() != null && request.getGraduationYearEnd() != null) {
            sql.append("AND p.graduation_year >= ").append(request.getGraduationYearStart())
                    .append(" AND p.graduation_year <= ").append(request.getGraduationYearEnd()).append(" ");
        } else if (("TIPO_USUARIO".equals(type) || "USER_TYPE".equals(type)) && request.getUserType() != null) {
            sql.append("AND cast(u.user_type as text) = '").append(request.getUserType().toUpperCase()).append("' ");
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        List<Number> results = query.getResultList();

        return results.stream()
                .map(Number::longValue)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> getTargetEmails(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return new ArrayList<>();
        Query query = entityManager.createNativeQuery("SELECT email FROM users WHERE id IN :ids");
        query.setParameter("ids", userIds);
        return query.getResultList();
    }

    private void enviarNotificaciones(List<String> emails) {

    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getAssignments(Long surveyId) {
        return assignmentRepository.findBySurveyId(surveyId).stream()
                .map(a -> {
                    Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", a.getId());
                    map.put("assignmentType", a.getAssignmentType().name());
                    map.put("assignedAt", a.getAssignedAt());

                    if (a.getCareerId() != null) {
                        map.put("careerId", a.getCareerId());
                        try {
                            String careerName = (String) entityManager.createNativeQuery(
                                            "SELECT name FROM careers WHERE id = :id")
                                    .setParameter("id", a.getCareerId())
                                    .getSingleResult();
                            map.put("careerName", careerName);
                        } catch (Exception e) {
                            map.put("careerName", "Desconocida");
                        }
                    }

                    if (a.getGraduationYearStart() != null) {
                        map.put("graduationYearStart", a.getGraduationYearStart());
                        map.put("graduationYearEnd", a.getGraduationYearEnd());
                    }

                    if (a.getUserType() != null) {
                        map.put("userType", a.getUserType().name());
                    }

                    if (a.getAssignmentType() == SurveyAssignment.AssignmentType.SPECIFIC_USERS) {
                        List<SurveyAssignedUser> assignedUsers = assignedUserRepository.findByAssignmentId(a.getId());
                        List<Long> userIds = assignedUsers.stream().map(SurveyAssignedUser::getUserId).toList();
                        map.put("userIds", userIds);

                        List<Map<String, Object>> usersDetails = new ArrayList<>();
                        if (!userIds.isEmpty()) {
                            List<Object[]> results = entityManager.createNativeQuery(
                                            "SELECT u.id, p.first_name, p.last_name, u.email " +
                                                    "FROM users u " +
                                                    "LEFT JOIN profiles p ON u.id = p.user_id " +
                                                    "WHERE u.id IN :ids")
                                    .setParameter("ids", userIds)
                                    .getResultList();

                            for (Object[] row : results) {
                                Map<String, Object> uInfo = new java.util.LinkedHashMap<>();
                                uInfo.put("id", ((Number) row[0]).longValue());
                                String fname = row[1] != null ? row[1].toString() : "";
                                String lname = row[2] != null ? row[2].toString() : "";
                                uInfo.put("fullName", (fname + " " + lname).trim());
                                uInfo.put("email", row[3] != null ? row[3].toString() : "");
                                usersDetails.add(uInfo);
                            }
                        }
                        map.put("users", usersDetails);
                    }

                    return map;
                }).toList();
    }

    @Transactional
    public void removeAssignment(Long assignmentId) {
        List<SurveyAssignedUser> users = assignedUserRepository.findByAssignmentId(assignmentId);
        assignedUserRepository.deleteAll(users);
        assignmentRepository.deleteById(assignmentId);
    }
}