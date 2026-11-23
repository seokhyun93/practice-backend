package com.practice.board.domain.post.service;

import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.dto.PostResponse;
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
public class PostService {
    private final PostRepository postRepository;

    @Transactional
    public PostResponse create(PostCreateRequest request) {
        Post post = new Post(request.getTitle(), request.getContent());
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
    public List<PostResponse> findAll() {
        return postRepository.findAll().stream().map(PostResponse::new).toList();
    }
}
