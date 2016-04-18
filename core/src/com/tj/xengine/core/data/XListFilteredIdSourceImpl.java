package com.tj.xengine.core.data;

import com.tj.xengine.core.data.annotation.XId;
import com.tj.xengine.core.toolkit.filter.XBaseFilter;
import com.tj.xengine.core.toolkit.filter.XFilter;
import com.tj.xengine.core.utils.XAnnotationUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 基于Id唯一标识每个数据的带过滤功能的数据源抽象类。
 * Created by 赵之韵.
 * Modified by jasontujun
 * Date: 12-3-17
 * Time: 下午11:43
 */
public class XListFilteredIdSourceImpl<T>
        implements XListDataSource<T>, XWithFilter<T>, XWithId<T> {

    private String mSourceName;
    private Field idField;

    protected XFilter<T> mFilter;
    protected ArrayList<T> mItemList;
    protected ArrayList<T> mCache;
    protected List<XWithId.Listener<T>> mListeners;
    protected List<XWithId.Listener<T>> mOriginListeners;
    protected Comparator<T> mComparator;

    /**
     * 自动通知监听者
     */
    protected volatile boolean mIsAutoNotify;

    /**
     * 重复元素是否覆盖
     */
    protected volatile boolean mOverride;

    public XListFilteredIdSourceImpl(Class<T> clazz, String sourceName) {
        mSourceName = sourceName;
        mItemList = new ArrayList<T>();
        mCache = new ArrayList<T>();
        mListeners = new CopyOnWriteArrayList<XWithId.Listener<T>>();
        mOriginListeners = new CopyOnWriteArrayList<XWithId.Listener<T>>();
        mIsAutoNotify = true;
        mOverride = false;
        doFilter();
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
        mComparator = comparator;
        Collections.sort(mCache, comparator);
        if (mIsAutoNotify)
            notifyCacheDataChanged();
    }

    @Override
    public void sortOrigin(Comparator<T> comparator) {
        mComparator = comparator;
        Collections.sort(mItemList, comparator);
        if (mIsAutoNotify)
            notifyOriginDataChanged();
    }

    @Override
    public void setFilter(XFilter<T> filter) {
        this.mFilter = filter;
        doFilter();
    }

    @Override
    public XFilter<T> getFilter() {
        return mFilter;
    }

    @Override
    public void doFilter() {
        mCache.clear();
        if (mFilter == null)
            mCache.addAll(mItemList);
        else
            mCache.addAll(mFilter.doFilter(mItemList));
        if (mComparator != null) {
            Collections.sort(mCache, mComparator);
            Collections.sort(mItemList, mComparator);
        }

        if (mIsAutoNotify)
            notifyDataChanged();
    }

    @Override
    public T get(int i) {
        return mCache.get(i);
    }
    @Override
    public T getOrigin(int i) {
        return mItemList.get(i);
    }

    @Override
    public T getById(String id) {
        int originIndex = getOriginIndexOf(id);
        if (originIndex != -1)
            return mItemList.get(originIndex);
        else
            return null;
    }

    @Override
    public int size() {
        return mCache.size();
    }


    @Override
    public int sizeOrigin() {
        return mItemList.size();
    }

    @Override
    public synchronized void add(T item) {
        if (item == null)
            return;

        int originIndex = getOriginIndexOf(getId(item));
        if (originIndex == -1) {
            mItemList.add(item);
            if (mIsAutoNotify)
                notifyAddOriginItem(item);

            T result = (mFilter != null) ? mFilter.doFilter(item) : item;
            if (result != null) {
                mCache.add(result);
                if (mIsAutoNotify)
                    notifyAddItem(item);
            }
        } else {
            if (mOverride) {
                T oldItem = mItemList.get(originIndex);
                T newItem = replace(originIndex, item);
                List<T> oldItems = new ArrayList<T>();
                oldItems.add(oldItem);
                List<T> newItems = new ArrayList<T>();
                newItems.add(newItem);
                if (mIsAutoNotify)
                    notifyReplaceOriginItem(newItems, oldItems);

                T result = (mFilter != null) ? mFilter.doFilter(item) : item;
                if (result != null && getIndexById(getId(item)) == -1) {
                    mCache.add(result);
                    if (mIsAutoNotify)
                        notifyAddItem(item);
                }
            }
        }
    }

    @Override
    public synchronized void addAll(List<T> items) {
        if (items == null || items.size() == 0)
            return;

        ArrayList<T> addedToOrigin = new ArrayList<T>();
        ArrayList<T> addedToCache = new ArrayList<T>();
        List<T> oldItems = new ArrayList<T>();
        List<T> newItems = new ArrayList<T>();
        for(T item: items) {
            int originIndex = getOriginIndexOf(getId(item));
            if (originIndex == -1) {
                mItemList.add(item);
                addedToOrigin.add(item);

                T result = (mFilter != null) ? mFilter.doFilter(item) : item;
                if (result != null) {
                    mCache.add(result);
                    addedToCache.add(item);
                }
            } else {
                if (mOverride) {
                    T oldItem = mItemList.get(originIndex);
                    T newItem = replace(originIndex, item);
                    oldItems.add(oldItem);
                    newItems.add(newItem);

                    T result = (mFilter != null) ? mFilter.doFilter(item) : item;
                    if (result != null) {
                        int index = getIndexById(getId(item));
                        if (index == -1) {
                            mCache.add(result);
                            addedToCache.add(item);
                        }
                    }
                }
            }
        }
        if (mIsAutoNotify) {
            if (addedToOrigin.size() > 0) {
                notifyAddOriginItems(addedToOrigin);
            }
            if (addedToCache.size() > 0) {
                notifyAddItems(addedToCache);
            }
            if (newItems.size() > 0) {
                notifyReplaceOriginItem(newItems, oldItems);
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return mCache.size() == 0;
    }

    @Override
    public synchronized void deleteById(String id) {
        T item = getById(id);
        if (item != null)
            delete(item);
    }

    @Override
    public synchronized void delete(T item) {
        boolean originDeleted = mItemList.remove(item);
        boolean cacheDeleted = mCache.remove(item);
        if (mIsAutoNotify) {
            if (originDeleted)
                notifyDeleteOriginItem(item);
            if (cacheDeleted)
                notifyDeleteItem(item);
        }
    }

    @Override
    public synchronized void delete(int index) {
        if (index < 0 || index >= mCache.size())
            return;

        T item = mCache.get(index);
        if (item != null)
            delete(item);
    }

    @Override
    public synchronized void deleteAll(List<T> items) {
        List<T> copyDeleted = new ArrayList<T>(items);
        boolean originDeleted = mItemList.removeAll(copyDeleted);
        boolean cacheDeleted = mCache.removeAll(copyDeleted);
        if (mIsAutoNotify) {
            if (originDeleted)
                notifyDeleteOriginItems(copyDeleted);
            if (cacheDeleted)
                notifyDeleteItems(copyDeleted);
        }
    }

    @Override
    public synchronized void deleteAllById(List<String> ids) {
        if (ids == null || ids.size() == 0)
            return;
        List<T> items = new ArrayList<T>();
        for (String id : ids) {
            T item = getById(id);
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
    public void notifyDataChanged() {
        notifyOriginDataChanged();
        notifyCacheDataChanged();
    }

    protected void notifyCacheDataChanged() {
        for (XWithId.Listener<T> listener: mListeners) {
            listener.onChange();
        }
    }

    protected void notifyOriginDataChanged() {
        for (XWithId.Listener<T> listener: mOriginListeners)
            listener.onChange();
    }

    protected void notifyReplaceOriginItem(List<T> newItems, List<T> oldItems) {
        for (XWithId.Listener<T> listener: mOriginListeners)
            listener.onReplace(newItems, oldItems);
    }

    protected void notifyAddItem(T item) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onAdd(item);
    }

    protected void notifyAddOriginItem(T item) {
        for (XWithId.Listener<T> listener: mOriginListeners)
            listener.onAdd(item);
    }

    protected void notifyAddItems(List<T> items) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onAddAll(items);
    }

    protected void notifyAddOriginItems(List<T> items) {
        for (XWithId.Listener<T> listener: mOriginListeners)
            listener.onAddAll(items);
    }

    protected void notifyDeleteItem(T item) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onDelete(item);
    }

    protected void notifyDeleteOriginItem(T item) {
        for (XWithId.Listener<T> listener: mOriginListeners)
            listener.onDelete(item);
    }

    protected void notifyDeleteItems(List<T> items) {
        for (XWithId.Listener<T> listener: mListeners)
            listener.onDeleteAll(items);
    }

    protected void notifyDeleteOriginItems(List<T> items) {
        for (XWithId.Listener<T> listener: mOriginListeners)
            listener.onDeleteAll(items);
    }

    @Override
    public int indexOf(T item) {
        return mCache.indexOf(item);
    }

    @Override
    public void clear() {
        List<T> copyCache = new ArrayList<T>(mCache);
        List<T> copyItems = new ArrayList<T>(mItemList);
        mCache.clear();
        mItemList.clear();
        if (mIsAutoNotify) {
            notifyDeleteOriginItems(copyItems);
            notifyDeleteItems(copyCache);
        }
    }

    @Override
    public boolean contains(T item) {
        return mCache.contains(item);
    }

    @Override
    public List<T> copyAll() {
        return new ArrayList<T>(mCache);
    }

    @Override
    public void registerListener(XListDataSource.Listener<T> listener) {
        if (!mListeners.contains(listener) && listener instanceof XWithId.Listener)
            mListeners.add((XWithId.Listener<T>) listener);
    }

    @Override
    public void unregisterListener(XListDataSource.Listener<T> listener) {
        mListeners.remove(listener);
    }


    @Override
    public void registerListenerForOrigin(XListDataSource.Listener<T> listener) {
        if (!mOriginListeners.contains(listener) && listener instanceof XWithId.Listener)
            mOriginListeners.add((XWithId.Listener<T>) listener);
    }

    @Override
    public void unregisterListenerForOrigin(XListDataSource.Listener<T> listener) {
        mOriginListeners.remove(listener);
    }

    @Override
    public int getIndexById(String id) {
        for (int i = 0; i<size(); i++) {
            T tmp = get(i);
            if (getId(tmp).equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public int getOriginIndexOf(String id) {
        for (int i = 0; i<sizeOrigin(); i++) {
            T tmp = getOrigin(i);
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
     * @param index 原始列表中的索引位置
     * @param newItem 要替换的新元素
     * @return 返回实际添加的新元素
     */
    protected T replace(int index, T newItem) {
        mItemList.set(index, newItem);
        return newItem;
    }
}
