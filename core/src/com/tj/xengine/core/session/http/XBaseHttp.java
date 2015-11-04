package com.tj.xengine.core.session.http;

/**
 * Created with IntelliJ IDEA.
 * User: jasontujun
 * Date: 13-9-11
 * Time: 下午7:15
 * To change this template use File | Settings | File Templates.
 */
public abstract class XBaseHttp implements XHttp {

    private XHttpConfig mConfig;
    protected boolean mIsDisposed;// 标识通信线程池状态，是否已经关闭

    public XBaseHttp() {
        mConfig = new XBaseHttpConfig();
        mIsDisposed = false;
    }

    protected abstract void initConfig(XHttpConfig config);

    @Override
    public XHttpConfig getConfig() {
        return mConfig.copy();
    }

    @Override
    public void setConfig(XHttpConfig config) {
        if (config == null)
            mConfig = new XBaseHttpConfig();
        else
            mConfig = config;
        initConfig(mConfig);
    }

    @Override
    public boolean isDisposed() {
        return mIsDisposed;
    }


    private class XBaseHttpConfig implements XHttpConfig, Cloneable {
        private static final int DEFAULT_CONNECTION_TIMEOUT = 30 *1000;
        private static final int DEFAULT_RESPONSE_TIMEOUT = 30 *1000;

        private XProxy mProxy;// 代理
        private String mUserAgent;// 客户端名称
        private int mConnectionTimeOut;// 尝试建立连接的等待时间，默认为30秒。
        private int mResponseTimeOut;// 等待数据返回时间，默认为30秒。
        private boolean mRedirect;// 是否开启自动重定向
        private boolean mHandleCookie;// 是否记录并携带Cookie

        public XBaseHttpConfig() {
            mConnectionTimeOut = DEFAULT_CONNECTION_TIMEOUT;
            mResponseTimeOut = DEFAULT_RESPONSE_TIMEOUT;
            mRedirect = false;
            mHandleCookie = false;
        }

        @Override
        public boolean isNetworkConnected() {
            return true;
        }

        @Override
        public int getConnectionTimeOut() {
            return mConnectionTimeOut;
        }

        @Override
        public XHttpConfig setConnectionTimeOut(int connectionTimeOut) {
            mConnectionTimeOut = connectionTimeOut;
            return this;
        }

        @Override
        public int getResponseTimeOut() {
            return mResponseTimeOut;
        }

        @Override
        public XHttpConfig setResponseTimeOut(int responseTimeOut) {
            mResponseTimeOut = responseTimeOut;
            return this;
        }

        @Override
        public String getUserAgent() {
            return mUserAgent;
        }

        @Override
        public XHttpConfig setUserAgent(String userAgent) {
            mUserAgent = userAgent;
            return this;
        }

        @Override
        public XProxy getProxy() {
            return mProxy;
        }

        @Override
        public XHttpConfig setProxy(XProxy proxy) {
            mProxy = proxy;
            return this;
        }

        @Override
        public boolean isRedirect() {
            return mRedirect;
        }

        @Override
        public XHttpConfig setRedirect(boolean open) {
            mRedirect = open;
            return this;
        }

        @Override
        public boolean isHandleCookie() {
            return mHandleCookie;
        }

        @Override
        public XHttpConfig setHandleCookie(boolean open) {
            mHandleCookie = open;
            return this;
        }

        @Override
        public XHttpConfig copy() {
            return (XHttpConfig)clone();
        }

        @Override
        protected Object clone(){
            XHttpConfig o = null;
            try {
                o = (XHttpConfig)super.clone();
            } catch(CloneNotSupportedException e) {
                e.printStackTrace();
            }
            return o;
        }
    }
}
