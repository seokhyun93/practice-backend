package com.practice.board.domain.comment.dto;

import com.practice.board.domain.comment.entity.Comment;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecentCommentResponse {
    private final Long id;
    private final String content;
    private final String author;
    private final Long postId;
    private final String postTitle;
    private final LocalDateTime createdAt;

    public RecentCommentResponse(Comment comment) {
        this.id = comment.getId();
        this.content = comment.getContent();
        this.author = comment.getAuthor();
        this.postId = comment.getPost().getId();
        this.postTitle = comment.getPost().getTitle();
        this.createdAt = comment.getCreatedAt();
    }
}
