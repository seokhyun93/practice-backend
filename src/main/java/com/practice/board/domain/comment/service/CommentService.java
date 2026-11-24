package com.practice.board.domain.comment.service;

import com.practice.board.domain.comment.dto.CommentCreateRequest;
import com.practice.board.domain.comment.dto.CommentResponse;
import com.practice.board.domain.comment.dto.RecentCommentResponse;
import com.practice.board.domain.comment.entity.Comment;
import com.practice.board.domain.comment.repository.CommentRepository;
import com.practice.board.domain.post.entity.Post;
import com.practice.board.domain.post.repository.PostRepository;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Transactional
    public CommentResponse create(Long postId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        Comment comment = new Comment(post, request.getContent(), request.getAuthor());
        return new CommentResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> findAllByPost(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return commentRepository.findAllByPostIdOrderByIdAsc(postId).stream()
                .map(CommentResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecentCommentResponse> findRecent() {
        return commentRepository.findRecentWithPost().stream()
                .map(RecentCommentResponse::new)
                .toList();
    }
}
