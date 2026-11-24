package com.practice.board.domain.comment.service;

import com.practice.board.domain.comment.dto.CommentCreateRequest;
import com.practice.board.domain.comment.dto.CommentResponse;
import com.practice.board.domain.comment.dto.RecentCommentResponse;
import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.service.PostService;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class CommentServiceTest {

    @Autowired
    CommentService commentService;

    @Autowired
    PostService postService;

    @Test
    void 댓글을_달고_목록을_조회하면_작성한_순서대로_나온다() {
        Long postId = postService.create(new PostCreateRequest("글", "내용", "석현")).getId();

        commentService.create(postId, new CommentCreateRequest("첫 댓글", "민수"));
        commentService.create(postId, new CommentCreateRequest("둘째 댓글", "지영"));

        List<CommentResponse> comments = commentService.findAllByPost(postId);

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getContent()).isEqualTo("첫 댓글");
        assertThat(comments.get(1).getContent()).isEqualTo("둘째 댓글");
        assertThat(comments.get(0).getPostId()).isEqualTo(postId);
    }

    @Test
    void 다른_글의_댓글은_목록에_섞이지_않는다() {
        Long postA = postService.create(new PostCreateRequest("글 A", "내용", "석현")).getId();
        Long postB = postService.create(new PostCreateRequest("글 B", "내용", "석현")).getId();
        commentService.create(postA, new CommentCreateRequest("A의 댓글", "민수"));
        commentService.create(postB, new CommentCreateRequest("B의 댓글", "지영"));

        List<CommentResponse> comments = commentService.findAllByPost(postA);

        assertThat(comments).extracting("content").containsExactly("A의 댓글");
    }

    @Test
    void 없는_글에_댓글을_달면_POST_NOT_FOUND_예외가_발생한다() {
        assertThatThrownBy(() -> commentService.create(-1L, new CommentCreateRequest("유령 댓글", "민수")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 없는_글의_댓글_목록을_조회하면_POST_NOT_FOUND_예외가_발생한다() {
        assertThatThrownBy(() -> commentService.findAllByPost(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 최근_댓글은_최신순으로_글_제목과_함께_나온다() {
        Long postA = postService.create(new PostCreateRequest("글 A", "내용", "석현")).getId();
        Long postB = postService.create(new PostCreateRequest("글 B", "내용", "석현")).getId();
        commentService.create(postA, new CommentCreateRequest("A의 댓글", "민수"));
        commentService.create(postB, new CommentCreateRequest("B의 댓글", "지영"));

        List<RecentCommentResponse> recent = commentService.findRecent();

        // DB에 원래 있던 댓글과 상관없이, 방금 단 댓글이 가장 최신이다
        assertThat(recent.get(0).getContent()).isEqualTo("B의 댓글");
        assertThat(recent.get(0).getPostId()).isEqualTo(postB);
        assertThat(recent.get(0).getPostTitle()).isEqualTo("글 B");
        assertThat(recent.get(1).getContent()).isEqualTo("A의 댓글");
        assertThat(recent.get(1).getPostTitle()).isEqualTo("글 A");
    }

    @Test
    void 최근_댓글은_최대_10개까지만_나온다() {
        Long postId = postService.create(new PostCreateRequest("글", "내용", "석현")).getId();
        for (int i = 1; i <= 11; i++) {
            commentService.create(postId, new CommentCreateRequest("댓글 " + i, "민수"));
        }

        List<RecentCommentResponse> recent = commentService.findRecent();

        assertThat(recent).hasSize(10);
        assertThat(recent.get(0).getContent()).isEqualTo("댓글 11");
        assertThat(recent.get(9).getContent()).isEqualTo("댓글 2");
    }
}
