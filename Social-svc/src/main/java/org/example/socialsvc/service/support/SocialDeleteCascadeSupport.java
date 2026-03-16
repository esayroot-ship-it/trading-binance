package org.example.socialsvc.service.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.socialsvc.mapper.ContentTagRelationMapper;
import org.example.socialsvc.mapper.NewsArticleMapper;
import org.example.socialsvc.mapper.PostCommentMapper;
import org.example.socialsvc.mapper.PostInteractionMapper;
import org.example.socialsvc.mapper.PostMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialDeleteCascadeSupport {

    private static final String TARGET_TYPE_POST = "POST";
    private static final String TARGET_TYPE_NEWS = "NEWS";

    private final PostMapper postMapper;
    private final NewsArticleMapper newsArticleMapper;
    private final PostCommentMapper postCommentMapper;
    private final PostInteractionMapper postInteractionMapper;
    private final ContentTagRelationMapper contentTagRelationMapper;

    public boolean cascadeDeletePostByAdmin(Long postId) {
        int updated = postMapper.softDeleteById(postId);
        if (updated <= 0) {
            return false;
        }
        clearPostRelations(postId);
        return true;
    }

    public boolean cascadeDeletePostByOwner(Long postId, Long userId) {
        int updated = postMapper.softDeleteByIdAndUserId(postId, userId);
        if (updated <= 0) {
            return false;
        }
        clearPostRelations(postId);
        return true;
    }

    public boolean cascadeDeleteNewsByAdmin(Long newsId) {
        int deleted = newsArticleMapper.deleteById(newsId);
        if (deleted <= 0) {
            return false;
        }
        contentTagRelationMapper.deleteByTarget(TARGET_TYPE_NEWS, newsId);
        return true;
    }

    public int cascadeDeleteCommentTree(Long rootCommentId) {
        List<Long> treeIds = collectCommentTreeIds(rootCommentId);
        if (treeIds.isEmpty()) {
            return 0;
        }
        int deletedCount = postCommentMapper.countNotDeletedByIds(treeIds);
        postCommentMapper.softDeleteByIds(treeIds);
        return deletedCount;
    }

    public void decrementPostCommentCount(Long postId, int delta) {
        if (postId == null || postId <= 0 || delta <= 0) {
            return;
        }
        postMapper.decrementCommentCount(postId, delta);
    }

    private void clearPostRelations(Long postId) {
        postCommentMapper.softDeleteByPostId(postId);
        postInteractionMapper.deleteByPostId(postId);
        contentTagRelationMapper.deleteByTarget(TARGET_TYPE_POST, postId);
    }

    private List<Long> collectCommentTreeIds(Long rootCommentId) {
        if (rootCommentId == null || rootCommentId <= 0) {
            return new ArrayList<>();
        }

        Set<Long> allIds = new LinkedHashSet<>();
        List<Long> currentLevel = new ArrayList<>();
        currentLevel.add(rootCommentId);
        allIds.add(rootCommentId);

        while (!currentLevel.isEmpty()) {
            List<Long> childIds = postCommentMapper.selectChildIdsByParentIds(currentLevel);
            List<Long> nextLevel = new ArrayList<>();
            if (childIds != null) {
                for (Long childId : childIds) {
                    if (childId != null && allIds.add(childId)) {
                        nextLevel.add(childId);
                    }
                }
            }
            currentLevel = nextLevel;
        }

        return new ArrayList<>(allIds);
    }
}
