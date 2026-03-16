package org.example.socialsvc.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.common.entity.ContentTagRelation;
import org.example.common.entity.NewsArticle;
import org.example.common.entity.Post;
import org.example.common.entity.PostComment;
import org.example.common.entity.PostInteraction;
import org.example.common.entity.SysCategory;
import org.example.socialsvc.dto.ContentListItem;
import org.example.socialsvc.dto.PageResponse;
import org.example.socialsvc.dto.UserPostCreateRequest;
import org.example.socialsvc.mapper.ContentTagRelationMapper;
import org.example.socialsvc.mapper.NewsArticleMapper;
import org.example.socialsvc.mapper.PostCommentMapper;
import org.example.socialsvc.mapper.PostInteractionMapper;
import org.example.socialsvc.mapper.PostMapper;
import org.example.socialsvc.mapper.SysCategoryMapper;
import org.example.socialsvc.mq.SocialCommentNotifyProducer;
import org.example.socialsvc.service.SocialUserContentService;
import org.example.socialsvc.service.support.SocialDeleteCascadeSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.R;

@Service
@RequiredArgsConstructor
public class SocialUserContentServiceImpl implements SocialUserContentService {

    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_SYMBOL_LENGTH = 64;

    private static final String TYPE_NEWS = "NEWS";
    private static final String TYPE_POST = "POST";

    private final NewsArticleMapper newsArticleMapper;
    private final PostMapper postMapper;
    private final SysCategoryMapper sysCategoryMapper;
    private final ContentTagRelationMapper contentTagRelationMapper;
    private final PostInteractionMapper postInteractionMapper;
    private final PostCommentMapper postCommentMapper;
    private final SocialCommentNotifyProducer socialCommentNotifyProducer;
    private final SocialDeleteCascadeSupport socialDeleteCascadeSupport;

    @Override
    public R<PageResponse<ContentListItem>> pageContent(String contentType, Long categoryId, Integer pageNo, Integer pageSize) {
        String safeType = normalizeContentType(contentType);
        if (safeType == null) {
            return R.fail("invalid contentType");
        }

        int safePageNo = normalizePageNo(pageNo);
        int safePageSize = normalizePageSize(pageSize);
        int offset = (safePageNo - 1) * safePageSize;

        if (TYPE_NEWS.equals(safeType)) {
            long total;
            List<NewsArticle> records;
            if (categoryId == null || categoryId <= 0) {
                total = newsArticleMapper.countVisible();
                records = newsArticleMapper.selectPage(offset, safePageSize);
            } else {
                total = newsArticleMapper.countVisibleByCategory(categoryId);
                records = newsArticleMapper.selectPageByCategory(categoryId, offset, safePageSize);
            }
            List<ContentListItem> items = records.stream().map(this::convertNews).collect(Collectors.toList());
            return R.ok("success", PageResponse.of(safePageNo, safePageSize, total, items));
        }

        long total;
        List<Post> records;
        if (categoryId == null || categoryId <= 0) {
            total = postMapper.countVisible();
            records = postMapper.selectPage(offset, safePageSize);
        } else {
            total = postMapper.countVisibleByCategory(categoryId);
            records = postMapper.selectPageByCategory(categoryId, offset, safePageSize);
        }
        List<ContentListItem> items = records.stream().map(this::convertPost).collect(Collectors.toList());
        return R.ok("success", PageResponse.of(safePageNo, safePageSize, total, items));
    }

