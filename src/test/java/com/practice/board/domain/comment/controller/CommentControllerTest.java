package com.practice.board.domain.comment.controller;

import com.practice.board.domain.comment.dto.CommentResponse;
import com.practice.board.domain.comment.dto.RecentCommentResponse;
import com.practice.board.domain.comment.entity.Comment;
import com.practice.board.domain.comment.service.CommentService;
import com.practice.board.domain.post.entity.Post;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommentService commentService;

    private CommentResponse response(String content, String author) {
        Post post = new Post("글 제목", "글 내용", "석현");
        return new CommentResponse(new Comment(post, content, author));
    }

    @Test
    void 댓글을_달면_201과_작성된_댓글을_반환한다() throws Exception {
        given(commentService.create(eq(1L), any())).willReturn(response("좋은 글이네요", "민수"));

        mockMvc.perform(post("/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"좋은 글이네요\",\"author\":\"민수\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("좋은 글이네요"))
                .andExpect(jsonPath("$.author").value("민수"));
    }

    @Test
    void 없는_글에_댓글을_달면_404를_반환한다() throws Exception {
        given(commentService.create(eq(999L), any())).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(post("/posts/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"유령 댓글\",\"author\":\"민수\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 댓글_내용이_비어_있으면_400을_반환하고_Service를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\",\"author\":\"민수\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("댓글 내용은 필수입니다."));

        verify(commentService, never()).create(any(), any());
    }

    @Test
    void 댓글_목록을_조회하면_배열로_반환한다() throws Exception {
        given(commentService.findAllByPost(1L)).willReturn(List.of(
                response("첫 댓글", "민수"),
                response("둘째 댓글", "지영")));

        mockMvc.perform(get("/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].author").value("지영"));
    }

    @Test
    void 없는_글의_댓글_목록을_조회하면_404를_반환한다() throws Exception {
        given(commentService.findAllByPost(999L)).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/posts/999/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 최근_댓글을_조회하면_글_제목과_함께_배열로_반환한다() throws Exception {
        given(commentService.findRecent()).willReturn(List.of(
                new RecentCommentResponse(new Comment(new Post("스프링 질문", "내용", "석현"), "저도 궁금해요", "민수")),
                new RecentCommentResponse(new Comment(new Post("점심 메뉴", "내용", "지영"), "김치찌개", "석현"))));

        mockMvc.perform(get("/comments/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("저도 궁금해요"))
                .andExpect(jsonPath("$[0].postTitle").value("스프링 질문"))
                .andExpect(jsonPath("$[1].postTitle").value("점심 메뉴"));
    }
}
