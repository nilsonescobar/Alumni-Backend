package org.fia.alumni.alumnifiauesbackend.service.post;

import lombok.RequiredArgsConstructor;
import org.fia.alumni.alumnifiauesbackend.dto.request.post.CommentRequest;
import org.fia.alumni.alumnifiauesbackend.dto.request.post.PostRequest;
import org.fia.alumni.alumnifiauesbackend.dto.request.post.ReactionRequest;
import org.fia.alumni.alumnifiauesbackend.dto.response.post.CommentResponse;
import org.fia.alumni.alumnifiauesbackend.dto.response.post.PostResponse;
import org.fia.alumni.alumnifiauesbackend.entity.post.*;
import org.fia.alumni.alumnifiauesbackend.entity.profile.Profile;
import org.fia.alumni.alumnifiauesbackend.entity.user.User;
import org.fia.alumni.alumnifiauesbackend.exception.BadRequestException;
import org.fia.alumni.alumnifiauesbackend.exception.ResourceNotFoundException;
import org.fia.alumni.alumnifiauesbackend.repository.post.CommentRepository;
import org.fia.alumni.alumnifiauesbackend.repository.post.PostReactionRepository;
import org.fia.alumni.alumnifiauesbackend.repository.post.PostRepository;
import org.fia.alumni.alumnifiauesbackend.repository.post.TagRepository;
import org.fia.alumni.alumnifiauesbackend.repository.profile.ProfileRepository;
import org.fia.alumni.alumnifiauesbackend.repository.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostReactionRepository postReactionRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional
    public PostResponse createPost(PostRequest request, Long userId) {
        User user = findUserById(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .build();

        if (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) {
            List<PostMedia> mediaList = new ArrayList<>();
            int order = 0;
            for (String url : request.getMediaUrls()) {
                PostMedia media = PostMedia.builder()
                        .post(post)
                        .mediaUrl(url)
                        .mediaType("image")
                        .displayOrder(order++)
                        .build();
                mediaList.add(media);
            }
            post.setMedia(mediaList);
        }

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            List<Tag> tags = request.getTags().stream()
                    .map(this::findOrCreateTag)
                    .toList();
            post.setTags(tags);
        }

        Post savedPost = postRepository.save(post);
        return mapToResponse(savedPost, userId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getPublicFeed(Pageable pageable, Long currentUserId) {
        Page<Post> posts = postRepository.findPublicFeed(pageable);
        return mapToResponsePage(posts, currentUserId);
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable, Long currentUserId) {
        Page<Post> posts = postRepository.findByUserId(userId, pageable);
        return mapToResponsePage(posts, currentUserId);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));

        return mapToResponse(post, currentUserId);
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));

        if (!post.getUser().getId().equals(userId)) {
            throw new BadRequestException("No tienes permiso para editar esta publicación");
        }

        post.setContent(request.getContent());
        if (request.getIsPublic() != null) {
            post.setIsPublic(request.getIsPublic());
        }

        Post updatedPost = postRepository.save(post);
        return mapToResponse(updatedPost, userId);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));

        if (!post.getUser().getId().equals(userId)) {
            throw new BadRequestException("No tienes permiso para eliminar esta publicación");
        }

        post.softDelete();
        postRepository.save(post);
    }

    @Transactional
    public void addReaction(Long postId, ReactionRequest request, Long userId) {
        User user = findUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            PostReaction reaction = existing.get();
            reaction.setReactionType(request.reactionType());
            postReactionRepository.save(reaction);
        } else {
            PostReaction reaction = PostReaction.builder()
                    .post(post)
                    .user(user)
                    .reactionType(request.reactionType())
                    .build();
            postReactionRepository.save(reaction);
        }
    }

    @Transactional
    public void removeReaction(Long postId, Long userId) {
        PostReaction reaction = postReactionRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reacción no encontrada"));

        postReactionRepository.delete(reaction);
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request, Long userId) {
        User user = findUserById(userId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Publicación no encontrada"));

        Comment comment = Comment.builder()
                .post(post)
                .user(user)
                .content(request.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);
        return mapCommentToResponse(savedComment);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getComments(Long postId, Pageable pageable) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Publicación no encontrada");
        }

        Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
        return comments.map(this::mapCommentToResponse);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        if (!comment.getPost().getId().equals(postId)) {
            throw new BadRequestException("El comentario no pertenece a esta publicación");
        }

        if (!comment.getUser().getId().equals(userId)) {
            throw new BadRequestException("No tienes permiso para eliminar este comentario");
        }

        comment.softDelete();
        commentRepository.save(comment);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Tag findOrCreateTag(String tagName) {
        String normalizedName = tagName.toLowerCase().trim();
        return tagRepository.findByName(normalizedName)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(normalizedName).build()));
    }

    private Page<PostResponse> mapToResponsePage(Page<Post> posts, Long currentUserId) {
        Page<PostResponse> responsePage = posts.map(post -> mapToResponse(post, null));

        if (responsePage.hasContent() && currentUserId != null) {
            Set<Long> postIds = responsePage.getContent().stream()
                    .map(PostResponse::getId)
                    .collect(Collectors.toSet());

            Map<Long, PostReaction> userReactions = postReactionRepository.findUserReactionsForPosts(currentUserId, postIds);

            responsePage.getContent().forEach(response -> {
                PostReaction reaction = userReactions.get(response.getId());
                if (reaction != null) {
                    response.setCurrentUserReaction(reaction.getReactionType());
                }
            });
        }

        return responsePage;
    }

    private PostResponse mapToResponse(Post post, Long currentUserId) {
        Profile profile = profileRepository.findById(post.getUser().getId()).orElse(null);

        PostResponse.UserSummary userSummary = PostResponse.UserSummary.builder()
                .id(post.getUser().getId())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .profilePicture(profile != null ? profile.getProfilePicture() : null)
                .build();

        List<PostResponse.MediaInfo> mediaInfo = post.getMedia().stream()
                .map(media -> PostResponse.MediaInfo.builder()
                        .id(media.getId())
                        .url(media.getMediaUrl())
                        .type(media.getMediaType())
                        .displayOrder(media.getDisplayOrder())
                        .build())
                .toList();

        List<String> tags = post.getTags().stream()
                .map(Tag::getName)
                .toList();

        Long reactionCount = postReactionRepository.countByPostId(post.getId());
        Long commentCount = commentRepository.countByPostIdAndDeletedAtIsNull(post.getId());

        PostReaction.ReactionType currentUserReaction = null;
        if (currentUserId != null) {
            currentUserReaction = postReactionRepository.findByPostIdAndUserId(post.getId(), currentUserId)
                    .map(PostReaction::getReactionType)
                    .orElse(null);
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .isPublic(post.getIsPublic())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .user(userSummary)
                .media(mediaInfo)
                .tags(tags)
                .reactionCount(reactionCount)
                .commentCount(commentCount)
                .currentUserReaction(currentUserReaction)
                .build();
    }

    private CommentResponse mapCommentToResponse(Comment comment) {
        Profile profile = profileRepository.findById(comment.getUser().getId()).orElse(null);

        CommentResponse.UserSummary userSummary = CommentResponse.UserSummary.builder()
                .id(comment.getUser().getId())
                .firstName(profile != null ? profile.getFirstName() : "")
                .lastName(profile != null ? profile.getLastName() : "")
                .profilePicture(profile != null ? profile.getProfilePicture() : null)
                .build();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .user(userSummary)
                .build();
    }
}