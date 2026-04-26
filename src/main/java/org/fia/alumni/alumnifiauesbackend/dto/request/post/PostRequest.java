package org.fia.alumni.alumnifiauesbackend.dto.request.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @NotBlank(message = "El contenido no puede estar vacío")
    @Size(max = 5000, message = "El contenido no puede exceder 5000 caracteres")
    private String content;

    private Boolean isPublic = true;

    private List<String> mediaUrls;

    private List<String> tags;
}