package com.jpkocommunity.domain.post.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByDisplayOrderAsc(Long postId);

    void deleteByPostId(Long postId);

    // 주어진 게시글 목록 중 이미지가 있는 게시글 id만 반환
    @Query("SELECT DISTINCT pi.post.id FROM PostImage pi WHERE pi.post.id IN :postIds")
    List<Long> findPostIdsHavingImages(@Param("postIds") List<Long> postIds);
}