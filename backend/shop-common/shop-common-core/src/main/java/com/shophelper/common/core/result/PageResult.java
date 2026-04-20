package com.shophelper.common.core.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应结构
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;
    private long total;
    private int pageNum;
    private int pageSize;
    private boolean hasNext;

    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setHasNext((long) pageNum * pageSize < total);
        return result;
    }
}
