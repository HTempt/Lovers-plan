package com.lovers.service;

import java.util.Optional;

/**
 * 基础服务接口（所有 Service 的公共抽象）
 */
public interface IBaseService<T> {

    /**
     * 根据 ID 查询
     */
    Optional<T> findById(Long id);

    /**
     * 保存实体
     */
    T saveEntity(T entity);

    /**
     * 根据 ID 删除
     */
    void deleteEntity(Long id);
}
