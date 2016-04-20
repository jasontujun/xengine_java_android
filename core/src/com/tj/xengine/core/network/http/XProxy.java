package com.tj.xengine.core.network.http;

/**
 * Http代理接口
 * Created by jasontujun on 2015/10/26.
 */
public interface XProxy {

    enum Type {
        /**
         * Represents a direct connection, or the absence of a proxy.
         */
        PROXY_DIRECT,
        /**
         * Represents proxy for high level protocols such as HTTP or FTP.
         */
        PROXY_HTTP,
        /**
         * Represents a SOCKS (V4 or V5) proxy.
         */
        PROXY_SOCKS
    };

    Type getType();

    String getProxyAddress();

    int getProxyPort();
}
