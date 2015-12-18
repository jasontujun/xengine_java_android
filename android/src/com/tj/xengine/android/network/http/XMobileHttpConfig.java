package com.tj.xengine.android.network.http;

import android.content.Context;
import com.tj.xengine.android.utils.XNetworkUtil;
import com.tj.xengine.core.network.http.XHttpConfig;
import com.tj.xengine.core.network.http.XProxy;

/**
 * Created by jason on 2015/10/27.
 */
public class XMobileHttpConfig implements XHttpConfig {

    private Context mContext;
    private XProxy mProxy;// 代理
    private String mUserAgent;// 客户端名称
    private int mConnectionTimeOut;// 尝试建立连接的等待时间，默认为30秒。
    private int mResponseTimeOut;// 等待数据返回时间，默认为30秒。
    private boolean mRedirect;// 是否开启自动重定向
    private boolean mHandleCookie;// 是否记录并携带Cookie

    public XMobileHttpConfig(Context context) {
        mContext = context;
        mConnectionTimeOut = 60 * 1000;
        mResponseTimeOut = 60 * 1000;
        mRedirect = false;
        mHandleCookie = false;
    }

    @Override
    public boolean isNetworkConnected() {
        return XNetworkUtil.isNetworkConnected(mContext);
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
