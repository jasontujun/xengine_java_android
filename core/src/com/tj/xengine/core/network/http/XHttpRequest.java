package com.tj.xengine.core.network.http;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-3
 * Time: 下午1:58
 * To change this template use File | Settings | File Templates.
 */
public interface XHttpRequest {

    enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE
    }

    /**
     * 设置请求的url
     * @param url
     * @return
     */
    XHttpRequest setUrl(String url);

    /**
     * 获取请求的url
     * @return
     */
    String getUrl();

    /**
     * 设置请求的类型，默认是GET请求
     * @see HttpMethod
     * @param method
     */
    XHttpRequest setMethod(HttpMethod method);

    /**
     * 获取请求的类型
     * @see HttpMethod
     * @return
     */
    HttpMethod getMethod();

    /**
     * 设置请求的字符编码
     * @return 如果字符编码支持，返回true；否则返回false
     */
    boolean setCharset(String charsetName);

    /**
     * 获取该请求的字符编码
     * @return
     */
    String getCharset();

    /**
     * 请求的数据是否压缩（实际在请求头中添加参数Accept-Encoding : gzip）
     * @param gzip 是否压缩
     */
    XHttpRequest setGzip(boolean gzip);

    /**
     * 获取该请求是否会压缩
     * @return
     */
    boolean isGzip();


    /**
     * 请求中的数据(通常是文件数据)是否以chunked模式传输
     * @param chunked 是否使用chunked模式传输
     */
    XHttpRequest setChunked(boolean chunked);

    /**
     * 获取该请求是否使用chunked模式传输
     * @return
     */
    boolean isChunked();

    /**
     * 对该请求临时设置相关http配置
     * @param config
     */
    XHttpRequest setConfig(XHttpConfig config);

    /**
     * 获取对该请求的临时http配置
     * @return
     */
    XHttpConfig getConfig();

    /**
     * 添加字符串的参数（键值对）
     * @param key
     * @param value
     */
    XHttpRequest addStringParam(String key, String value);

    /**
     * 添加文件参数（常用于上传）
     * @param key
     * @param file
     */
    XHttpRequest addFileParam(String key, File file);

    /**
     * 添加请求头的参数（键值对）
     * @param key
     * @param value
     */
    XHttpRequest addHeader(String key, String value);
}
