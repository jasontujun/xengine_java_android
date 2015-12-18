package com.tj.xengine.android.network.http;

import com.tj.xengine.core.network.http.XHttpResponse;
import org.apache.http.util.ByteArrayBuffer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 可以缓存XHttpResponse中InputStream的包装类。
 * 通过此类，可以通过多次调用getInputStream()获取多个InputStream。
 * 参考BufferedHttpEntity.java的实现。
 * @see org.apache.http.entity.BufferedHttpEntity
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-4
 * Time: 下午6:41
 * To change this template use File | Settings | File Templates.
 */
public class XBufferedHttpResponse implements XHttpResponse {
    private XHttpResponse mResponseWrapper;
    private final byte[] mBuffer;

    public XBufferedHttpResponse(XHttpResponse responseWrapper) throws IOException {
        super();
        mResponseWrapper = responseWrapper;
        if (!mResponseWrapper.isRepeatable() || mResponseWrapper.getContentLength() < 0) {
            mBuffer = toByteArray(mResponseWrapper.getContent(),
                    mResponseWrapper.getContentLength());
        } else {
            mBuffer = null;
        }
    }
    @Override
    public int getStatusCode() {
        return mResponseWrapper.getStatusCode();
    }

    @Override
    public Map<String, List<String>> getAllHeaders() {
        return mResponseWrapper.getAllHeaders();
    }

    @Override
    public List<String> getHeader(String name) {
        return mResponseWrapper.getHeader(name);
    }

    @Override
    public InputStream getContent() {
        if (mBuffer != null)
            return new ByteArrayInputStream(mBuffer);
        else
            return mResponseWrapper.getContent();
    }

    @Override
    public void consumeContent() {
        mResponseWrapper.consumeContent();
    }

    @Override
    public long getContentLength() {
        if (mBuffer != null)
            return mBuffer.length;
        else
            return mResponseWrapper.getContentLength();
    }

    @Override
    public Charset getContentType() {
        return mResponseWrapper.getContentType();
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    private static byte[] toByteArray(final InputStream inStream,
                                     final long contentLength) throws IOException {
        if (inStream == null) {
            return new byte[] {};
        }
        if (contentLength > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
        }
        int i = (int)contentLength;
        if (i < 0) {
            i = 4096;
        }
        ByteArrayBuffer buffer = new ByteArrayBuffer(i);
        try {
            byte[] tmp = new byte[4096];
            int l;
            while((l = inStream.read(tmp)) != -1) {
                buffer.append(tmp, 0, l);
            }
        } finally {
            inStream.close();
        }
        return buffer.toByteArray();
    }

}
