package com.tj.xengine.core.data;


import com.tj.xengine.core.toolkit.filter.XFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 带过滤功能的数据源抽象类。
 * Created by 赵之韵.
 * Modified by jasontujun
 * Date: 12-3-17
 * Time: 下午11:43
 */
public class XListFilteredSourceImpl<T>
        implements XListDataSource<T>, XWithFilter<T> {

    private String mSourceName;
    protected XFilter<T> mFilter;
    protected ArrayList<T> mItemList;
    protected ArrayList<T> mCache;
    protected List<Listener<T>> mListeners;
    protected List<Listener<T>> mOriginListeners;
    protected Comparator<T> mComparator;

    /**
     * 自动通知监听者
     */
    protected boolean mIsAutoNotify = true;

    public XListFilteredSourceImpl(String sourceName) {
        mSourceName = sourceName;
        mItemList = new ArrayList<T>();
        mCache = new ArrayList<T>();
        mListeners = new CopyOnWriteArrayList<Listener<T>>();
        mOriginListeners = new CopyOnWriteArrayList<Listener<T>>();
        doFilter();
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
    public int size() {
        return mCache.size();
    }

    @Override
    public T getOrigin(int i) {
        return mItemList.get(i);
    }

    @Override
    public int sizeOrigin() {
        return mItemList.size();
    }

    @Override
    public void add(T item) {
        if (!mItemList.contains(item)) {
            mItemList.add(item);
            if (mIsAutoNotify)
                notifyAddOriginItem(item);

            T result = (mFilter != null) ? mFilter.doFilter(item) : item;
            if (result != null) {
                mCache.add(result);
                if (mIsAutoNotify)
                    notifyAddItem(item);
            }
        }
    }

    @Override
    public void addAll(List<T> items) {
        if(items == null) return;
        ArrayList<T> addedToOrigin = new ArrayList<T>();
        ArrayList<T> addedToCache = new ArrayList<T>();

        for (T item: items) {
            if (!mItemList.contains(item)) {
                mItemList.add(item);
                addedToOrigin.add(item);

                T result = (mFilter != null) ? mFilter.doFilter(item) : item;
                if (result != null) {
                    mCache.add(result);
                    addedToCache.add(item);
                }
            }
        }
        if (mIsAutoNotify) {
            notifyAddOriginItems(addedToOrigin);
            notifyAddItems(addedToCache);
        }
    }

    @Override
    public boolean isEmpty() {
        return mCache.size() == 0;
    }

    @Override
    public void delete(int index) {
        if (index < 0 || index >= mCache.size())
            return;

        T item = mCache.get(index);
        if (item != null)
            delete(item);
    }

    @Override
    public void delete(T item) {
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
    public void deleteAll(List<T> items) {
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
    public void notifyDataChanged() {
        notifyOriginDataChanged();
        notifyCacheDataChanged();
    }

    protected void notifyCacheDataChanged() {
        for (Listener<T> listener: mListeners)
            listener.onChange();
    }

    protected void notifyOriginDataChanged() {
        for (Listener<T> listener: mOriginListeners)
            listener.onChange();
    }

    protected void notifyAddItem(T item) {
        for (Listener<T> listener: mListeners)
            listener.onAdd(item);
    }

    protected void notifyAddOriginItem(T item) {
        for (Listener<T> listener: mOriginListeners)
            listener.onAdd(item);
    }

    protected void notifyAddItems(List<T> items) {
        for (Listener<T> listener: mListeners)
            listener.onAddAll(items);
    }

    protected void notifyAddOriginItems(List<T> items) {
        for (Listener<T> listener: mOriginListeners)
            listener.onAddAll(items);
    }

    protected void notifyDeleteItem(T item) {
        for (Listener<T> listener: mListeners)
            listener.onDelete(item);
    }

    protected void notifyDeleteOriginItem(T item) {
        for (Listener<T> listener: mOriginListeners)
            listener.onDelete(item);
    }

    protected void notifyDeleteItems(List<T> items) {
        for (Listener<T> listener: mListeners)
            listener.onDeleteAll(items);
    }

    protected void notifyDeleteOriginItems(List<T> items) {
        for (Listener<T> listener: mOriginListeners)
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
    public void registerListener(Listener<T> listener) {
        if (!mListeners.contains(listener))
            mListeners.add(listener);
    }

    @Override
    public void unregisterListener(Listener<T> listener) {
        mListeners.remove(listener);
    }


    @Override
    public void registerListenerForOrigin(Listener<T> listener) {
        if (!mOriginListeners.contains(listener))
            mOriginListeners.add(listener);
    }

    @Override
    public void unregisterListenerForOrigin(Listener<T> listener) {
        mOriginListeners.remove(listener);
    }

    @Override
    public void setAutoNotifyListeners(boolean isAuto) {
        this.mIsAutoNotify = isAuto;
    }

    @Override
    public String getSourceName() {
        return mSourceName;
    }
}
