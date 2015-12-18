package com.tj.xengine.java.network.http.apache;

import com.tj.xengine.core.network.http.*;
import com.tj.xengine.java.utils.XLog;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.CodingErrorAction;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.GZIPInputStream;

/**
 * <pre>
 * XApacheHttpClient是实现XHttp接口的实现类。
 * 本质是基于HttpClient的包装类，一个XApacheHttpClient对象就是一个通信线程池，额外添加以下功能:
 *      1.直接增删Cookie(只支持Set-Cookie，不支持Set-Cookie2)
 *      2.直接支持Socks代理
 * 注意，以下XHttpConfig属性只在全局生效，如需设置，请调用{@link #setConfig(XHttpConfig)}方法进行全局设置:
 *      {@link XHttpConfig#getUserAgent()}
 * </pre>
 * @see org.apache.http.client.HttpClient
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-2
 * Time: 下午3:38
 */
public class XApacheHttpClient extends XBaseHttp {

    private static final String TAG = XApacheHttpClient.class.getSimpleName();

    public static final String KEY_MAX_CONNECTIONS_TOTAL = "MaxTotal";// 最大连接数.默认20
    public static final String KEY_MAX_CONNECTIONS_PER_ROUTE = "MaxPerRoute";// 每个路由最大连接数.默认2

    protected CookieStore mCookieStore;
    private volatile CloseableHttpClient mHttpClient;
    private List<HttpUriRequest> mCurrentRequests;

    public XApacheHttpClient() {
        super();
        mCurrentRequests = new CopyOnWriteArrayList<HttpUriRequest>();
        mCookieStore = new BasicCookieStore();
        initConfig(getConfig());
    }


    public XApacheHttpClient(XHttpConfig config) {
        super(config);
        mCurrentRequests = new CopyOnWriteArrayList<HttpUriRequest>();
        mCookieStore = new BasicCookieStore();
        initConfig(getConfig());
    }

    protected static RequestConfig toRequestConfig(XHttpConfig config) {
        // 创建代理对象
        HttpHost proxy = null;
        // TIP: HttpClient不直接支持Socks代理,通过重写底层socket连接来时实现Socks代理功能
        if (config.getProxy() != null && config.getProxy().getType() != XProxy.Type.PROXY_SOCKS) {
            String scheme = null;
            switch (config.getProxy().getType()) {
                case PROXY_HTTP:
                    scheme = "http";
                    break;
            }
            if (scheme != null)
                proxy = new HttpHost(config.getProxy().getProxyAddress(), config.getProxy().getProxyPort(), scheme);
        }
        // 创建RequestConfig对象
        return RequestConfig.custom()
                .setExpectContinueEnabled(false)// Expect 100-continue有副作用，关闭
                .setProxy(proxy)
                .setRedirectsEnabled(config.isHandleRedirect())
                .setMaxRedirects(config.getMaxRedirect())
                .setCookieSpec(config.isHandleCookie() ? CookieSpecs.STANDARD : CookieSpecs.IGNORE_COOKIES)
                .setConnectTimeout(config.getConnectionTimeOut())
                .setSocketTimeout(config.getResponseTimeOut())
                .build();
    }

