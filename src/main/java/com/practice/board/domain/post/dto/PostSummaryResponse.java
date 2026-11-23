package com.practice.board.domain.post.dto;

import com.practice.board.domain.post.entity.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostSummaryResponse {
    private final Long id;
    private final String title;
    private final String author;
    private final LocalDateTime createdAt;
    private final long commentCount;

    public PostSummaryResponse(Post post, long commentCount) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.author = post.getAuthor();
        this.createdAt = post.getCreatedAt();
        this.commentCount = commentCount;
    }
}