package org.example.socialsvc.dto;

import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * 通用分页响应对象。
 */
@Data
public class PageResponse<T> {

    /** 当前页码（从1开始）。 */
    private Integer pageNo;

    /** 每页条数。 */
    private Integer pageSize;

    /** 总记录数。 */
    private Long total;

    /** 当前页数据列表。 */
    private List<T> records;

    public static <T> PageResponse<T> of(Integer pageNo, Integer pageSize, Long total, List<T> records) {
        PageResponse<T> page = new PageResponse<>();
        page.setPageNo(pageNo);
        page.setPageSize(pageSize);
        page.setTotal(total == null ? 0L : total);
        page.setRecords(records == null ? Collections.emptyList() : records);
        return page;
    }
}
