package com.practice.board.domain.post.service;

import com.practice.board.domain.comment.repository.CommentCount;
import com.practice.board.domain.comment.repository.CommentRepository;
import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.domain.post.dto.PostSummaryResponse;
import com.practice.board.domain.post.dto.PostUpdateRequest;
import com.practice.board.domain.post.entity.Post;
import com.practice.board.domain.post.repository.PostRepository;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        Post post = new Post(request.getTitle(), request.getContent(), request.getAuthor());
        Post saved = postRepository.save(post);
        return new PostResponse(saved);
    }

    @Transactional(readOnly = true)
    public PostResponse findById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        return new PostResponse(post);
    }

    @Transactional(readOnly = true)
    public Page<PostSummaryResponse> findAll(Pageable pageable) {
        // 1) 글 한 페이지 조회 (SQL: 목록 1번 + 전체 개수 1번)
        Page<Post> posts = postRepository.findAll(pageable);

        // 2) 이번 페이지 글들의 id만 모으기: [80, 79, ..., 71]
        List<Long> postIds = posts.getContent().stream()
                .map(Post::getId)
                .toList();

        // 3) 댓글 수를 한 번에 세고, 찾기 쉽게 Map으로: {80=3, 79=3, 78=3}
        Map<Long, Long> commentCounts = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(CommentCount::getPostId, CommentCount::getCount));

        // 4) 글마다 Map에서 꺼내기. 댓글 없는 글은 Map에 없으니 0
        return posts.map(post -> new PostSummaryResponse(post, commentCounts.getOrDefault(post.getId(), 0L)));
    }

    @Transactional
    public PostResponse update(Long id, PostUpdateRequest request) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        post.update(request.getTitle(), request.getContent());
        postRepository.flush();
        return new PostResponse(post);
    }

    @Transactional
    public void delete(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        commentRepository.deleteAllByPostId(id);
        postRepository.delete(post);
    }
}
