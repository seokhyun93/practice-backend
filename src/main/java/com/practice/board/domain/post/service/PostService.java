package com.practice.board.domain.post.service;

import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.domain.post.entity.Post;
import com.practice.board.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
