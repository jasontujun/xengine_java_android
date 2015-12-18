package com.tj.xengine.core.network.http;

import java.util.HashMap;
import java.util.Map;

/**
 * Http请求参数和相关设置的接口。
 * Created by jason on 2015/10/26.
 */
public final class XHttpConfig {

    public static final String KEY_NETWORK_CHECKER = "NetworkChecker";// [NetworkChecker]判断网络是否连接
    public static final String KEY_PROXY = "Proxy";// [XProxy]代理
    public static final String KEY_USER_AGENT = "UserAgent";// [String]客户端名称
    public static final String KEY_CONNECTION_TIMEOUT = "ConnectionTimeout";// [int]尝试建立连接的等待时间，默认为30秒
    public static final String KEY_RESPONSE_TIMEOUT = "ResponseTimeout";// [int]等待数据返回时间，默认为30秒
    public static final String KEY_HANDLE_COOKIE = "HandleCookie";// [boolean]是否处理并记录Cookie
    public static final String KEY_HANDLE_REDIRECT = "HandleRedirect";// [boolean]是否自动处理重定向
    public static final String KEY_MAX_REDIRECT = "MaxRedirect";// [int]最大重定向次数，默认30

    protected final Map<String, Object> configs;

    protected XHttpConfig() {
        configs = new HashMap<String, Object>();
    }

    public static XHttpConfig createDefault() {
        return new Builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public XHttpConfig copy(XHttpConfig otherConfig) {
        Builder builder = builder();
        for (Map.Entry<String, Object> item : configs.entrySet()) {
            builder.set(item.getKey(), item.getValue());
        }
        if (otherConfig != null) {
            for (Map.Entry<String, Object> item : otherConfig.configs.entrySet()) {
                builder.set(item.getKey(), item.getValue());
            }
        }
        return builder.build();
    }

    public boolean isNetworkConnected() {
        NetworkChecker checker = (NetworkChecker) configs.get(KEY_NETWORK_CHECKER);
        return checker == null || checker.connected();
    }

    public XProxy getProxy() {
        return (XProxy) configs.get(KEY_PROXY);
    }

    public String getUserAgent() {
        return (String) configs.get(KEY_USER_AGENT);
    }

    public int getConnectionTimeOut() {
        Integer data = (Integer) configs.get(KEY_CONNECTION_TIMEOUT);
        return data == null ? 0 : data;
    }

    public int getResponseTimeOut() {
        Integer data = (Integer) configs.get(KEY_RESPONSE_TIMEOUT);
        return data == null ? 0 : data;
    }

    public boolean isHandleCookie() {
        Boolean data = (Boolean) configs.get(KEY_HANDLE_COOKIE);
        return data == null ? false : data;
    }

    public boolean isHandleRedirect() {
        Boolean data = (Boolean) configs.get(KEY_HANDLE_REDIRECT);
        return data == null ? false : data;
    }

    public int getMaxRedirect() {
        Integer data = (Integer) configs.get(KEY_MAX_REDIRECT);
        return data == null ? 0 : data;
    }

    public Object get(String key) {
        return configs.get(key);
    }


    public interface NetworkChecker {
        boolean connected();
    }

    public static class Builder {

        private XHttpConfig xhc;

        Builder() {
            xhc = new XHttpConfig();
            xhc.configs.put(KEY_CONNECTION_TIMEOUT, 30);
            xhc.configs.put(KEY_RESPONSE_TIMEOUT, 30);
            xhc.configs.put(KEY_HANDLE_COOKIE, true);
            xhc.configs.put(KEY_HANDLE_REDIRECT, true);
            xhc.configs.put(KEY_MAX_REDIRECT, 30);
        }

        public Builder setNewWorkChecker(NetworkChecker checker) {
            xhc.configs.put(KEY_NETWORK_CHECKER, checker);
            return this;
        }

        public Builder setProxy(XProxy proxy) {
            xhc.configs.put(KEY_PROXY, proxy);
            return this;
        }

        public Builder setConnectionTimeOut(int connectionTimeOut) {
            xhc.configs.put(KEY_CONNECTION_TIMEOUT, connectionTimeOut);
            return this;
        }

        public Builder setResponseTimeOut(int responseTimeOut) {
            xhc.configs.put(KEY_RESPONSE_TIMEOUT, responseTimeOut);
            return this;
        }

        public Builder setUserAgent(String userAgent) {
            xhc.configs.put(KEY_USER_AGENT, userAgent);
            return this;
        }

        public Builder setHandleCookie(boolean open) {
            xhc.configs.put(KEY_HANDLE_COOKIE, open);
            return this;
        }

        public Builder setHandleRedirect(boolean open) {
            xhc.configs.put(KEY_HANDLE_REDIRECT, open);
            return this;
        }

        public Builder setMaxRedirect(boolean max) {
            xhc.configs.put(KEY_MAX_REDIRECT, max);
            return this;
        }

        public Builder set(String key, Object value) {
            xhc.configs.put(key, value);
            return this;
        }

        public XHttpConfig build() {
            return xhc;
        }
    }
}
