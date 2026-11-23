package com.practice.board.domain.post.service;

import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.domain.post.dto.PostUpdateRequest;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PostServiceTest {

    @Autowired
    PostService postService;

    @Test
    void 글을_저장하고_다시_조회하면_같은_내용이_나온다() {
        PostResponse created = postService.create(new PostCreateRequest("통합 테스트 글", "내용"));

        PostResponse found = postService.findById(created.getId());

        assertThat(found.getId()).isNotNull();
        assertThat(found.getTitle()).isEqualTo("통합 테스트 글");
        assertThat(found.getContent()).isEqualTo("내용");
    }

    @Test
    void 없는_글을_조회하면_POST_NOT_FOUND_예외가_발생한다() {
        assertThatThrownBy(() -> postService.findById(-1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    /**
     * 이 테스트만 테스트용 트랜잭션을 끈다(NOT_SUPPORTED).
     * 테스트 전체를 트랜잭션으로 감싸면 Service에 @Transactional이 빠져도 테스트의 트랜잭션이 대신 일을 해서
     * "수정이 DB에 저장되지 않는 버그"를 잡지 못한다.
     * 대신 롤백이 없으므로 마지막에 만든 글을 직접 지운다.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void 글을_수정하면_다시_조회했을_때_수정된_내용이_나온다() {
        Long id = postService.create(new PostCreateRequest("원래 제목", "원래 내용")).getId();
        try {
            postService.update(id, new PostUpdateRequest("수정된 제목", "수정된 내용"));

            PostResponse found = postService.findById(id);

            assertThat(found.getTitle()).isEqualTo("수정된 제목");
            assertThat(found.getContent()).isEqualTo("수정된 내용");
        } finally {
            postService.delete(id);
        }
    }

    @Test
    void 없는_글을_수정하면_POST_NOT_FOUND_예외가_발생한다() {
        assertThatThrownBy(() -> postService.update(-1L, new PostUpdateRequest("제목", "내용")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 글을_삭제하면_다시_조회할_때_POST_NOT_FOUND_예외가_발생한다() {
        Long id = postService.create(new PostCreateRequest("삭제할 글", "내용")).getId();

        postService.delete(id);

        assertThatThrownBy(() -> postService.findById(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }
}
