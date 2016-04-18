package com.tj.xengine.core.data;

/**
 * 数据仓库。
 * Created by 赵之韵.
 * Date: 12-3-24
 * Time: 上午9:56
 */
public interface XDataRepository {

    void registerDataSource(XDataSource source);

    void unregisterDataSource(XDataSource source);

    XDataSource getSource(String sourceName);
}
