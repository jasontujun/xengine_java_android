package com.tj.xengine.java.network.http.java;

import com.tj.xengine.core.network.http.*;
import com.tj.xengine.core.utils.XStringUtil;
import com.tj.xengine.java.utils.XLog;

import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

/**
 * <pre>
 * XJavaHttpClient是实现XHttp接口的实现类。
 * 本质是基于HttpURLConnection的包装类，额外添加了以下功能:
 *      1.直接增删Cookie(只支持Set-Cookie，不支持Set-Cookie2)
 *      2.自定义的重定向处理(可以获取重定向过程的所有信息)
 *      3.支持Chunked传输模式
 * 注意，以下XHttpConfig属性只在全局生效，如需设置，请调用{@link #setConfig(XHttpConfig)}方法进行全局设置:
 *      {@link XHttpConfig#isHandleCookie()}
 * </pre>
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

    public XJavaHttpClient(XHttpConfig config) {
        super(config);
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
        XLog.log(TAG, "send http request. url=" + req.getUrl());

        if (isDisposed())
            return null;

        final XHttpConfig config = req.getConfig() != null ? getConfig().copy(req.getConfig()) : getConfig();
        if (!config.isNetworkConnected())
            return null;

        // 构造HttpURLConnection
        XJavaHttpRequest javaRequest = (XJavaHttpRequest) req;
        HttpURLConnection connection = javaRequest.toJavaHttpRequest(config);
        if (connection == null)
            return null;
        mCurrentRequests.add(connection);

        try {
            // 和服务器通信
            int statusCode = connection.getResponseCode();
            XLog.log(TAG, "http response statusCode=" + statusCode);

            // 自动处理重定向
            List<String> redirectLocations = new ArrayList<String>();
            if (config.isHandleRedirect() && isRedirect(req.getMethod(), statusCode)) {
                connection = handleRedirect(config, connection, req, redirectLocations);
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
            javaResponse.setRedirectLocations(redirectLocations.size() == 0 ? null : redirectLocations);
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
            mCurrentRequests.remove(connection);
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
        if (mIsDisposed.compareAndSet(false, true)) {
            abort();
            clearCookie();
        }
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        mCookieStore.add(null, cookie);
    }

    @Override
    public void clearCookie() {
        mCookieStore.removeAll();
    }

    private boolean isRedirect(XHttpRequest.Method method, int statusCode) {
        switch (statusCode) {
            case HttpURLConnection.HTTP_MOVED_TEMP:
            case HttpURLConnection.HTTP_MOVED_PERM:
                return method == XHttpRequest.Method.GET;
            case HttpURLConnection.HTTP_SEE_OTHER:
                return true;
            default:
                return false;
        }
    }

    private HttpURLConnection handleRedirect(final XHttpConfig config,
                                             final HttpURLConnection conn,
                                             final XHttpRequest request,
                                             final List<String> redirectLocations) {
        XLog.log(TAG, "need handleRedirect");
        HttpURLConnection connection = conn;
        int redirectCount = 0;
        while (redirectCount < config.getMaxRedirect()) {
            redirectCount++;
            if (connection == null)
                return null;

            // 重定向，获取新的url
            String newUrl = connection.getHeaderField(XHttp.LOCATION);
            if (newUrl == null) {
                return connection;
            }
            XLog.log(TAG, "redirect newUlr=" + newUrl);

            // 关闭上一次请求
            try {
                InputStream inputStream = connection.getInputStream();
                if (inputStream != null) {
                    inputStream.close();
                } else {
                    connection.disconnect();// TIP:调用disconnect()会导致底层socket连接无法复用，影响http连接效率
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mCurrentRequests.remove(connection);

            if (isDisposed())
                return null;
            if (!config.isNetworkConnected())
                return null;

            redirectLocations.add(newUrl);
            // 生成新的URLConnection请求
            XJavaHttpRequest javaRequest = (XJavaHttpRequest) request;
            connection = javaRequest.toJavaHttpRequest(config, newUrl);
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
