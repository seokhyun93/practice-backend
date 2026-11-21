package com.practice.board.domain.post.dto;

import com.practice.board.domain.post.entity.Post;
import lombok.Getter;

@Getter
public class PostResponse {
    private Long id;
    private String title;
    private String content;

    public PostResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
    }
}
