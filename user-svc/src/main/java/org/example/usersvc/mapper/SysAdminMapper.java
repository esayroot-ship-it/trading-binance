package org.example.usersvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.example.common.entity.SysAdmin;

/**
 * 管理员数据访问接口。
 */
@Mapper
public interface SysAdminMapper {

    /** 根据主键查询管理员 */
    SysAdmin selectById(Integer id);

    /** 根据用户名查询管理员 */
    SysAdmin selectByUsername(String username);

    /** 查询全部管理员 */
    List<SysAdmin> selectAll();

    /** 新增管理员 */
    int insert(SysAdmin entity);

    /** 根据主键更新管理员 */
    int updateById(SysAdmin entity);

    /** 根据主键删除管理员 */
    int deleteById(Integer id);
}
