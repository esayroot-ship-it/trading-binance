package org.example.socialsvc.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.ContentTagRelation;
import org.example.common.entity.NewsArticle;
import org.example.common.entity.PostComment;
import org.example.common.entity.SysCategory;
import org.example.socialsvc.dto.AdminCategoryCreateRequest;
import org.example.socialsvc.dto.AdminNewsCreateRequest;
import org.example.socialsvc.mapper.ContentTagRelationMapper;
import org.example.socialsvc.mapper.NewsArticleMapper;
import org.example.socialsvc.mapper.PostCommentMapper;
import org.example.socialsvc.mapper.SysCategoryMapper;
import org.example.socialsvc.service.SocialAdminContentService;
import org.example.socialsvc.service.support.SocialDeleteCascadeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Service
@RequiredArgsConstructor
public class SocialAdminContentServiceImpl implements SocialAdminContentService {

    private static final String TYPE_NEWS = "NEWS";
    private static final String TYPE_POST = "POST";
    private static final int MAX_SYMBOL_LENGTH = 64;

    private final NewsArticleMapper newsArticleMapper;
    private final ContentTagRelationMapper contentTagRelationMapper;
    private final PostCommentMapper postCommentMapper;
    private final SysCategoryMapper sysCategoryMapper;
    private final SocialDeleteCascadeSupport socialDeleteCascadeSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> createNews(AdminNewsCreateRequest request) {
        if (request == null) {
            return R.fail("invalid params");
        }
        if (request.getCategoryId() == null || request.getCategoryId() <= 0) {
            return R.fail("invalid categoryId");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return R.fail("title is empty");
        }

        SysCategory category = sysCategoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            return R.fail("category unavailable");
        }
        if (!TYPE_NEWS.equalsIgnoreCase(category.getModuleType())) {
            return R.fail("category is not for news");
        }

        NewsArticle news = new NewsArticle();
        news.setCategoryId(request.getCategoryId());
        news.setTitle(request.getTitle().trim());
        news.setSummary(trimToNull(request.getSummary()));
        news.setAiSummary(null);
        news.setAiSentiment("NEUTRAL");
        news.setAiStatus(0);
        news.setContent(trimToNull(request.getContent()));
        news.setSource(trimToNull(request.getSource()));
        news.setImageUrl(trimToNull(request.getImageUrl()));
        news.setStatus(1);
        news.setPublishTime(request.getPublishTime() == null ? LocalDateTime.now() : request.getPublishTime());
        news.setCreateTime(LocalDateTime.now());

        int inserted = newsArticleMapper.insert(news);
        if (inserted <= 0) {
            return R.fail("create news failed");
        }

        if (news.getId() == null || news.getId() <= 0) {
            return R.fail("create news failed");
        }
        saveContentTags(TYPE_NEWS, news.getId(), request.getSymbols());
        return R.ok("create news success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteNews(Long newsId) {
        if (newsId == null || newsId <= 0) {
            return R.fail("invalid params");
        }
        boolean deleted = socialDeleteCascadeSupport.cascadeDeleteNewsByAdmin(newsId);
        if (!deleted) {
            return R.fail("news delete failed");
        }
        return R.ok("delete success");
    }

    @Override
    public R<List<SysCategory>> listCategories() {
        return R.ok("success", sysCategoryMapper.selectAll());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> createCategory(AdminCategoryCreateRequest request) {
        if (request == null) {
            return R.fail("invalid params");
        }
        if (!StringUtils.hasText(request.getCategoryName())) {
            return R.fail("categoryName is empty");
        }
        String safeType = normalizeContentType(request.getModuleType());
        if (safeType == null) {
            return R.fail("invalid moduleType");
        }

        SysCategory category = new SysCategory();
        category.setCategoryName(request.getCategoryName().trim());
        category.setModuleType(safeType);
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        category.setCreateTime(LocalDateTime.now());

        int inserted = sysCategoryMapper.insert(category);
        if (inserted <= 0) {
            return R.fail("create category failed");
        }
        return R.ok("create category success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return R.fail("invalid params");
        }
        int deleted = sysCategoryMapper.deleteById(categoryId);
        if (deleted <= 0) {
            return R.fail("delete category failed");
        }
        return R.ok("delete success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deletePost(Long postId) {
        if (postId == null || postId <= 0) {
            return R.fail("invalid params");
        }
        boolean deleted = socialDeleteCascadeSupport.cascadeDeletePostByAdmin(postId);
        if (!deleted) {
            return R.fail("delete post failed");
        }
        return R.ok("delete success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteComment(Long commentId) {
        if (commentId == null || commentId <= 0) {
            return R.fail("invalid params");
        }

        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == null || comment.getIsDeleted() != 0) {
            return R.fail("comment unavailable");
        }

        int deletedCount = socialDeleteCascadeSupport.cascadeDeleteCommentTree(commentId);
        if (deletedCount <= 0) {
            return R.fail("delete comment failed");
        }
        socialDeleteCascadeSupport.decrementPostCommentCount(comment.getPostId(), deletedCount);
        return R.ok("delete success");
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        String type = contentType.trim().toUpperCase(Locale.ROOT);
        if (TYPE_NEWS.equals(type) || TYPE_POST.equals(type)) {
            return type;
        }
        return null;
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private void saveContentTags(String targetType, Long targetId, List<String> symbols) {
        if (!StringUtils.hasText(targetType) || targetId == null || targetId <= 0 || symbols == null || symbols.isEmpty()) {
            return;
        }

        Set<String> normalizedSymbols = new LinkedHashSet<>();
        for (String symbol : symbols) {
            if (!StringUtils.hasText(symbol)) {
                continue;
            }
            String normalized = symbol.trim().toUpperCase(Locale.ROOT);
            if (normalized.length() > MAX_SYMBOL_LENGTH) {
                normalized = normalized.substring(0, MAX_SYMBOL_LENGTH);
            }
            if (StringUtils.hasText(normalized)) {
                normalizedSymbols.add(normalized);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        for (String symbol : normalizedSymbols) {
            ContentTagRelation relation = new ContentTagRelation();
            relation.setTargetType(targetType);
            relation.setTargetId(targetId);
            relation.setSymbol(symbol);
            relation.setCreateTime(now);
            contentTagRelationMapper.insert(relation);
        }
    }
}
