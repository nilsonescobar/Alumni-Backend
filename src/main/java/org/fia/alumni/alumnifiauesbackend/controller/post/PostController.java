package org.fia.alumni.alumnifiauesbackend.controller.post;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.post.CommentRequest;
import org.fia.alumni.alumnifiauesbackend.dto.request.post.PostRequest;
import org.fia.alumni.alumnifiauesbackend.dto.request.post.ReactionRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.post.CommentResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.post.PostResponse;
import org.fia.alumni.alumnifiauesbackend.service.post.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPost(
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        PostResponse post = postService.createPost(request, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", post);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPublicFeed(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;
        Page<PostResponse> posts = postService.getPublicFeed(pageable, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", posts.getContent());
        response.put("page", posts.getNumber());
        response.put("size", posts.getSize());
        response.put("totalElements", posts.getTotalElements());
        response.put("totalPages", posts.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserPosts(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        Long currentUserId = authentication != null ? (Long) authentication.getPrincipal() : null;
        Page<PostResponse> posts = postService.getUserPosts(userId, pageable, currentUserId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", posts.getContent());
        response.put("page", posts.getNumber());
        response.put("size", posts.getSize());
        response.put("totalElements", posts.getTotalElements());
        response.put("totalPages", posts.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> getPostById(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = authentication != null ? (Long) authentication.getPrincipal() : null;
        PostResponse post = postService.getPostById(postId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", post);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> updatePost(
            @PathVariable Long postId,
            @Valid @RequestBody PostRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        PostResponse post = postService.updatePost(postId, request, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", post);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Map<String, Object>> deletePost(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        postService.deletePost(postId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Publicación eliminada exitosamente");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/reactions")
    public ResponseEntity<Map<String, Object>> addReaction(
            @PathVariable Long postId,
            @Valid @RequestBody ReactionRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        postService.addReaction(postId, request, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Reacción agregada exitosamente");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/reactions")
    public ResponseEntity<Map<String, Object>> removeReaction(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        postService.removeReaction(postId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Reacción eliminada exitosamente");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        CommentResponse comment = postService.addComment(postId, request, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", comment);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{postId}/comments")
    public ResponseEntity<Map<String, Object>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<CommentResponse> comments = postService.getComments(postId, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", comments.getContent());
        response.put("page", comments.getNumber());
        response.put("size", comments.getSize());
        response.put("totalElements", comments.getTotalElements());
        response.put("totalPages", comments.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        postService.deleteComment(postId, commentId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Comentario eliminado exitosamente");

        return ResponseEntity.ok(response);
    }
}