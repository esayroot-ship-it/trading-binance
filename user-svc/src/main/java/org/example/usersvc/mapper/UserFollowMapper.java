package org.example.usersvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.DTO.TradingPlatformDTO;
import org.example.common.entity.UserFollow;

/**
 * 用户关注关系数据访问接口。
 */
@Mapper
public interface UserFollowMapper {

    /** 根据主键查询关注关系 */
    UserFollow selectById(Long id);

    /** 查询全部关注关系 */
    List<UserFollow> selectAll();

    /** 新增关注关系 */
    int insert(UserFollow entity);

    /** 根据关注者和被关注者查询关系 */
    UserFollow selectByFollowerAndFollowee(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 查询关注列表 */
    List<TradingPlatformDTO.UserFollowItem> selectFolloweesByFollowerId(@Param("followerId") Long followerId);

    /** 根据主键更新关注关系 */
    int updateById(UserFollow entity);

    /** 根据关注者和被关注者删除关系 */
    int deleteByFollowerAndFollowee(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /** 根据主键删除关注关系 */
    int deleteById(Long id);
}