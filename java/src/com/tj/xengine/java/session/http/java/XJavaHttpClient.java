package com.tj.xengine.java.session.http.java;

import com.tj.xengine.core.session.http.*;
import com.tj.xengine.core.utils.XStringUtil;
import com.tj.xengine.java.utils.XLog;

import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

/**
 * XJavaHttpClient是实现XHttp接口的实现类。
 * 本质是基于HttpURLConnection的包装类，
 * 添加了Cookie的管理。（目前只支持Set-Cookie，不支持Set-Cookie2）
 * 添加了对通信过程的监听。
 * @see HttpURLConnection
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-3
 * Time: 下午2:38
 */
public class XJavaHttpClient extends XBaseHttp {

    private static final String TAG = XJavaHttpClient.class.getSimpleName();

    private CookieStore mCookieStore;
    private List<HttpURLConnection> mCurrentRequests;

    public XJavaHttpClient() {
        super();
        mCurrentRequests = new CopyOnWriteArrayList<HttpURLConnection>();
        CookieManager cookieMgr = new CookieManager();
        mCookieStore = cookieMgr.getCookieStore();
        CookieHandler.setDefault(cookieMgr);
        initConfig(getConfig());
    }

    @Override
    protected void initConfig(XHttpConfig config) {
        if (config.isHandleCookie())
            ((CookieManager)CookieHandler.getDefault()).setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        else
            ((CookieManager)CookieHandler.getDefault()).setCookiePolicy(CookiePolicy.ACCEPT_NONE);
    }

    @Override
    public XHttpRequest newRequest(String url) {
        return new XJavaHttpRequest().setUrl(url);
    }

    @Override
    public XHttpResponse execute(XHttpRequest req) {
        if (req == null || !(req instanceof XJavaHttpRequest))
            throw new IllegalArgumentException("XHttpRequest is not correct. Needed XJavaHttpRequest!");

        final boolean needRecoverGlobalConfig = req.getConfig() != null;
        final XHttpConfig globalConfig = getConfig();

        XLog.log(TAG, "send http request. url=" + req.getUrl());
        // 构造HttpURLConnection
        HttpURLConnection connection = prepareHttpRequest(req);
        if (connection == null)
            return null;
        mCurrentRequests.add(connection);

        // 使req的config设置局部生效
        if (needRecoverGlobalConfig) {
            setConfig(req.getConfig());
        }
        try {
            // 和服务器通信
            int statusCode = connection.getResponseCode();
            XLog.log(TAG, "http response statusCode=" + statusCode);

            // 自动处理重定向
            XHttpConfig config = req.getConfig() != null ? req.getConfig() : getConfig();
            if (config.isRedirect() && isRedirect(req.getMethod(), statusCode)) {
                connection = handleRedirect(connection, req);
            }
            if (connection == null)
                return  null;

            InputStream inputStream = null;
            try {
                // TIP 当服务器返回4XX或5XX时，调用getInputStream()会抛出IOException
                // 为了让后面生成HttpResponse正常进行，此处要捕获该异常
                inputStream = connection.getInputStream();
            } catch (Exception ex) {
//                ex.printStackTrace();
            }

            // 解析出Response的ContentType对应的Charset
            Charset charset = null;
            String contentType = connection.getContentType();
            if (!XStringUtil.isEmpty(contentType))
                charset = XJavaHttpUtil.getResponseCharset(contentType);

            // 构造HttpResponse
            XJavaHttpResponse javaResponse = new XJavaHttpResponse();
            javaResponse.setConnection(connection);
            javaResponse.setStatusCode(connection.getResponseCode());
            javaResponse.setContentLength(connection.getContentLengthLong());
            javaResponse.setContentType(charset);
            javaResponse.setAllHeaders(connection.getHeaderFields());
            if (inputStream != null) {
                // TODO Java自动做了Chunked解码??
                // 如果response是chunked编码的分块传输，则用ChunkedInputStream转换一下
//                String transferEncoding = connection.getHeaderField(XHttp.TRANSFER_ENCODING);
//                if (!XStringUtil.isEmpty(transferEncoding) && transferEncoding.equalsIgnoreCase(XHttp.CHUNKED)) {
//                    inputStream = new XChunkedInputStream(inputStream);
//                }
                // 如果response是压缩的，则自动用GZIPInputStream转换一下
                String contentEncoding = connection.getContentEncoding();
                if (!XStringUtil.isEmpty(contentEncoding) && contentEncoding.equalsIgnoreCase(XHttp.GZIP)) {
                    javaResponse.setContent(new GZIPInputStream((inputStream)));
                } else {
                    javaResponse.setContent(inputStream);
                }
            }
            return javaResponse;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
//            mCurrentRequest.disconnect();// TIP 不能在此disconnect，会关闭InputStream
            mCurrentRequests.remove(connection);
            // 恢复全局的config设置
            if (needRecoverGlobalConfig) {
                setConfig(globalConfig);
            }
        }
    }

