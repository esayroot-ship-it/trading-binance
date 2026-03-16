package org.example.tradingsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.TradePosition;

@Mapper
public interface TradePositionMapper {

    TradePosition selectById(Long id);

    List<TradePosition> selectAll();

    List<TradePosition> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") Integer status);

    TradePosition selectActiveByUserSymbolDirection(@Param("userId") Long userId,
                                                    @Param("symbol") String symbol,
                                                    @Param("direction") String direction);

    int insert(TradePosition entity);

    int updateById(TradePosition entity);

    int deleteById(Long id);
}
