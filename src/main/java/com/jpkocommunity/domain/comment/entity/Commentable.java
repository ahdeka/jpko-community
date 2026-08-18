package com.jpkocommunity.domain.comment.entity;

import com.jpkocommunity.domain.user.entity.User;

public interface Commentable {
    Long getId();
    User getAuthor();
    boolean isCommentActive();
    CommentTargetType getTargetType();
}
