package com.tj.xengine.core.session.http;

import java.net.HttpCookie;
import java.nio.charset.Charset;

public interface XHttp {

    Charset UTF_8 = Charset.forName("UTF-8");
    Charset ASCII = Charset.forName("US-ASCII");
    Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    Charset DEFAULT_CHARSET = ASCII;
    Charset DEF_CONTENT_CHARSET = ISO_8859_1;

    String ACCEPT_ENCODING = "Accept-Encoding";
    String TRANSFER_ENCODING = "Transfer-Encoding";
    String CONTENT_LEN = "Content-Length";
    String CONTENT_TYPE = "Content-Type";
    String CONTENT_ENCODING = "Content-Encoding";
    String CONTENT_TRANSFER_ENC = "Content-Transfer-Encoding";
    String CONTENT_DISPOSITION = "Content-Disposition";
    String EXPECT_DIRECTIVE = "Expect";
    String CONN_DIRECTIVE = "Connection";
    String TARGET_HOST = "Host";
    String USER_AGENT = "User-Agent";
    String LOCATION = "Location";
    String GZIP = "gzip";
    String CHUNKED = "chunked";

    /**
     * 创建一个Http请求对象。默认为GET请求
     * @param url
     * @return
     */
    XHttpRequest newRequest(String url);

    /**
     * 获取Http请求相关参数设置(XHttpConfig对象的副本)。
     * @return
     */
    XHttpConfig getConfig();

    /**
     * 设置Http请求相关参数。如：超时阀值，http代理，网络是否可用等。
     * 新设置的配置，会覆盖旧的配置，并可能触发相关设置操作。
     * @param config
     */
    void setConfig(XHttpConfig config);

    /**
     * 执行http请求
     * @param req
     * @return
     */
    XHttpResponse execute(XHttpRequest req);

    /**
     * 终止当前的http请求
     */
    void abort();

    /**
     * 当前HttpClient是否停止
     * @return
     */
    boolean isDisposed();

    /**
     * 停止HttpClient
     */
    void dispose();

    /**
     * 添加Cookie
     * @param cookie
     */
    void addCookie(HttpCookie cookie);

    /**
     * 设置Cookie。
     * 如果cookie的name出现重复，则覆盖之前的cookie。
     * @param cookie
     */
    void setCookie(HttpCookie cookie);

    /**
     * 清空Cookie
     */
    void clearCookie();
}
