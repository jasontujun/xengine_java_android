package com.tj.xengine.core.toolkit.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-8-13
 * Time: 上午10:14
 * To change this template use File | Settings | File Templates.
 */
public abstract class XBaseFilter<T> implements XFilter<T> {
    @Override
    public List<T> doFilter(List<T> source) {
        List<T> result = new ArrayList<T>();
        final int size = source.size();
        for (int i = 0; i < size; i++) {
            T data = doFilter(source.get(i));
            if (data != null)
                result.add(data);
        }
        return result;
    }
}
