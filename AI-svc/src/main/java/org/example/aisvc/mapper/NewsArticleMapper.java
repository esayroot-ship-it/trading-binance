package org.example.aisvc.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.common.entity.NewsArticle;

@Mapper
public interface NewsArticleMapper {

    NewsArticle selectVisibleById(Long id);
}
