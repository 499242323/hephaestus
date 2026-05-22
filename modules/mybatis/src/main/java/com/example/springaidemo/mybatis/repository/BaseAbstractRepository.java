package com.example.springaidemo.mybatis.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

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
}
