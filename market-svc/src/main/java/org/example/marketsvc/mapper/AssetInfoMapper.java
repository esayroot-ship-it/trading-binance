package org.example.marketsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.AssetInfo;

@Mapper
public interface AssetInfoMapper {

    AssetInfo selectById(Long id);

    List<AssetInfo> selectAll();

    List<AssetInfo> selectByAssetTypeAndStatus(@Param("assetType") String assetType,
                                               @Param("status") Integer status);

    AssetInfo selectBySymbol(@Param("symbol") String symbol);

    int insert(AssetInfo entity);

    int updateById(AssetInfo entity);

    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);

    int deleteById(Long id);
}