package org.example.aisvc.mapper;

import java.util.List;
import org.example.common.entity.MqMessageLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * MqMessageLog 数据访问层。
 */
@Mapper
public interface MqMessageLogMapper {

    /** 根据主键查询 */
    MqMessageLog selectById(String messageId);

    /** 查询全部 */
    List<MqMessageLog> selectAll();

    /** 新增 */
    int insert(MqMessageLog entity);

    /** 根据主键更新 */
    int updateById(MqMessageLog entity);

    /** 根据主键删除 */
    int deleteById(String messageId);
}
