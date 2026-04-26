package org.fia.alumni.alumnifiauesbackend.controller.storage;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.entity.post.Post;
import org.fia.alumni.alumnifiauesbackend.entity.post.PostMedia;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.post.PostRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.service.storage.AzureBlobService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final AzureBlobService blobService;
    private final ProfileRepository profileRepository;
    private final PostRepository postRepository;

    @PutMapping("/profile/picture")
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        String newUrl = blobService.replaceImage(file, profile.getProfilePicture(), "profiles");
        profile.setProfilePicture(newUrl);
        profileRepository.save(profile);

        return ok("Foto de perfil actualizada", Map.of("profilePicture", newUrl));
    }

    /**
     * DELETE /api/v1/storage/profile/picture
     * Elimina la foto de perfil del usuario autenticado.
     */
    @DeleteMapping("/profile/picture")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteProfilePicture(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();

        Profile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado"));

        if (profile.getProfilePicture() == null || profile.getProfilePicture().isBlank()) {
            throw new BadRequestException("No tienes foto de perfil");
        }

        blobService.deleteByUrl(profile.getProfilePicture());
        profile.setProfilePicture(null);
        profileRepository.save(profile);

        return ok("Foto de perfil eliminada", null);
    }

    // ═══════════════════════════════════════════════════════════════
    // POST MEDIA
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/storage/posts/{postId}/media
     * Sube una o varias imágenes a un post existente.
     * Form-data: files[] (imágenes)
     */
    @PostMapping("/posts/{postId}/media")
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadPostMedia(
            @PathVariable Long postId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Post post = findOwnPost(postId, userId);

        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Debes enviar al menos un archivo");
        }
        if (files.size() > 5) {
            throw new BadRequestException("Máximo 5 imágenes por post");
        }

        List<String> uploadedUrls = new ArrayList<>();
        int nextOrder = post.getMedia() != null ? post.getMedia().size() : 0;

        for (int i = 0; i < files.size(); i++) {
            String url = blobService.uploadImage(files.get(i), "posts/" + postId);
            uploadedUrls.add(url);

            PostMedia media = PostMedia.builder()
                    .post(post)
                    .mediaUrl(url)
                    .mediaType(files.get(i).getContentType())
                    .displayOrder(nextOrder + i)
                    .build();

            if (post.getMedia() == null) post.setMedia(new ArrayList<>());
            post.getMedia().add(media);
        }

        postRepository.save(post);

        return ok("Imágenes subidas", Map.of("urls", uploadedUrls));
    }

    /**
     * DELETE /api/v1/storage/posts/{postId}/media/{mediaId}
     * Elimina una imagen específica de un post.
     */
    @DeleteMapping("/posts/{postId}/media/{mediaId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deletePostMedia(
            @PathVariable Long postId,
            @PathVariable Long mediaId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Post post = findOwnPost(postId, userId);

        PostMedia toDelete = post.getMedia().stream()
                .filter(m -> m.getId().equals(mediaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Media no encontrado en este post"));

        blobService.deleteByUrl(toDelete.getMediaUrl());
        post.getMedia().remove(toDelete);

        // Reordenar displayOrder
        for (int i = 0; i < post.getMedia().size(); i++) {
            post.getMedia().get(i).setDisplayOrder(i);
        }

        postRepository.save(post);

        return ok("Imagen eliminada", null);
    }

    /**
     * DELETE /api/v1/storage/posts/{postId}/media
     * Elimina TODAS las imágenes de un post.
     */
    @DeleteMapping("/posts/{postId}/media")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAllPostMedia(
            @PathVariable Long postId,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Post post = findOwnPost(postId, userId);

        if (post.getMedia() != null && !post.getMedia().isEmpty()) {
            List<String> urls = post.getMedia().stream()
                    .map(PostMedia::getMediaUrl).toList();
            blobService.deleteAll(urls);
            post.getMedia().clear();
            postRepository.save(post);
        }

        return ok("Imágenes eliminadas", null);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPLOAD GENÉRICO (para usar desde el frontend antes de crear post)
    // ═══════════════════════════════════════════════════════════════

    /**
     * POST /api/v1/storage/upload
     * Sube una imagen a una carpeta genérica y retorna la URL.
     * Útil para previsualización antes de guardar en BD.
     * Form-data: file, folder (opcional, default "general")
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder,
            Authentication authentication
    ) {
        // Sanitizar folder — solo letras, números y guiones
        String safeFolder = folder.replaceAll("[^a-zA-Z0-9_-]", "").toLowerCase();
        if (safeFolder.isBlank()) safeFolder = "general";

        String url = blobService.uploadImage(file, safeFolder);
        return ok("Imagen subida", Map.of("url", url));
    }

    /**
     * DELETE /api/v1/storage/delete
     * Elimina una imagen por URL (para cleanup de uploads huérfanos).
     * Body: { "url": "https://..." }
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteByUrl(
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            throw new BadRequestException("La URL es requerida");
        }
        blobService.deleteByUrl(url);
        return ok("Imagen eliminada", null);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════

    private Post findOwnPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post no encontrado"));
        if (!post.getUser().getId().equals(userId)) {
            throw new BadRequestException("No tienes permiso para modificar este post");
        }
        return post;
    }

    private ResponseEntity<Map<String, Object>> ok(String message, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", message);
        if (data != null) r.put("data", data);
        return ResponseEntity.ok(r);
    }
}