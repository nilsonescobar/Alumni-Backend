package org.fia.alumni.alumnifiauesbackend.dto.surveys;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnswerDistributionDto {
    private String value;
    private long count;
    private double percentage;
}