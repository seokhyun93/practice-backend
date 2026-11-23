package com.practice.board.domain.post.controller;

import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.domain.post.entity.Post;
import com.practice.board.domain.post.service.PostService;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        given(postService.create(any())).willReturn(new PostResponse(new Post("첫 글", "안녕하세요", "석현")));

        // When: POST /posts 요청
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"첫 글\",\"content\":\"안녕하세요\",\"author\":\"석현\"}"))
                // Then: 201 + 응답의 title 확인
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("첫 글"))
                .andExpect(jsonPath("$.author").value("석현"));
    }

    @Test
    void 제목이_없으면_400과_에러_메시지를_반환한다() throws Exception {
        // When: 제목 없이 요청
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"안녕하세요\",\"author\":\"석현\"}"))
                // Then: 400 + 우리가 만든 에러 형식
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("제목은 필수입니다."));

        // Then: 검증에 실패했으니 Service는 한 번도 호출되면 안 됨
        verify(postService, never()).create(any());
    }

    @Test
    void 작성자가_없으면_400과_에러_메시지를_반환한다() throws Exception {
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("작성자는 필수입니다."));

        verify(postService, never()).create(any());
    }

    @Test
    void 글을_조회하면_200과_글을_반환한다() throws Exception {
        given(postService.findById(1L)).willReturn(new PostResponse(new Post("첫 글", "안녕하세요", "석현")));

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
    void 글_목록을_조회하면_페이지_형식으로_반환한다() throws Exception {
        given(postService.findAll(any(Pageable.class))).willReturn(new PageImpl<>(
                List.of(new PostResponse(new Post("첫 글", "내용1", "석현")),
                        new PostResponse(new Post("둘째 글", "내용2", "석현"))),
                PageRequest.of(0, 2),
                5));

        mockMvc.perform(get("/posts").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[1].title").value("둘째 글"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true))
                // Spring 내부 구조(PageImpl)가 그대로 노출되지 않아야 한다
                .andExpect(jsonPath("$.pageable").doesNotExist());
    }

    @Test
    void 페이지_파라미터가_없으면_첫_페이지_10개_최신순으로_조회한다() throws Exception {
        given(postService.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/posts")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).findAll(captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void 페이지_크기는_최대_50으로_제한된다() throws Exception {
        given(postService.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/posts").param("size", "1000")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postService).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void 글을_수정하면_200과_수정된_글을_반환한다() throws Exception {
        given(postService.update(eq(1L), any())).willReturn(new PostResponse(new Post("수정된 제목", "수정된 내용", "석현")));

        mockMvc.perform(put("/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정된 제목\",\"content\":\"수정된 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"));
    }

    @Test
    void 없는_글을_수정하면_404를_반환한다() throws Exception {
        given(postService.update(eq(99L), any())).willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND));

        mockMvc.perform(put("/posts/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    @Test
    void 수정할_제목이_비어_있으면_400을_반환하고_Service를_호출하지_않는다() throws Exception {
        mockMvc.perform(put("/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("제목은 필수입니다."));

        verify(postService, never()).update(any(), any());
    }

    @Test
    void 글을_삭제하면_204와_빈_본문을_반환한다() throws Exception {
        mockMvc.perform(delete("/posts/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(postService).delete(1L);
    }

    @Test
    void 없는_글을_삭제하면_404를_반환한다() throws Exception {
        willThrow(new BusinessException(ErrorCode.POST_NOT_FOUND)).given(postService).delete(99L);

        mockMvc.perform(delete("/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }
}