    @Override
    public void abort() {
        for (HttpURLConnection connection : mCurrentRequests) {
            if (connection != null)
                connection.disconnect();
        }
        mCurrentRequests.clear();
    }

    @Override
    public void dispose() {
        if (!mIsDisposed) {
            abort();
            mIsDisposed = true;
            clearCookie();
        }
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        mCookieStore.add(null, cookie);
    }

    @Override
    public void setCookie(HttpCookie cookie) {
        mCookieStore.remove(null, cookie);
    }

    @Override
    public void clearCookie() {
        mCookieStore.removeAll();
    }


    /**
     * 用XHttpRequest生成HttpURLConnection请求
     * @param req XHttpRequest的http请求
     * @return 返回java框架的HttpURLConnection请求
     */
    private HttpURLConnection prepareHttpRequest(XHttpRequest req) {
        if (mIsDisposed)
            return null;

        XHttpConfig config = req.getConfig() != null ? req.getConfig() : getConfig();
        if (!config.isNetworkConnected()) {
            return null;
        }

        // 设置代理
        Proxy proxy = null;
        if (config.getProxy() != null) {
            Proxy.Type type;
            switch (config.getProxy().getType()) {
                case PROXY_HTTP:
                    type = Proxy.Type.HTTP;
                    break;
                case PROXY_SOCKS:
                    type = Proxy.Type.SOCKS;
                    break;
                case PROXY_DIRECT:
                    type = Proxy.Type.DIRECT;
                    break;
                default:
                    type = Proxy.Type.DIRECT;
                    break;
            }
            if (type != Proxy.Type.DIRECT)
                proxy = new Proxy(type, new InetSocketAddress(config.getProxy().getProxyAddress(), config.getProxy().getProxyPort()));
        }
        // 构造URLConnection
        XJavaHttpRequest javaRequest = (XJavaHttpRequest) req;
        // 如果用户传入的request的config为空，则把当前全局的config设置进request中，便于生成有效的URLConnection
        if (javaRequest.getConfig() == null)
            javaRequest.setConfig(config);
        return javaRequest.toJavaHttpRequest(proxy);
    }

    private boolean isRedirect(XHttpRequest.HttpMethod method, int statusCode) {
        switch (statusCode) {
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
                return method == XHttpRequest.HttpMethod.GET;
            case HttpURLConnection.HTTP_SEE_OTHER:
                return true;
            default:
                return false;
        }
    }

    private HttpURLConnection handleRedirect(HttpURLConnection connection, XHttpRequest request) {
        XLog.log(TAG, "need handleRedirect");
        while (true) {
            if (connection == null)
                return null;

            // 重定向，获取新的url
            String newUrl = connection.getHeaderField(XHttp.LOCATION);
            if (newUrl == null) {
                return connection;
            }
            XLog.log(TAG, "redirect newUlr=" + newUrl);
            request.setUrl(newUrl);

            // 关闭上一次请求
            connection.disconnect();
            mCurrentRequests.remove(connection);

            // 生成新的url的请求
            connection = prepareHttpRequest(request);
            if (connection == null)
                return null;
            mCurrentRequests.add(connection);

            // 发起新的通信
            try {
                int statusCode = connection.getResponseCode();
                XLog.log(TAG, "redirect statusCode=" + statusCode);
                // 如果还是重定向，则继续重新发起请求，否则跳出循环
                if (!isRedirect(request.getMethod(), statusCode))
                    break;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return connection;
    }
}
