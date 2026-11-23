package com.practice.board.domain.post.controller;

import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.domain.post.entity.Post;
import com.practice.board.domain.post.service.PostService;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostService postService;

    @Test
    void 글을_작성하면_201과_작성된_글을_반환한다() throws Exception {
        // Given: 가짜 Service가 이 글을 돌려주도록 설정
        given(postService.create(any())).willReturn(new PostResponse(new Post("첫 글", "안녕하세요")));

        // When: POST /posts 요청
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"첫 글\",\"content\":\"안녕하세요\"}"))
                // Then: 201 + 응답의 title 확인
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("첫 글"));
    }

    @Test
    void 제목이_없으면_400과_에러_메시지를_반환한다() throws Exception {
        // When: 제목 없이 요청
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\"}"))
                // Then: 400 + 우리가 만든 에러 형식
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("제목은 필수입니다."));

        // Then: 검증에 실패했으니 Service는 한 번도 호출되면 안 됨
        verify(postService, never()).create(any());
    }

    @Test
    void 글을_조회하면_200과_글을_반환한다() throws Exception {
        given(postService.findById(1L)).willReturn(new PostResponse(new Post("첫 글", "안녕하세요")));

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("첫 글"));
    }

    @Test
    void 없는_글을_조회하면_404와_POST_NOT_FOUND를_반환한다() throws Exception {
        given(postService.findById(99L)).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 글_목록을_조회하면_배열로_반환한다() throws Exception {
        given(postService.findAll()).willReturn(List.of(
                new PostResponse(new Post("첫 글", "내용1")),
                new PostResponse(new Post("둘째 글", "내용2"))));

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].title").value("둘째 글"));
    }
}
