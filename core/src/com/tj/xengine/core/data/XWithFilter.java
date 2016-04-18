package com.tj.xengine.core.data;

import com.tj.xengine.core.toolkit.filter.XFilter;

import java.util.Comparator;

/**
 * 带过滤功能的缓存接口。
 * 该接口配合XListDataSource使用
 * Created by jasontujun.
 * Date: 12-3-30
 * Time: 下午2:30
 * @see XListDataSource<T>
 */
public interface XWithFilter<T> {

    void registerListenerForOrigin(XListDataSource.Listener<T> listener);

    void unregisterListenerForOrigin(XListDataSource.Listener<T> listener);

    /**
     * 设置过滤器(同时会执行一遍过滤器)。
     * @see #doFilter()
     */
    void setFilter(XFilter<T> filter);

    XFilter<T> getFilter();

    /**
     * 执行一遍过滤器。
     * Origin相关方法返回的是原始数据，而XListDataSource相关的接口返回的是过滤后的结构。
     * 过滤后，不会影响Origin相关方法的结果，只会影响XListDataSource相关的接口返回结果。
     */
    void doFilter();

    T getOrigin(int i);

    int sizeOrigin();

    void sortOrigin(Comparator<T> comparator);
}
