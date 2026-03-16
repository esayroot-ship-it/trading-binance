package org.example.socialsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.SysCategory;

/**
 * 分类数据访问层。
 */
@Mapper
public interface SysCategoryMapper {

    SysCategory selectById(Long id);

    List<SysCategory> selectAll();

    int insert(SysCategory entity);

    int updateById(SysCategory entity);

    int deleteById(Long id);

    List<SysCategory> selectEnabledAll();

    List<SysCategory> selectEnabledByModuleType(@Param("moduleType") String moduleType);
}
