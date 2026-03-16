package org.example.tradingsvc.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.UserAccount;

@Mapper
public interface UserAccountMapper {

    UserAccount selectById(Long id);

    List<UserAccount> selectAll();

    UserAccount selectByUserIdAndCurrency(@Param("userId") Long userId, @Param("currency") String currency);

    int insert(UserAccount entity);

    int updateById(UserAccount entity);

    int updateByIdAndVersion(@Param("id") Long id,
                             @Param("version") Integer version,
                             @Param("newVersion") Integer newVersion,
                             @Param("balance") BigDecimal balance,
                             @Param("frozenBalance") BigDecimal frozenBalance,
                             @Param("updateTime") LocalDateTime updateTime);

    int deleteById(Long id);
}
