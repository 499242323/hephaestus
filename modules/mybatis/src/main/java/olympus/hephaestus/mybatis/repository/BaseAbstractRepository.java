package olympus.hephaestus.mybatis.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import olympus.hephaestus.mybatis.page.PageInfo;
import olympus.hephaestus.mybatis.page.PageQuery;
import olympus.hephaestus.mybatis.page.PageSupport;
import olympus.hephaestus.mybatis.page.Pagination;
import olympus.hephaestus.mybatis.page.Paging;
import org.apache.ibatis.annotations.UpdateProvider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public interface BaseAbstractRepository<T, ID extends Serializable> extends BaseMapper<T> {

    default T getById(ID id) {
        return selectById(id);
    }

    default T save(T entity) {
        insert(entity);
        return entity;
    }

    @UpdateProvider(type = BaseUpdateTemplate.class, method = "updateById")
    void updateNonNullById(T entity);

    default T update(T entity) {
        updateNonNullById(entity);
        return entity;
    }

    default void removeById(ID id) {
        deleteById(id);
    }

    default void insertList(Iterable<? extends T> entities) {
        if (entities == null) {
            return;
        }
        Iterator<? extends T> iterator = entities.iterator();
        if (!iterator.hasNext()) {
            return;
        }
        for (T entity : entities) {
            insert(entity);
        }
    }

    default void insertListBatchLimit(List<? extends T> entities, int batchSize) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        if (batchSize <= 0 || entities.size() <= batchSize) {
            insertList(entities);
            return;
        }

        for (int start = 0; start < entities.size(); start += batchSize) {
            int end = Math.min(start + batchSize, entities.size());
            insertList(new ArrayList<>(entities.subList(start, end)));
        }
    }

    default <R> PageInfo<R> toPageInfo(List<R> items, long total, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageSupport.normalize(page, pageSize);
        return PageSupport.pageInfo(items, total, pageQuery);
    }

    default <R> PageInfo<R> toPageInfo(List<R> items, long total, Paging paging) {
        return PageSupport.pageInfo(items, total, PageSupport.fromPaging(paging));
    }

    default <R> Pagination<R> toPagination(List<R> items, long total, Integer page, Integer pageSize) {
        PageQuery pageQuery = PageSupport.normalize(page, pageSize);
        return PageSupport.pagination(items, total, pageQuery);
    }

    default <R> Pagination<R> toPagination(List<R> items, long total, Paging paging) {
        return PageSupport.pagination(items, total, paging);
    }
}
