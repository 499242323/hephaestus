package com.example.springaidemo.mybatis.page;

import java.util.List;

public final class PageInfo<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long total;

    public PageInfo(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public List<T> items() {
        return items;
    }

    public List<T> getItems() {
        return items;
    }

    public int page() {
        return page;
    }

    public int getPage() {
        return page;
    }

    public int pageSize() {
        return pageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long total() {
        return total;
    }

    public long getTotal() {
        return total;
    }

    public long totalCount() {
        return total;
    }

    public long getTotalCount() {
        return total;
    }

    public static <T> PageInfo<T> fromPaging(Paging paging, long total) {
        PageQuery query = PageSupport.normalize((int) paging.pageIndex(), (int) paging.pageSize());
        return new PageInfo<>(List.of(), query.page(), query.pageSize(), total);
    }
}
