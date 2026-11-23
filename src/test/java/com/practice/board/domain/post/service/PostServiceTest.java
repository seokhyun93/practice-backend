package com.practice.board.domain.post.service;

import com.practice.board.domain.comment.dto.CommentCreateRequest;
import com.practice.board.domain.comment.repository.CommentRepository;
import com.practice.board.domain.comment.service.CommentService;
import com.practice.board.domain.post.dto.PostCreateRequest;
import com.practice.board.domain.post.dto.PostResponse;
import com.practice.board.domain.post.dto.PostSummaryResponse;
import com.practice.board.domain.post.dto.PostUpdateRequest;
import com.practice.board.global.exception.BusinessException;
import com.practice.board.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class PostServiceTest {

    @Autowired
    PostService postService;

    @Autowired
    CommentService commentService;

    @Autowired
    CommentRepository commentRepository;

    @Test
    void 글을_저장하고_다시_조회하면_같은_내용이_나온다() {
        PostResponse created = postService.create(new PostCreateRequest("통합 테스트 글", "내용", "석현"));

        PostResponse found = postService.findById(created.getId());

        assertThat(found.getId()).isNotNull();
        assertThat(found.getTitle()).isEqualTo("통합 테스트 글");
        assertThat(found.getContent()).isEqualTo("내용");
        assertThat(found.getAuthor()).isEqualTo("석현");
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
        PostResponse created = postService.create(new PostCreateRequest("원래 제목", "원래 내용", "석현"));
        Long id = created.getId();
        try {
            PostResponse updated = postService.update(id, new PostUpdateRequest("수정된 제목", "수정된 내용"));

            PostResponse found = postService.findById(id);

            assertThat(found.getTitle()).isEqualTo("수정된 제목");
            assertThat(found.getContent()).isEqualTo("수정된 내용");

            // 작성일은 그대로, 수정일만 바뀌어야 한다
            assertThat(found.getCreatedAt()).isEqualTo(created.getCreatedAt());
            assertThat(found.getUpdatedAt()).isAfter(created.getUpdatedAt());
            // 수정 API의 응답에도 새 수정일이 담겨야 한다 (flush 누락 버그 방지)
            assertThat(updated.getUpdatedAt()).isEqualTo(found.getUpdatedAt());
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
        Long id = postService.create(new PostCreateRequest("삭제할 글", "내용", "석현")).getId();

        postService.delete(id);

        assertThatThrownBy(() -> postService.findById(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 댓글이_있는_글을_삭제하면_댓글도_함께_삭제된다() {
        Long id = postService.create(new PostCreateRequest("댓글 있는 글", "내용", "석현")).getId();
        commentService.create(id, new CommentCreateRequest("첫 댓글", "민수"));
        commentService.create(id, new CommentCreateRequest("둘째 댓글", "지영"));

        // 댓글을 먼저 지우지 않으면 외래 키 때문에 여기서 예외가 난다
        postService.delete(id);

        assertThat(commentRepository.findAllByPostIdOrderByIdAsc(id)).isEmpty();
        assertThatThrownBy(() -> postService.findById(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    void 최신순으로_페이지를_조회하면_가장_최근에_쓴_글이_먼저_나온다() {
        postService.create(new PostCreateRequest("페이지 테스트 1", "내용", "석현"));
        Long second = postService.create(new PostCreateRequest("페이지 테스트 2", "내용", "석현")).getId();
        Long third = postService.create(new PostCreateRequest("페이지 테스트 3", "내용", "석현")).getId();

        Page<PostSummaryResponse> page = postService.findAll(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")));

        // DB에 원래 있던 글과 상관없이, 방금 쓴 글이 가장 최신이다
        assertThat(page.getContent()).extracting("id").containsExactly(third, second);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void 목록을_조회하면_글마다_댓글_수가_담기고_댓글이_없는_글은_0이다() {
        Long noComment = postService.create(new PostCreateRequest("댓글 없는 글", "내용", "석현")).getId();
        Long twoComments = postService.create(new PostCreateRequest("댓글 2개 글", "내용", "석현")).getId();
        commentService.create(twoComments, new CommentCreateRequest("첫 댓글", "민수"));
        commentService.create(twoComments, new CommentCreateRequest("둘째 댓글", "지영"));

        Page<PostSummaryResponse> page = postService.findAll(PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")));

        // 최신순이므로 [댓글 2개 글, 댓글 없는 글]
        assertThat(page.getContent()).extracting("id").containsExactly(twoComments, noComment);
        assertThat(page.getContent()).extracting("commentCount").containsExactly(2L, 0L);
    }

    @Test
    void 글이_없는_페이지를_조회하면_빈_목록이_나온다() {
        // id 목록이 비어 "in ()"가 되는 경우에도 에러가 나지 않아야 한다
        Page<PostSummaryResponse> page = postService.findAll(PageRequest.of(10_000, 10));

        assertThat(page.getContent()).isEmpty();
    }
}
