package com.tj.xengine.core.network.http;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created with IntelliJ IDEA.
 * User: jasontujun
 * Date: 13-9-11
 * Time: 下午7:15
 * To change this template use File | Settings | File Templates.
 */
public abstract class XBaseHttp implements XHttp {

    private volatile XHttpConfig mConfig;
    protected AtomicBoolean mIsDisposed;// 标识通信线程池状态，是否已经关闭

    public XBaseHttp() {
        this(XHttpConfig.createDefault());
        mIsDisposed = new AtomicBoolean(false);
    }

    public XBaseHttp(XHttpConfig config) {
        mConfig = (config == null ? XHttpConfig.createDefault() : config);
        mIsDisposed = new AtomicBoolean(false);
    }

    protected abstract void initConfig(XHttpConfig config);

    @Override
    public XHttpConfig getConfig() {
        return mConfig;
    }

    @Override
    public void setConfig(XHttpConfig config) {
        if (config == null)
            mConfig = XHttpConfig.createDefault();
        else
            mConfig = config;
        initConfig(mConfig);
    }

    @Override
    public boolean isDisposed() {
        return mIsDisposed.get();
    }
}
