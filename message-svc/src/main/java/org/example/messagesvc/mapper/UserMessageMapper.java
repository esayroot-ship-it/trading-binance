package org.example.messagesvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.UserMessage;

@Mapper
public interface UserMessageMapper {

    UserMessage selectById(Long id);

    List<UserMessage> selectAll();

    List<UserMessage> selectByUserId(@Param("userId") Long userId);

    int insert(UserMessage entity);

    int updateById(UserMessage entity);

    int updateReadByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId, @Param("isRead") Integer isRead);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteById(Long id);
}
