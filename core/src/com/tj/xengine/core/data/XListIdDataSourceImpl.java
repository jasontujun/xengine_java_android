package com.tj.xengine.core.data;

import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.toolkit.filter.XBaseFilter;
import com.tj.xengine.core.utils.XAnnotationUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于唯一Id标识的数据源缓存。
 * Created by jasontujun.
 * Date: 11-12-17
 * Time: 上午1:01
 */
public class XListIdDataSourceImpl<T> implements XListDataSource<T>, XWithId<T> {

    private String mSourceName;
    private Field idField;

    /**
     * 实际的对象列表。
     */
    protected ArrayList<T> mItemList;

    /**
     * 数据变化监听器
     */
    protected List<XWithId.Listener<T>> mListeners;

    /**
     * 自动通知监听者
     */
    protected volatile boolean mIsAutoNotify;

    /**
     * 重复元素是否覆盖
     */
    protected volatile boolean mOverride;

    public XListIdDataSourceImpl(Class<T> clazz, String sourceName) {
        mSourceName = sourceName;
        mItemList = new ArrayList<T>();
        mListeners = new CopyOnWriteArrayList<XWithId.Listener<T>>();
        mIsAutoNotify = true;
        mOverride = false;
        idField = XAnnotationUtil.findFirstObjectField(clazz, XId.class,
                new XBaseFilter<Field>() {
                    @Override
                    public Field doFilter(Field source) {
                        // 如果@XId标注的属性，类型不是String的，则查找失败
                        return source.getType().equals(String.class) ? source : null;
                    }
                });
        if (idField == null)
            throw new IllegalArgumentException("cannot find @XId field");
    }

    @Override
    public String getId(T item) {
        if (idField != null) {
            try {
                return (String) idField.get(item);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void sort(Comparator<T> comparator) {
        Collections.sort(mItemList, comparator);
        if (mIsAutoNotify)
            notifyDataChanged();
    }

    @Override
    public T get(int index) {
        return mItemList.get(index);
    }

    @Override
    public T getById(String id) {
        int index = getIndexById(id);
        if (index != -1)
            return get(index);
        else
            return null;
    }

    @Override
    public int size() {
        return mItemList.size();
    }

    @Override
    public synchronized void add(T item) {
        if (item == null)
            return;

        int index = getIndexById(getId(item));
        if (index == -1) {
            mItemList.add(item);
            if (mIsAutoNotify)
                notifyAddItem(item);
        } else {
            if (mOverride) {
                T oldItem = mItemList.get(index);
                T newItem = replace(index, item);
                List<T> oldItems = new ArrayList<T>();
                oldItems.add(oldItem);
                List<T> newItems = new ArrayList<T>();
                newItems.add(newItem);
                if (mIsAutoNotify)
                    notifyReplaceItem(newItems, oldItems);
            }
        }
    }

    @Override
    public synchronized void addAll(List<T> items) {
        if (items == null || items.size() == 0)
            return;

        List<T> addedItems = new ArrayList<T>();
        List<T> oldItems = new ArrayList<T>();
        List<T> newItems = new ArrayList<T>();
        for (int i = 0; i<items.size(); i++) {
            T item = items.get(i);
            int index = getIndexById(getId(item));
            if (index == -1) {
                mItemList.add(item);
                addedItems.add(item);
            } else {
                if (mOverride) {
                    T oldItem = mItemList.get(index);
                    T newItem = replace(index, item);
                    oldItems.add(oldItem);
                    newItems.add(newItem);
                }
            }
        }
        if (mIsAutoNotify) {
            if (addedItems.size() > 0) {
                notifyAddItems(addedItems);
            }
            if (newItems.size() > 0) {
                notifyReplaceItem(newItems, oldItems);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return mItemList.isEmpty();
    }

    @Override
    public synchronized void deleteById(String id) {
        int index = getIndexById(id);
        if (index != -1)
            delete(index);
    }

    @Override
    public synchronized void delete(int index) {
        if (index < 0 || index >= mItemList.size())
            return;

        T item = mItemList.remove(index);
        if (mIsAutoNotify)
            notifyDeleteItem(item);
    }

    @Override
    public synchronized void delete(T item) {
        if (mItemList.remove(item)) {
            if (mIsAutoNotify)
                notifyDeleteItem(item);
        }
    }

    @Override
    public synchronized void deleteAll(List<T> items) {
        if (mItemList.removeAll(items)) {
            if (mIsAutoNotify)
                notifyDeleteItems(items);
        }
    }

    @Override
    public synchronized void deleteAllById(List<String> ids) {
        if (ids == null || ids.size() == 0)
            return;
        List<T> items = new ArrayList<T>();
        for (int i = 0; i < ids.size(); i++) {
            T item = getById(ids.get(i));
            if (item != null) {
                items.add(item);
            }
        }
        deleteAll(items);
    }

    @Override
    public void setReplaceOverride(boolean override) {
        mOverride = override;
    }

    @Override
    public int indexOf(T item) {
        return mItemList.indexOf(item);
    }

    @Override
    public boolean contains(T item) {
        if (item == null)
            return false;

        final String id = getId(item);
        for (int i = 0; i<size(); i++) {
            T tmp = get(i);
            if (getId(tmp).equals(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回数据源中所有的数据项的副本
     */
    @Override
    public List<T> copyAll() {
        return new ArrayList<T>(mItemList);
    }

    @Override
    public synchronized void clear() {
        List<T> copyItems = new ArrayList<T>(mItemList);
        mItemList.clear();
        if (mIsAutoNotify)
            notifyDeleteItems(copyItems);
    }

    @Override
    public void registerListener(XListDataSource.Listener<T> listener) {
        if (!(listener instanceof XWithId.Listener)) {
            throw new IllegalArgumentException("listener must be XWithId.Listener.");
        }
        if (!mListeners.contains(listener))
            mListeners.add((XWithId.Listener<T>) listener);
    }

    @Override
    public void unregisterListener(XListDataSource.Listener<T> listener) {
        if (!(listener instanceof XWithId.Listener)) {
            throw new IllegalArgumentException("listener must be XWithId.Listener.");
        }
        mListeners.remove(listener);
    }

    @Override
    public void notifyDataChanged() {
        for (XWithId.Listener<T> listener: mListeners) {
            listener.onChange();
        }
    }

    protected void notifyReplaceItem(List<T> newItems, List<T> oldItems) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onReplace(newItems, oldItems);
    }

    protected void notifyAddItem(T item) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onAdd(item);
    }

    protected void notifyAddItems(List<T> items) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onAddAll(items);
    }

    protected void notifyDeleteItem(T item) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onDelete(item);
    }

    protected void notifyDeleteItems(List<T> items) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onDeleteAll(items);
    }

    @Override
    public int getIndexById(String id) {
        for (int i = 0; i < size(); i++) {
            T tmp = get(i);
            if (getId(tmp).equals(id)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setAutoNotifyListeners(boolean isAuto) {
        this.mIsAutoNotify = isAuto;
    }

    @Override
    public String getSourceName() {
        return mSourceName;
    }

    /**
     * 替换某一位置的元素，此方法内负责新旧元素的合并逻辑。
     * 注意:子类可覆盖此方法来自定义合并逻辑。
     * @param index 列表中的索引位置
     * @param newItem 要替换的新元素
     * @return 返回实际添加的新元素
     */
    protected T replace(int index, T newItem) {
        mItemList.set(index, newItem);
        return newItem;
    }
}
