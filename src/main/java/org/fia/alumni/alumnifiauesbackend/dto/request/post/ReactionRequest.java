package org.fia.alumni.alumnifiauesbackend.dto.request.post;

import jakarta.validation.constraints.NotNull;
import org.fia.alumni.alumnifiauesbackend.entity.post.PostReaction;

public record ReactionRequest(
        @NotNull(message = "El tipo de reacción es requerido")
        PostReaction.ReactionType reactionType
) {
}