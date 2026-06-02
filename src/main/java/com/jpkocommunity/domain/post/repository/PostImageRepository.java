package com.jpkocommunity.domain.post.repository;

import com.jpkocommunity.domain.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByDisplayOrderAsc(Long postId);

    void deleteByPostId(Long postId);
}