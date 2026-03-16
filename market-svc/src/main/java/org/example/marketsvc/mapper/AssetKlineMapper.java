package org.example.marketsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.AssetKline;

@Mapper
public interface AssetKlineMapper {

    AssetKline selectById(Long id);

    List<AssetKline> selectAll();

    AssetKline selectBySymbolAndTimeframe(@Param("symbol") String symbol,
                                          @Param("timeframe") String timeframe);

    int insert(AssetKline entity);

    int updateById(AssetKline entity);

    int upsertBySymbolAndTimeframe(AssetKline entity);

    int deleteById(Long id);
}