    @Override
    protected void initConfig(XHttpConfig config) {
        final CloseableHttpClient preHttpClient = mHttpClient;
        // 设置连接池参数
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", new SocksPlainConnectionSocketFactory())
                .register("https", new SocksSSLConnectionSocketFactory(SSLContexts.createSystemDefault()))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(reg);
        cm.setValidateAfterInactivity(1000);// 无活动的1秒后去检查无效的连接，默认为2秒
        if (config.get(KEY_MAX_CONNECTIONS_TOTAL) != null &&
                (Integer)config.get(KEY_MAX_CONNECTIONS_TOTAL) > 0) {
            cm.setMaxTotal((Integer)config.get(KEY_MAX_CONNECTIONS_TOTAL));
        }
        if (config.get(KEY_MAX_CONNECTIONS_PER_ROUTE) != null &&
                (Integer)config.get(KEY_MAX_CONNECTIONS_PER_ROUTE) > 0) {
            cm.setDefaultMaxPerRoute((Integer) config.get(KEY_MAX_CONNECTIONS_PER_ROUTE));
        }
        // 创建HttpClient对象
        mHttpClient = HttpClients.custom()
                .setUserAgent(config.getUserAgent())
                .setConnectionManager(cm)
                .setDefaultCookieStore(mCookieStore)
                .setDefaultRequestConfig(toRequestConfig(config))
                .setDefaultConnectionConfig(
                        ConnectionConfig.custom()
                                .setCharset(Consts.UTF_8)
                                .setMalformedInputAction(CodingErrorAction.REPORT)
                                .setUnmappableInputAction(CodingErrorAction.REPORT)
                                .build())
                .setDefaultSocketConfig(
                        SocketConfig.custom()
                                .setTcpNoDelay(true)
                                .build())
                .build();
        // 关闭并释放之前的httpClient
        if (preHttpClient != null) {
            try {
                preHttpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
        XLog.log(TAG, "send http request. url=" + req.getUrl());

        if (isDisposed())
            return null;

        final XHttpConfig config = req.getConfig() != null ? getConfig().copy(req.getConfig()) : getConfig();
        if (!config.isNetworkConnected()) {
            return null;
        }

        // 构造HttpUriRequest
        XApacheHttpRequest apacheRequest = (XApacheHttpRequest) req;
        final HttpUriRequest request = (HttpUriRequest) apacheRequest.toApacheHttpRequest();
        if (request == null)
            return null;
        mCurrentRequests.add(request);

        // 如果设置的代理为Socks代理，创建对应的HttpClientContent上下文环境
        HttpClientContext localContext = HttpClientContext.create();
        if (config.getProxy() != null && config.getProxy().getType() == XProxy.Type.PROXY_SOCKS) {
            localContext.setAttribute("socks.address",
                    new InetSocketAddress(config.getProxy().getProxyAddress(), config.getProxy().getProxyPort()));
        }

        try {
            // 和服务器通信
            CloseableHttpResponse response = mHttpClient.execute(request, localContext);
            if (response == null)
                return  null;


            // 构造apacheResponse
            XApacheHttpResponse apacheResponse = new XApacheHttpResponse();
            // 设置状态码
            if (response.getStatusLine() != null)
                apacheResponse.setStatusCode(response.getStatusLine().getStatusCode());
            // 设置Entity
            apacheResponse.setEntity(response.getEntity());
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
            // 设置header
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
            // 设置重定向路径
            List<URI> redirectLocations = localContext.getRedirectLocations();
            if (redirectLocations != null && redirectLocations.size() > 0) {
                List<String> locations = new ArrayList<String>();
                for (URI uri : redirectLocations) {
                    locations.add(uri.toString());
                }
                apacheResponse.setRedirectLocations(locations);
            }
            return apacheResponse;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mCurrentRequests.remove(request);
        }
        return null;
    }

    @Override
    public void abort() {
        for (HttpUriRequest request : mCurrentRequests) {
            if (request != null) {
                try {
                    request.abort();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        mCurrentRequests.clear();
    }

    @Override
    public void dispose() {
        if (mIsDisposed.compareAndSet(false, true)) {
            try {
                mHttpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            abort();
            clearCookie();
        }
    }

    @Override
    public void addCookie(HttpCookie cookie) {
        BasicClientCookie c = new BasicClientCookie(cookie.getName(), cookie.getValue());
        c.setComment(cookie.getComment());
        c.setDomain(cookie.getDomain());
        c.setPath(cookie.getPath());
        c.setVersion(cookie.getVersion());
        c.setSecure(cookie.getSecure());
        if (cookie.getMaxAge() != -1) {
            Object value = null;
            try {
                Field field = cookie.getClass().getDeclaredField("whenCreated");
                field.setAccessible(true);
                value = field.get(cookie);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (value != null) {
                long whenCreated = (Long) value;
                long expiryTime = whenCreated + cookie.getMaxAge() * 1000;
                c.setExpiryDate(new Date(expiryTime));
            }
        }
        mCookieStore.addCookie(c);
    }

    @Override
    public void clearCookie() {
        mCookieStore.clear();
    }

    /**
     * 支持Socks代理功能的SSLConnectionSocketFactory
     */
    private static class SocksSSLConnectionSocketFactory extends SSLConnectionSocketFactory {

        public SocksSSLConnectionSocketFactory(final SSLContext sslContext) {
            super(sslContext);
        }

        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
            if (socksaddr  == null) {
                return super.createSocket(context);
            } else {
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
                return new Socket(proxy);
            }
        }

    }

    /**
     * 支持Socks代理的PlainConnectionSocketFactory
     */
    private static class SocksPlainConnectionSocketFactory extends PlainConnectionSocketFactory {

        @Override
        public Socket createSocket(final HttpContext context) throws IOException {
            InetSocketAddress socksaddr = (InetSocketAddress) context.getAttribute("socks.address");
            if (socksaddr  == null) {
                return super.createSocket(context);
            } else {
                Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
                return new Socket(proxy);
            }
        }
    }
}
