package olympus.hephaestus.mybatis.page;

public final class PageQuery {

    private final int page;
    private final int pageSize;

    public PageQuery(int page, int pageSize) {
        this.page = page;
        this.pageSize = pageSize;
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

    public int offset() {
        return (page - 1) * pageSize;
    }

    public int getOffset() {
        return offset();
    }

    public int limit() {
        return pageSize;
    }

    public int getLimit() {
        return limit();
    }

    public static PageQuery of(int page, int pageSize) {
        return new PageQuery(page, pageSize);
    }
}
