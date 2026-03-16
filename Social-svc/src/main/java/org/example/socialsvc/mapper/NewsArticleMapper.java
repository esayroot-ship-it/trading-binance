package org.example.socialsvc.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.common.entity.NewsArticle;

/**
 * 新闻数据访问层。
 */
@Mapper
public interface NewsArticleMapper {

    NewsArticle selectById(Long id);

    List<NewsArticle> selectAll();

    int insert(NewsArticle entity);

    int updateById(NewsArticle entity);

    int deleteById(Long id);

    List<NewsArticle> selectPage(@Param("offset") int offset, @Param("size") int size);

    long countVisible();

    List<NewsArticle> selectPageByCategory(@Param("categoryId") Long categoryId,
                                           @Param("offset") int offset,
                                           @Param("size") int size);

    long countVisibleByCategory(@Param("categoryId") Long categoryId);
}
