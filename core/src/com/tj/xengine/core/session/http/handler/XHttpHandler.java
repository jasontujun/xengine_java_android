package com.tj.xengine.core.session.http.handler;

import com.tj.xengine.core.session.http.XHttpResponse;

/**
 * 对Http返回数据流处理的接口。
 * Created by jason on 2015/10/28.
 */
public interface XHttpHandler<T> {

    /**
     * 将XHttpResponse获取的数据流转换成对应的T类型数据。
     * @param response http响应数据
     * @return 返回T类型数据。如果转换失败，返回null。
     */
    T handleResponse(XHttpResponse response);
}
