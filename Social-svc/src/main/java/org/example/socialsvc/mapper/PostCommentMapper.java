package org.example.socialsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.PostComment;

/**
 * 帖子评论数据访问层。
 */
@Mapper
public interface PostCommentMapper {

    PostComment selectById(Long id);

    List<PostComment> selectAll();

    List<PostComment> selectVisibleByPostId(@Param("postId") Long postId);

    int insert(PostComment entity);

    int updateById(PostComment entity);

    int deleteById(Long id);

    int softDeleteByPostId(@Param("postId") Long postId);

    int softDeleteByIds(@Param("ids") List<Long> ids);

    int countNotDeletedByIds(@Param("ids") List<Long> ids);

    List<Long> selectChildIdsByParentIds(@Param("parentIds") List<Long> parentIds);
}
