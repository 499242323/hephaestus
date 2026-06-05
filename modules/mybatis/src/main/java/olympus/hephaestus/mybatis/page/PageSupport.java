package olympus.hephaestus.mybatis.page;

import java.util.List;

public final class PageSupport {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private PageSupport() {
    }

    public static PageQuery normalize(Integer page, Integer pageSize) {
        int normalizedPage = page == null || page < 1 ? DEFAULT_PAGE : page;
        int normalizedPageSize = pageSize == null || pageSize < 1
                ? DEFAULT_PAGE_SIZE
                : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageQuery(normalizedPage, normalizedPageSize);
    }

    public static PageQuery normalize(Long page, Long pageSize) {
        Integer normalizedPage = page == null ? null : Math.toIntExact(page);
        Integer normalizedPageSize = pageSize == null ? null : Math.toIntExact(pageSize);
        return normalize(normalizedPage, normalizedPageSize);
    }

    public static PageQuery fromPaging(Paging paging) {
        if (paging == null) {
            return normalize((Integer) null, null);
        }
        return normalize(paging.pageIndex(), paging.pageSize());
    }

    public static <T> PageInfo<T> pageInfo(List<T> items, long total, PageQuery query) {
        List<T> normalizedItems = items == null ? List.of() : items;
        return new PageInfo<>(normalizedItems, query.page(), query.pageSize(), total);
    }

    public static <T> Pagination<T> pagination(List<T> items, long total, PageQuery query) {
        return Pagination.of(items == null ? List.of() : items, pageInfo(items, total, query));
    }

    public static <T> Pagination<T> pagination(List<T> items, long total, Paging paging) {
        return pagination(items, total, fromPaging(paging));
    }
}
