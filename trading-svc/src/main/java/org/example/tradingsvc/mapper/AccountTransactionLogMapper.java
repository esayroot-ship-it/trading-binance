package org.example.tradingsvc.mapper;

import java.util.List;
import org.example.common.entity.AccountTransactionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * AccountTransactionLog 数据访问层。
 */
@Mapper
public interface AccountTransactionLogMapper {

    /** 根据主键查询 */
    AccountTransactionLog selectById(Long id);

    /** 查询全部 */
    List<AccountTransactionLog> selectAll();

    /** 新增 */
    int insert(AccountTransactionLog entity);

    /** 根据主键更新 */
    int updateById(AccountTransactionLog entity);

    /** 根据主键删除 */
    int deleteById(Long id);
}
