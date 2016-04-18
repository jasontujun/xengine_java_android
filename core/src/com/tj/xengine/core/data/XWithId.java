package com.tj.xengine.core.data;

import java.util.List;

/**
 * 基于唯一Id标识的缓存的接口。
 * Created by jasontujun.
 * Date: 12-8-23
 * Time: 下午10:54
 */
public interface XWithId<T> {

    interface Listener<T> extends XListDataSource.Listener<T> {

        /**
         * 发生数据替换时的回调。
         * 注意:该回调的newItems数据，和onAdd()以及onAddAll()回调的数据不会重复，
         * 两者同时考虑是可以完整获得数据执行add()和addAll()导致的数据变化。
         * @param newItems 替换的新数据
         * @param oldItems 被替换的旧数据
         */
        void onReplace(List<T> newItems, List<T> oldItems);
    }

    /**
     * 某一项的id
     * @param item
     * @return
     */
    String getId(T item);

    /**
     * 根据id获取一个元素
     * @param id
     * @return
     */
    T getById(String id);

    /**
     * 根据Id获取一个元素的坐标
     * @param id
     * @return
     */
    public int getIndexById(String id);

    /**
     * 根据id删除一个元素
     * @param id
     */
    void deleteById(String id);

    /**
     * 根据id删除一个元素
     * @param ids
     */
    void deleteAllById(List<String> ids);

    /**
     * 设置添加时遇到重复id元素，是否替换为新元素。
     * 默认是不允许重复元素，override为false。
     * @param override
     */
    void setReplaceOverride(boolean override);
}
