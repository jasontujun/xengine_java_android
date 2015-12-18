package com.tj.xengine.java.network.http.java;

import com.tj.xengine.core.network.http.XBaseHttpResponse;

import java.net.HttpURLConnection;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-6
 * Time: 下午7:47
 * To change this template use File | Settings | File Templates.
 */
class XJavaHttpResponse extends XBaseHttpResponse {

    private HttpURLConnection mConnection;
    private Charset mCharset;

    protected void setConnection(HttpURLConnection connection) {
        mConnection = connection;
    }

    protected void setContentType(Charset charset) {
        mCharset = charset;
    }

    @Override
    public void consumeContent() {
        super.consumeContent();
        if (mConnection != null)
            mConnection.disconnect();
    }

    @Override
    public Charset getContentType() {
        return mCharset;
    }
}
