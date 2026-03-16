package org.example.socialsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.SysCategory;
import org.example.socialsvc.dto.AdminCategoryCreateRequest;
import org.example.socialsvc.dto.AdminNewsCreateRequest;
import org.example.socialsvc.service.SocialAdminContentService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 管理员内容管理接口。
 */
@RestController
@RequestMapping("/api/social/admin")
@RequiredArgsConstructor
public class SocialAdminContentController {

    private final SocialAdminContentService socialAdminContentService;

    /**
     * 创建新闻。
     */
    @PostMapping("/news")
    public R<Void> createNews(@RequestBody AdminNewsCreateRequest request) {
        return socialAdminContentService.createNews(request);
    }

    /**
     * 删除新闻。
     */
    @DeleteMapping("/news/{newsId}")
    public R<Void> deleteNews(@PathVariable Long newsId) {
        return socialAdminContentService.deleteNews(newsId);
    }

    /**
     * 列出所有分类。
     */
    @GetMapping("/categories")
    public R<List<SysCategory>> listCategories() {
        return socialAdminContentService.listCategories();
    }

    /**
     * 创建分类。
     */
    @PostMapping("/categories")
    public R<Void> createCategory(@RequestBody AdminCategoryCreateRequest request) {
        return socialAdminContentService.createCategory(request);
    }

    /**
     * 删除分类。
     */
    @DeleteMapping("/categories/{categoryId}")
    public R<Void> deleteCategory(@PathVariable Long categoryId) {
        return socialAdminContentService.deleteCategory(categoryId);
    }

    /**
     * 删除帖子。
     */
    @DeleteMapping("/posts/{postId}")
    public R<Void> deletePost(@PathVariable Long postId) {
        return socialAdminContentService.deletePost(postId);
    }

    /**
     * 删除评论（并级联删除该评论下的回复）。
     */
    @DeleteMapping("/comments/{commentId}")
    public R<Void> deleteComment(@PathVariable Long commentId) {
        return socialAdminContentService.deleteComment(commentId);
    }
}
