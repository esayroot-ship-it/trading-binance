package org.example.socialsvc.service;

import java.util.List;
import org.example.common.entity.PostComment;
import org.example.common.entity.SysCategory;
import org.example.socialsvc.dto.ContentListItem;
import org.example.socialsvc.dto.PageResponse;
import org.example.socialsvc.dto.UserPostCreateRequest;
import tools.R;

/**
 * Social user-facing content service.
 */
public interface SocialUserContentService {

    R<PageResponse<ContentListItem>> pageContent(String contentType, Long categoryId, Integer pageNo, Integer pageSize);

    R<List<SysCategory>> listCategories(String moduleType);

    R<Void> createPost(Long userId, UserPostCreateRequest request);

    R<List<String>> listSymbols(String targetType, Long targetId);

    R<List<PostComment>> listPostComments(Long postId);

    R<Void> interactPost(Long userId, Long postId, Integer actionType);

    R<Void> commentPost(Long userId, Long postId, Long parentId, String content);

    R<Void> deletePost(Long userId, Long postId);

    R<Void> deleteComment(Long userId, Long commentId);
}
