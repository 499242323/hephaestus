package com.example.springaidemo.mybatis.page;

public final class Paging {

    private final long pageIndex;
    private final long pageSize;

    public Paging(long pageIndex, long pageSize) {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    public long pageIndex() {
        return pageIndex;
    }

    public long pageSize() {
        return pageSize;
    }
}