    @Override
    public R<List<SysCategory>> listCategories(String moduleType) {
        List<SysCategory> categories;
        if (StringUtils.hasText(moduleType)) {
            String safeType = normalizeContentType(moduleType);
            if (safeType == null) {
                return R.fail("invalid moduleType");
            }
            categories = sysCategoryMapper.selectEnabledByModuleType(safeType);
        } else {
            categories = sysCategoryMapper.selectEnabledAll();
        }
        return R.ok("success", categories);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> createPost(Long userId, UserPostCreateRequest request) {
        if (userId == null || userId <= 0 || request == null) {
            return R.fail("invalid params");
        }
        if (request.getCategoryId() == null || request.getCategoryId() <= 0) {
            return R.fail("invalid categoryId");
        }
        if (!StringUtils.hasText(request.getContent())) {
            return R.fail("post content is empty");
        }

        SysCategory category = sysCategoryMapper.selectById(request.getCategoryId());
        if (category == null || category.getStatus() == null || category.getStatus() != 1) {
            return R.fail("category unavailable");
        }
        if (!TYPE_POST.equalsIgnoreCase(category.getModuleType())) {
            return R.fail("category is not for post");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setCategoryId(request.getCategoryId());
        post.setTitle(trimToNull(request.getTitle()));
        post.setContent(request.getContent().trim());
        post.setImages(trimToNull(request.getImages()));
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1);
        post.setIsDeleted(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());

        int inserted = postMapper.insert(post);
        if (inserted <= 0) {
            return R.fail("create post failed");
        }
        if (post.getId() == null || post.getId() <= 0) {
            return R.fail("create post failed");
        }
        saveContentTags(TYPE_POST, post.getId(), request.getSymbols());
        return R.ok("create post success");
    }

    @Override
    public R<List<String>> listSymbols(String targetType, Long targetId) {
        String safeType = normalizeContentType(targetType);
        if (safeType == null || targetId == null || targetId <= 0) {
            return R.fail("invalid params");
        }
        return R.ok("success", contentTagRelationMapper.selectSymbolsByTarget(safeType, targetId));
    }

    @Override
    public R<List<PostComment>> listPostComments(Long postId) {
        if (postId == null || postId <= 0) {
            return R.fail("invalid postId");
        }
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1
                || post.getIsDeleted() == null || post.getIsDeleted() != 0) {
            return R.fail("post unavailable");
        }
        return R.ok("success", postCommentMapper.selectVisibleByPostId(postId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> interactPost(Long userId, Long postId, Integer actionType) {
        if (userId == null || userId <= 0 || postId == null || postId <= 0) {
            return R.fail("invalid params");
        }
        if (actionType == null || (actionType != 1 && actionType != 2)) {
            return R.fail("invalid actionType");
        }

        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1
                || post.getIsDeleted() == null || post.getIsDeleted() != 0) {
            return R.fail("post unavailable");
        }

        PostInteraction exists =
                postInteractionMapper.selectByPostIdAndUserIdAndActionType(postId, userId, actionType);
        if (exists != null) {
            return R.ok("success");
        }

        PostInteraction interaction = new PostInteraction();
        interaction.setPostId(postId);
        interaction.setUserId(userId);
        interaction.setActionType(actionType);
        interaction.setCreateTime(LocalDateTime.now());
        int inserted = postInteractionMapper.insert(interaction);
        if (inserted <= 0) {
            return R.fail("interact failed");
        }

        if (actionType == 1) {
            postMapper.incrementLikeCount(postId);
        }
        return R.ok("success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> commentPost(Long userId, Long postId, Long parentId, String content) {
        if (userId == null || userId <= 0 || postId == null || postId <= 0) {
            return R.fail("invalid params");
        }
        if (!StringUtils.hasText(content)) {
            return R.fail("comment content is empty");
        }

        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == null || post.getStatus() != 1
                || post.getIsDeleted() == null || post.getIsDeleted() != 0) {
            return R.fail("post unavailable");
        }

        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId == null || parentId < 0 ? 0L : parentId);
        comment.setContent(content.trim());
        comment.setStatus(1);
        comment.setIsDeleted(0);
        comment.setCreateTime(LocalDateTime.now());
        int inserted = postCommentMapper.insert(comment);
        if (inserted <= 0) {
            return R.fail("comment failed");
        }

        postMapper.incrementCommentCount(postId);
        if (!userId.equals(post.getUserId())) {
            socialCommentNotifyProducer.sendPostCommentNotify(postId, post.getUserId(), userId, content);
        }
        return R.ok("comment success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deletePost(Long userId, Long postId) {
        if (userId == null || userId <= 0 || postId == null || postId <= 0) {
            return R.fail("invalid params");
        }

        Post post = postMapper.selectById(postId);
        if (post == null || post.getIsDeleted() == null || post.getIsDeleted() != 0) {
            return R.fail("post unavailable");
        }
        if (!userId.equals(post.getUserId())) {
            return R.fail("no permission to delete this post");
        }

        boolean deleted = socialDeleteCascadeSupport.cascadeDeletePostByOwner(postId, userId);
        if (!deleted) {
            return R.fail("delete post failed");
        }
        return R.ok("delete success");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> deleteComment(Long userId, Long commentId) {
        if (userId == null || userId <= 0 || commentId == null || commentId <= 0) {
            return R.fail("invalid params");
        }

        PostComment comment = postCommentMapper.selectById(commentId);
        if (comment == null || comment.getIsDeleted() == null || comment.getIsDeleted() != 0) {
            return R.fail("comment unavailable");
        }
        if (!userId.equals(comment.getUserId())) {
            return R.fail("no permission to delete this comment");
        }

        int deletedCount = socialDeleteCascadeSupport.cascadeDeleteCommentTree(commentId);
        if (deletedCount <= 0) {
            return R.fail("delete comment failed");
        }
        socialDeleteCascadeSupport.decrementPostCommentCount(comment.getPostId(), deletedCount);
        return R.ok("delete success");
    }

    private ContentListItem convertNews(NewsArticle news) {
        ContentListItem item = new ContentListItem();
        item.setId(news.getId());
        item.setContentType(TYPE_NEWS);
        item.setCategoryId(news.getCategoryId());
        item.setTitle(news.getTitle());
        item.setSummary(news.getSummary());
        item.setContent(news.getContent());
        item.setImageUrl(news.getImageUrl());
        item.setSource(news.getSource());
        item.setPublishTime(news.getPublishTime());
        item.setCreateTime(news.getCreateTime());
        return item;
    }

    private ContentListItem convertPost(Post post) {
        ContentListItem item = new ContentListItem();
        item.setId(post.getId());
        item.setContentType(TYPE_POST);
        item.setCategoryId(post.getCategoryId());
        item.setUserId(post.getUserId());
        item.setTitle(post.getTitle());
        item.setContent(post.getContent());
        item.setImages(post.getImages());
        item.setViewCount(post.getViewCount());
        item.setLikeCount(post.getLikeCount());
        item.setCommentCount(post.getCommentCount());
        item.setCreateTime(post.getCreateTime());
        return item;
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo <= 0 ? DEFAULT_PAGE_NO : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
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
