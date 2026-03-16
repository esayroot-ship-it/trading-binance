package org.example.marketsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.UserWatchlist;

@Mapper
public interface UserWatchlistMapper {

    UserWatchlist selectById(Long id);

    List<UserWatchlist> selectAll();

    List<UserWatchlist> selectByUserId(@Param("userId") Long userId);

    UserWatchlist selectByUserIdAndSymbol(@Param("userId") Long userId,
                                          @Param("symbol") String symbol);

    Integer selectMaxSortOrderByUserId(@Param("userId") Long userId);

    int insert(UserWatchlist entity);

    int updateById(UserWatchlist entity);

    int deleteById(Long id);

    int deleteByUserIdAndSymbol(@Param("userId") Long userId,
                                @Param("symbol") String symbol);
}
