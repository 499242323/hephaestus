package olympus.hephaestus.mybatis.page;

import java.util.List;

public final class Pagination<T> {

    private final List<T> list;
    private final PageInfo<T> pageInfo;

    public Pagination(List<T> list, PageInfo<T> pageInfo) {
        this.list = list;
        this.pageInfo = pageInfo;
    }

    public List<T> getList() {
        return list;
    }

    public List<T> getItems() {
        return list;
    }

    public PageInfo<T> getPageInfo() {
        return pageInfo;
    }

    public int getPage() {
        return pageInfo == null ? 1 : pageInfo.getPage();
    }

    public int getPageSize() {
        return pageInfo == null ? 0 : pageInfo.getPageSize();
    }

    public long getTotal() {
        return pageInfo == null ? 0L : pageInfo.getTotal();
    }

    public static <T> Pagination<T> of(List<T> list, PageInfo<T> pageInfo) {
        return new Pagination<>(list, pageInfo);
    }
}
