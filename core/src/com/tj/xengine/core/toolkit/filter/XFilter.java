package com.tj.xengine.core.toolkit.filter;

import java.util.List;

/**
 * 过滤器接口。
 * Created by 赵之韵.
 * Modified by jasontujun
 * Email: ttxzmorln@163.com
 * Date: 12-3-17
 * Time: 下午11:55
 */
public interface XFilter<T> {
    /**
     * 返回源数据列表中符合过滤条件的项目。
     * （注意不要对输入的参数source进行修改）
     * @param source 源数据
     * @return 过滤后的结果
     */
    List<T> doFilter(List<T> source);

    /**
     * 如果被过滤掉，返回null；没被过滤掉，则返回源数据
     * @param source 源数据
     * @return 过滤后的结果
     */
    T doFilter(T source);
}