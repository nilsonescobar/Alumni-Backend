package org.fia.alumni.alumnifiauesbackend.dto.request.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 1000, message = "El comentario no puede exceder 1000 caracteres")
    private String content;
}