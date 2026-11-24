package com.practice.board.domain.comment.controller;

import com.practice.board.domain.comment.dto.CommentCreateRequest;
import com.practice.board.domain.comment.dto.CommentResponse;
import com.practice.board.domain.comment.dto.RecentCommentResponse;
import com.practice.board.domain.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> create(@PathVariable Long postId,
                                                  @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.create(postId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/posts/{postId}/comments")
    public List<CommentResponse> findAll(@PathVariable Long postId) {
        return commentService.findAllByPost(postId);
    }

    @GetMapping("/comments/recent")
    public List<RecentCommentResponse> findRecent() {
        return commentService.findRecent();
    }
}

