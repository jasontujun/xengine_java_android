package com.tj.xengine.core.session.http;

/**
 * Http请求参数和相关设置的接口。
 * Created by jason on 2015/10/26.
 */
public interface XHttpConfig {

    /**
     * 当前网络是否可用
     * @return
     */
    boolean isNetworkConnected();

    /**
     * 获取连接超时的设定值
     * @return
     */
    int getConnectionTimeOut();

    /**
     * 设置连接超时的值
     * @param connectionTimeOut 单位：毫秒
     */
    XHttpConfig setConnectionTimeOut(int connectionTimeOut);

    /**
     * 获取响应超时的设定值
     * @return
     */
    int getResponseTimeOut();

    /**
     * 设置响应超时的值
     * @param responseTimeOut 单位：毫秒
     */
    XHttpConfig setResponseTimeOut(int responseTimeOut);

    /**
     * 获取UserAgent
     * @return
     */
    String getUserAgent();

    /**
     * 设置UserAgent值
     * @param userAgent
     */
    XHttpConfig setUserAgent(String userAgent);

    /**
     * 获取代理
     * @return
     */
    XProxy getProxy();

    /**
     * 设置代理
     * @param proxy
     */
    XHttpConfig setProxy(XProxy proxy);

    boolean isRedirect();

    XHttpConfig setRedirect(boolean open);

    boolean isHandleCookie();

    XHttpConfig setHandleCookie(boolean open);

    XHttpConfig copy();
}
