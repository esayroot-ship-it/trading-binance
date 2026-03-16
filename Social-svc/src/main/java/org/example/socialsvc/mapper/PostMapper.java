package org.example.socialsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.Post;

/**
 * 帖子数据访问层。
 */
@Mapper
public interface PostMapper {

    Post selectById(Long id);

    List<Post> selectAll();

    int insert(Post entity);

    int updateById(Post entity);

    int deleteById(Long id);

    List<Post> selectPage(@Param("offset") int offset, @Param("size") int size);

    long countVisible();

    List<Post> selectPageByCategory(@Param("categoryId") Long categoryId,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    long countVisibleByCategory(@Param("categoryId") Long categoryId);

    int incrementLikeCount(@Param("postId") Long postId);

    int incrementCommentCount(@Param("postId") Long postId);

    int softDeleteById(@Param("postId") Long postId);

    int softDeleteByIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    int decrementCommentCount(@Param("postId") Long postId, @Param("delta") Integer delta);
}
