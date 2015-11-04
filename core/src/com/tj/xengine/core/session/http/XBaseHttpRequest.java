package com.tj.xengine.core.session.http;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * XHttpRequest的基础实现类
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-3
 * Time: 下午5:57
 * To change this template use File | Settings | File Templates.
 */
public abstract class XBaseHttpRequest implements XHttpRequest {
    private String mUrl;
    private HttpMethod mMethod;
    private String mCharsetName;
    private boolean mGzip;
    private XHttpConfig mConfig;
    protected Map<String, String> mStringParams;
    protected Map<String, File> mFileParams;
    protected Map<String, String> mHeaders;

    protected XBaseHttpRequest() {
        mMethod = HttpMethod.GET;
    }

    @Override
    public String getUrl() {
        return mUrl;
    }

    @Override
    public XHttpRequest setUrl(String url) {
        mUrl = url;
        return this;
    }

    @Override
    public HttpMethod getMethod() {
        return mMethod;
    }

    @Override
    public XHttpRequest setMethod(HttpMethod method) {
        mMethod = method;
        return this;
    }

    @Override
    public String getCharset() {
        return mCharsetName;
    }

    @Override
    public boolean setCharset(String charsetName) {
        mCharsetName = charsetName;
        return Charset.isSupported(mCharsetName);
    }

    @Override
    public boolean isGzip() {
        return mGzip;
    }

    @Override
    public XHttpRequest setGzip(boolean gzip) {
        mGzip = gzip;
        return this;
    }

    @Override
    public XHttpConfig getConfig() {
        return mConfig;
    }

    @Override
    public XHttpRequest setConfig(XHttpConfig config) {
        mConfig = config;
        return this;
    }

    @Override
    public XHttpRequest addStringParam(String key, String value) {
        if (mStringParams == null)
            mStringParams = new HashMap<String, String>();
        mStringParams.put(key, value);
        return this;
    }

    @Override
    public XHttpRequest addFileParam(String key, File file) {
        if (mFileParams == null)
            mFileParams = new HashMap<String, File>();
        mFileParams.put(key, file);
        return this;
    }

    @Override
    public XHttpRequest addHeader(String key, String value) {
        if (mHeaders == null)
            mHeaders = new HashMap<String, String>();
        mHeaders.put(key, value);
        return this;
    }
}
