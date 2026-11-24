package com.practice.board.domain.comment.repository;

import com.practice.board.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findAllByPostIdOrderByIdAsc(Long postId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.post.id = :postId")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Query("select c.post.id as postId, count(c) as count from Comment c " +
            "where c.post.id in :postIds group by c.post.id")
    List<CommentCount> countByPostIds(@Param("postIds") List<Long> postIds);

    @Query("select c from Comment c join fetch c.post order by c.id desc limit 10")
    List<Comment> findRecentWithPost();
}
