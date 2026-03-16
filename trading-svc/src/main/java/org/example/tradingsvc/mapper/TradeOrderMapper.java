package org.example.tradingsvc.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.TradeOrder;

@Mapper
public interface TradeOrderMapper {

    TradeOrder selectById(Long id);

    List<TradeOrder> selectAll();

    TradeOrder selectByOrderNo(@Param("orderNo") String orderNo);

    List<TradeOrder> selectByUserId(@Param("userId") Long userId);

    List<TradeOrder> selectPendingBySymbol(@Param("symbol") String symbol);

    int insert(TradeOrder entity);

    int updateById(TradeOrder entity);

    int updateStatusByOrderNoAndUserId(@Param("orderNo") String orderNo,
                                       @Param("userId") Long userId,
                                       @Param("fromStatus") String fromStatus,
                                       @Param("toStatus") String toStatus,
                                       @Param("updateTime") LocalDateTime updateTime);

    int updateFilledIfPending(@Param("id") Long id,
                              @Param("filledQuantity") BigDecimal filledQuantity,
                              @Param("avgFilledPrice") BigDecimal avgFilledPrice,
                              @Param("realizedPnl") BigDecimal realizedPnl,
                              @Param("updateTime") LocalDateTime updateTime);

    int deleteById(Long id);
}
