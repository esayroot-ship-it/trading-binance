package org.example.socialsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.PostInteraction;

/**
 * 帖子互动数据访问层。
 */
@Mapper
public interface PostInteractionMapper {

    PostInteraction selectById(Long id);

    List<PostInteraction> selectAll();

    int insert(PostInteraction entity);

    int updateById(PostInteraction entity);

    int deleteById(Long id);

    int deleteByPostId(@Param("postId") Long postId);

    PostInteraction selectByPostIdAndUserIdAndActionType(@Param("postId") Long postId,
                                                          @Param("userId") Long userId,
                                                          @Param("actionType") Integer actionType);
}
