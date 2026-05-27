package com.jpkocommunity.domain.post.repository;

import com.jpkocommunity.domain.post.entity.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {

    void deleteByPostId(Long postId);
}