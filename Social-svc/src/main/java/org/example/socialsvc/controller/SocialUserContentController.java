package org.example.socialsvc.controller;

import lombok.RequiredArgsConstructor;
import org.example.common.entity.PostComment;
import org.example.common.entity.SysCategory;
import org.example.socialsvc.dto.ContentListItem;
import org.example.socialsvc.dto.PageResponse;
import org.example.socialsvc.dto.PostCommentCreateRequest;
import org.example.socialsvc.dto.UserPostCreateRequest;
import org.example.socialsvc.service.SocialUserContentService;
import org.springframework.web.bind.annotation.*;
import tools.R;

import java.util.List;

/**
 * 社交用户内容控制器
 */
@RestController
@RequestMapping("/api/social")
@RequiredArgsConstructor
public class SocialUserContentController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final SocialUserContentService socialUserContentService;

    /**
     * 获取内容列表
     *
     * @param contentType 内容类型
     * @param categoryId  分类ID
     * @param pageNo      页码
     * @param pageSize    每页数量
     * @return 内容列表
     */
    @GetMapping("/content/list")
    public R<PageResponse<ContentListItem>> pageContent(
            @RequestParam String contentType,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return socialUserContentService.pageContent(contentType, categoryId, pageNo, pageSize);
    }

    /**
     * 获取分类列表
     *
     * @param moduleType 模块类型
     * @return 分类列表
     */
    @GetMapping("/categories")
    public R<List<SysCategory>> listCategories(@RequestParam(required = false) String moduleType) {
        return socialUserContentService.listCategories(moduleType);
    }

    /**
     * 创建帖子
     *
     * @param userId   用户编号
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/posts")
    public R<Void> createPost(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestBody UserPostCreateRequest request) {
        return socialUserContentService.createPost(userId, request);
    }

    /**
     * 获取内容标签
     *
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 标签列表
     */
    @GetMapping("/content-tags")
    public R<List<String>> listSymbols(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        return socialUserContentService.listSymbols(targetType, targetId);
    }

    /**
     * 获取帖子评论列表
     *
     * @param postId 帖子编号
     * @return 评论列表
     */
    @GetMapping("/posts/{postId}/comments")
    public R<List<PostComment>> listPostComments(@PathVariable Long postId) {
        return socialUserContentService.listPostComments(postId);
    }

    /**
     * 帖子互动
     *
     * @param userId     用户编号
     * @param postId     帖子编号
     * @param actionType 动作类型
     * @return 互动结果
     */
    @PostMapping("/posts/{postId}/interactions")
    public R<Void> interactPost(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long postId,
            @RequestParam Integer actionType) {
        return socialUserContentService.interactPost(userId, postId, actionType);
    }

    /**
     * 创建帖子评论
     *
     * @param userId     用户编号
     * @param postId     帖子编号
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/posts/{postId}/comments")
    public R<Void> commentPost(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long postId,
            @RequestBody PostCommentCreateRequest request) {
        Long parentId = request == null ? 0L : request.getParentId();
        String content = request == null ? null : request.getContent();
        return socialUserContentService.commentPost(userId, postId, parentId, content);
    }

    /**
     * 用户删除自己的帖子（并级联删除帖子相关评论/互动/标签）。
     */
    @DeleteMapping("/posts/{postId}")
    public R<Void> deletePost(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long postId) {
        return socialUserContentService.deletePost(userId, postId);
    }

    /**
     * 用户删除自己的评论（并级联删除该评论下的回复）。
     */
    @DeleteMapping("/comments/{commentId}")
    public R<Void> deleteComment(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long commentId) {
        return socialUserContentService.deleteComment(userId, commentId);
    }
}
