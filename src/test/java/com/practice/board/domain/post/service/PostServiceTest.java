package com.practice.board.domain.post.service;

import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
}
