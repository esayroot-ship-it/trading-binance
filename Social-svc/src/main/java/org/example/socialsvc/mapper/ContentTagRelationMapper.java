package org.example.socialsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.ContentTagRelation;

/**
 * 内容标的关联数据访问层。
 */
@Mapper
public interface ContentTagRelationMapper {

    ContentTagRelation selectById(Long id);

    List<ContentTagRelation> selectAll();

    int insert(ContentTagRelation entity);

    int updateById(ContentTagRelation entity);

    int deleteById(Long id);

    int deleteByTarget(@Param("targetType") String targetType, @Param("targetId") Long targetId);

    List<String> selectSymbolsByTarget(@Param("targetType") String targetType,
                                       @Param("targetId") Long targetId);
}
