package com.tj.xengine.core.network.http;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-3
 * Time: 下午1:58
 * To change this template use File | Settings | File Templates.
 */
public interface XHttpResponse {

    /**
     * 获取响应的状态码
     * @return
     */
    int getStatusCode();

    /**
     * 获取响应头的所有键值对
     * @return
     */
    Map<String, List<String>> getAllHeaders();

    /**
     * 获取响应头的某个键值对
     * @param name
     * @return
     */
    List<String> getHeader(String name);

    /**
     * 获取响应的内容（以InputStream的形式）
     * @return
     */
    InputStream getContent();

    /**
     * 获取响应的内容的长度（单位：byte）
     * @return
     */
    long getContentLength();

    /**
     * 获取响应的字符编码
     * @return
     */
    Charset getContentType();

    /**
     * 释放响应的内容（关闭InputStream）
     */
    void consumeContent();

    /**
     * 响应的内容（InputStream）是否可获取多个
     * @return
     */
    boolean isRepeatable();

    List<String> getRedirectLocations();
}
