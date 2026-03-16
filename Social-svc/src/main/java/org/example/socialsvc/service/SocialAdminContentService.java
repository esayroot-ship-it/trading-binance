package org.example.socialsvc.service;

import java.util.List;
import org.example.common.entity.SysCategory;
import org.example.socialsvc.dto.AdminCategoryCreateRequest;
import org.example.socialsvc.dto.AdminNewsCreateRequest;
import tools.R;

/**
 * Social admin-facing content service.
 */
public interface SocialAdminContentService {

    R<Void> createNews(AdminNewsCreateRequest request);

    R<Void> deleteNews(Long newsId);

    R<List<SysCategory>> listCategories();

    R<Void> createCategory(AdminCategoryCreateRequest request);

    R<Void> deleteCategory(Long categoryId);

    R<Void> deletePost(Long postId);

    R<Void> deleteComment(Long commentId);
}
