package org.example.usersvc.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.User;

@Mapper
public interface UserMapper {

    User selectById(Long id);

    User selectByUsername(String username);

    List<User> selectAll();

    List<User> selectPage(@Param("offset") int offset, @Param("size") int size);

    long countAll();

    int insert(User entity);

    int updateById(User entity);

    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("updateTime") LocalDateTime updateTime);

    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname, @Param("updateTime") LocalDateTime updateTime);

    int updateEmail(@Param("id") Long id, @Param("email") String email, @Param("updateTime") LocalDateTime updateTime);

    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar, @Param("updateTime") LocalDateTime updateTime);

    int updateStatus(@Param("id") Long id, @Param("status") Integer status, @Param("updateTime") LocalDateTime updateTime);

    int updateDeleted(@Param("id") Long id, @Param("isDeleted") Integer isDeleted, @Param("status") Integer status,
                      @Param("updateTime") LocalDateTime updateTime);

    int deleteById(Long id);
}
