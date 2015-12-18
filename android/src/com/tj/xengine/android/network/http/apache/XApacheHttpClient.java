package com.tj.xengine.android.network.http.apache;

import com.tj.xengine.core.network.http.XBaseHttp;
import com.tj.xengine.core.network.http.XHttpConfig;
import com.tj.xengine.core.network.http.XHttpRequest;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * XApacheHttpClient是实现XHttp接口的实现类。
 * 一个XApacheHttpClient对象就是一个通信线程池，本质是基于DefaultHttpClient的包装类。
 * @see DefaultHttpClient
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-2
 * Time: 下午3:38
 */
public class XApacheHttpClient extends XBaseHttp {

    public final static int MAX_TOTAL_CONNECTIONS = 800;// 最大连接数
    public final static int MAX_ROUTE_CONNECTIONS = 400;// 每个路由最大连接数

    protected CookieStore mCookieStore;
    private HttpContext mHttpContext;
    private DefaultHttpClient mHttpClient;
    private HttpUriRequest mCurrentRequest;

    public XApacheHttpClient() {
        super();

        mCookieStore = new BasicCookieStore();
        mHttpContext = new BasicHttpContext();
        mHttpContext.setAttribute(ClientContext.COOKIE_STORE, mCookieStore);
        HttpParams params = new BasicHttpParams();
        XHttpConfig _config = getConfig();
        // http协议参数设置
        if (!XStringUtil.isEmpty(_config.getUserAgent()))
            HttpProtocolParams.setUserAgent(params, _config.getUserAgent());
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
        HttpProtocolParams.setUseExpectContinue(params, true);
        // http连接池设置
        ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);// 设置最大连接数
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(MAX_ROUTE_CONNECTIONS);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);// 设置每个路由最大连接数
        // http连接参数设置
        HttpConnectionParams.setSocketBufferSize(params, 8 * 1024);
        HttpConnectionParams.setConnectionTimeout(params, _config.getConnectionTimeOut());
        HttpConnectionParams.setSoTimeout(params, _config.getResponseTimeOut());
        HttpConnectionParams.setStaleCheckingEnabled(params, false);
        HttpConnectionParams.setTcpNoDelay(params, true);
        // http重定向设置
        HttpClientParams.setRedirecting(params, false);
        // scheme设置
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        // 线程池设置
        ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        mHttpClient = new DefaultHttpClient(cm, params);
    }

    @Override
    protected void initConfig(XHttpConfig config) {
        // TODO
    }

    @Override
    public XHttpRequest newRequest(String url) {
        XApacheHttpRequest request = new XApacheHttpRequest();
        request.setUrl(url);
        return request;
    }

    @Override
    public XHttpResponse execute(XHttpRequest req) {
        if (req == null || !(req instanceof XApacheHttpRequest))
            throw new IllegalArgumentException("XHttpRequest is not correct. Required XApacheHttpRequest!");

        try {
            // 构造HttpUriRequest
            HttpUriRequest request = prepareHttpRequest(req);
            mCurrentRequest = request;
            if (request == null)
                return null;

            // 和服务器通信
            HttpResponse response = mHttpClient.execute(request, mHttpContext);

            // 自动处理重定向
            XHttpConfig config = req.getConfig() != null ? req.getConfig() : getConfig();
            if (config.isHandleRedirect() && isRedirect(request, response)) {
                response = handleRedirect(req, response);
            }
            if (response == null)
                return  null;

            // 构造apacheResponse
            XApacheHttpResponse apacheResponse = new XApacheHttpResponse();
            if (response.getStatusLine() != null)
                apacheResponse.setStatusCode(response.getStatusLine().getStatusCode());
            if (response.getEntity() != null) {
                // 如果response是压缩的，则自动用GZIPInputStream转换一下
                Header contentEncoding = response.getFirstHeader(HTTP.CONTENT_ENCODING);
                if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                    apacheResponse.setContent(new GZIPInputStream((response.getEntity().getContent())));
                } else {
                    apacheResponse.setContent(response.getEntity().getContent());
                }
                apacheResponse.setContentLength(response.getEntity().getContentLength());
            }
            Header[] headers = response.getAllHeaders();
            if (headers != null) {
                Map<String, List<String>> wrapperHeaders = new HashMap<String, List<String>>();
                for (Header header : headers) {
                    if (wrapperHeaders.containsKey(header.getName())) {
                        List<String> valueList = wrapperHeaders.get(header.getName());
                        valueList.add(header.getValue());
                    } else {
                        List<String> valueList = new ArrayList<String>();
                        valueList.add(header.getValue());
                        wrapperHeaders.put(header.getName(), valueList);
                    }
                }
                apacheResponse.setAllHeaders(wrapperHeaders);
            }
            return apacheResponse;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mCurrentRequest = null;
        }
        return null;
    }

    @Override
    public void abort() {
        if (mCurrentRequest != null)
            mCurrentRequest.abort();
    }

    @Override
    public void dispose() {
        if (mIsDisposed.compareAndSet(false, true)) {
            mHttpClient.getConnectionManager().shutdown();
            clearCookie();
        }
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        // TODO
    }

    @Override
    public void clearCookie() {
        // TODO
    }


    /**
     * 用XHttpRequest生成HttpUriRequest请求
     * @param req XHttpRequest的http请求
     * @return 返回apache框架的HttpUriRequest请求
     */
    private HttpUriRequest prepareHttpRequest(XHttpRequest req) {
        if (isDisposed())
            return null;

        XHttpConfig config = req.getConfig() != null ? req.getConfig() : getConfig();
        if (!config.isNetworkConnected()) {
            return null;
        }

        // 设置代理
        HttpHost proxy = null;
        if (config.getProxy() != null) {
            String scheme = null;
            switch (config.getProxy().getType()) {
                case PROXY_HTTP:
                    scheme = "http";
                    break;
                case PROXY_SOCKS:
                    scheme = "socks";
                    break;
            }
            if (scheme != null)
                proxy = new HttpHost(config.getProxy().getProxyAddress(), config.getProxy().getProxyPort(), scheme);
        }
        if (proxy != null) {
            HttpParams params = mHttpClient.getParams();
            params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            mHttpClient.setParams(params);
        }

        // 构造HttpUriRequest
        XApacheHttpRequest apacheRequest = (XApacheHttpRequest) req;
        return (HttpUriRequest) apacheRequest.toApacheHttpRequest();
    }

    private boolean isRedirect(HttpRequest request, HttpResponse response) {
        if (response == null || response.getStatusLine() == null)
            return false;

        int statusCode = response.getStatusLine().getStatusCode();
        switch (statusCode) {
            case HttpStatus.SC_MOVED_TEMPORARILY:
            case HttpStatus.SC_MOVED_PERMANENTLY:
            case HttpStatus.SC_TEMPORARY_REDIRECT:
                String method = request.getRequestLine().getMethod();
                return method.equalsIgnoreCase(HttpGet.METHOD_NAME)
                        || method.equalsIgnoreCase(HttpHead.METHOD_NAME);
            case HttpStatus.SC_SEE_OTHER:
                return true;
            default:
                return false;
        }
    }

    private HttpResponse handleRedirect(XHttpRequest req, HttpResponse response) {
        while (true) {
            if (response == null)
                return null;

            // 重定向，获取新的url
            Header header = response.getFirstHeader("Location");
            if (header == null) {
                return response;
            }
            String redirectUrl = header.getValue();
            req.setUrl(redirectUrl);

            // 关闭上一次请求
            if (response.getEntity() != null)
                try {
                    response.getEntity().consumeContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            // 生成新的url的请求
            try {
                HttpUriRequest request = prepareHttpRequest(req);
                mCurrentRequest = request;
                if (request == null)
                    return null;
                // 发起新的通信
                response = mHttpClient.execute(request, mHttpContext);
                // 如果还是重定向，则继续重新发起请求，否则跳出循环
                if (!isRedirect(request, response))
                    break;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return response;
    }
}
