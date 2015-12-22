package com.tj.xengine.java.network.http.java;

import com.tj.xengine.core.network.http.XBaseHttpRequest;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpConfig;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

/**
 * 用于XApacheHttpClient的XHttpRequest实现类。
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-3
 * Time: 下午6:13
 */
class XJavaHttpRequest extends XBaseHttpRequest {

    protected XJavaHttpRequest() {
        super();
    }


    protected HttpURLConnection toJavaHttpRequest(XHttpConfig config) {
        return toJavaHttpRequest(config, getUrl());
    }

    protected HttpURLConnection toJavaHttpRequest(XHttpConfig config, String url) {
        if (XStringUtil.isEmpty(url))
            return null;

        HttpURLConnection request = null;
        switch (getMethod()) {
            case GET:
                request = createGetStyleRequest("GET", url, config);
                break;
            case POST:
                request = createPostStyleRequest("POST", url, config);
                break;
            case PUT:
                request = createPostStyleRequest("PUT", url, config);
                break;
            case DELETE:
                request = createGetStyleRequest("DELETE", url, config);
                break;
        }
        return request;
    }

    private Proxy getProxy(final XHttpConfig config) {
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
        return proxy;
    }

    /**
     * 创建不含Body的请求，包括GET、DELETE类型的请求。
     * @param method 请求类型
     * @return 返回HttpURLConnection请求对象
     */
    private HttpURLConnection createGetStyleRequest(String method, String url, XHttpConfig config) {
        try {
            Proxy proxy = getProxy(config);
            URL requestUrl = new URL(url);
            HttpURLConnection request = (HttpURLConnection) (proxy == null ?
                    requestUrl.openConnection() : requestUrl.openConnection(proxy));
            request.setRequestMethod(method);
            request.setDoOutput(false);
            request.setDoInput(true);
            request.setUseCaches(false);
            request.setConnectTimeout(config.getConnectionTimeOut());
            request.setReadTimeout(config.getResponseTimeOut());
            request.setInstanceFollowRedirects(false);
            // 设置userAgent
            if (!XStringUtil.isEmpty(config.getUserAgent()))
                request.addRequestProperty(XHttp.USER_AGENT, config.getUserAgent());
            // 设置Accept-Encoding为gizp
            if (isGzip()) {
                request.addRequestProperty(XHttp.ACCEPT_ENCODING, XHttp.GZIP);
            }
            // 设置用户自定义的http请求头
            if (mHeaders.size() > 0) {
                for (Map.Entry<String, String> header : mHeaders.entrySet())
                    request.setRequestProperty(header.getKey(), header.getValue());
            }
            return request;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建含有Body的请求，包括POST、PUT类型的请求。
     * @param method 请求类型
     * @return 返回HttpURLConnection请求对象
     */
    private HttpURLConnection createPostStyleRequest(String method, String url, XHttpConfig config) {
        HttpURLConnection request = null;
        try {
            Proxy proxy = getProxy(config);
            URL requestUrl = new URL(url);
            request = (HttpURLConnection) (proxy == null ?
                    requestUrl.openConnection() : requestUrl.openConnection(proxy));
            request.setRequestMethod(method);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (request != null) {
                request.disconnect();
            }
        }
        request.setDoOutput(true);
        request.setDoInput(true);
        request.setUseCaches(false);
        request.setConnectTimeout(config.getConnectionTimeOut());
        request.setReadTimeout(config.getResponseTimeOut());
        request.setInstanceFollowRedirects(false);
        // 设置userAgent
        if (!XStringUtil.isEmpty(config.getUserAgent()))
            request.addRequestProperty(XHttp.USER_AGENT, config.getUserAgent());
        // 设置Accept-Encoding为gzip
        if (isGzip()) {
            request.addRequestProperty(XHttp.ACCEPT_ENCODING, XHttp.GZIP);
        }
        // 设置用户自定义的http请求头
        if (mHeaders.size() > 0) {
            for (Map.Entry<String, String> header : mHeaders.entrySet())
                request.addRequestProperty(header.getKey(), header.getValue());
        }
        // 传输Post的内容
        OutputStream out = null;
        try {
            // 含有上传文件，Content-Type:multipart/form-data
            if (mFileParams.size() > 0) {
                // 设置ContentType
                String boundary = XJavaHttpUtil.generateBoundary();
                String contentType = XJavaHttpUtil.generateMultiContentType(boundary, getCharset());
                request.setRequestProperty(XHttp.CONTENT_TYPE, contentType);
                // 设置chunked模式
                if (isChunked()) {
                    request.setChunkedStreamingMode(0);// use default chunked size
                } else {
                    request.setFixedLengthStreamingMode(calTotalSize(boundary));
                }
                // 设置内容entity
                out = request.getOutputStream();
                writeFileParams(out, boundary);
                if (mStringParams.size() > 0) {
                    writeStringParams(out, boundary);
                    writeEndParams(out, boundary);
                }
                out.flush();
            }
            // 只有字符串参数，Content-Type:application/x-www-form-urlencoded
            else {
                // 设置ContentType
                String contentType = XJavaHttpUtil.generateStringContentType(getCharset());
                request.setRequestProperty(XHttp.CONTENT_TYPE, contentType);
                // 设置内容entity
                out = request.getOutputStream();
                String charset = getCharset() != null ? getCharset() : XHttp.DEF_CONTENT_CHARSET.name();
                String content = XJavaHttpUtil.format(mStringParams, charset);
                out.write(content.getBytes(charset));
                out.flush();
            }
            return request;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //普通字符串数据
    private void writeStringParams(OutputStream out, String boundary) throws IOException  {
        for (Map.Entry<String, String> entry : mStringParams.entrySet()) {
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);//2
            XJavaHttpUtil.writeBytes(boundary, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            XJavaHttpUtil.writeBytes(XHttp.CONTENT_DISPOSITION, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.FIELD_SEP, out);//2
            XJavaHttpUtil.writeBytes("form-data; name=\"", out);//17
            XJavaHttpUtil.writeBytes(entry.getKey(), out);
            XJavaHttpUtil.writeBytes("\"", out);//1
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            XJavaHttpUtil.writeBytes(entry.getValue(), out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
        }
    }

    //文件数据
    private void writeFileParams(OutputStream out, String boundary) throws IOException  {
        for (Map.Entry<String, File> entry : mFileParams.entrySet()) {
            File file = entry.getValue();
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);//2
            XJavaHttpUtil.writeBytes(boundary, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            XJavaHttpUtil.writeBytes(XHttp.CONTENT_DISPOSITION, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.FIELD_SEP, out);//2
            XJavaHttpUtil.writeBytes("form-data; name=\"", out);//17
            XJavaHttpUtil.writeBytes(entry.getKey(), out);
            XJavaHttpUtil.writeBytes("\"; filename=\"", out);//13
            XJavaHttpUtil.writeBytes(file.getName(), out);
            XJavaHttpUtil.writeBytes("\"", out);//1
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            XJavaHttpUtil.writeBytes(XHttp.CONTENT_TYPE, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.FIELD_SEP, out);//2
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.generateFileContentType(file), out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
            // [start]write file
            FileInputStream fis = new FileInputStream(file);
            byte[] buf = new byte[4096];
            int len;
            while ((len = fis.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            // [end]write file
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);//2
        }
    }

    //添加结尾数据
    private void writeEndParams(OutputStream out, String boundary) throws IOException {
        XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);
        XJavaHttpUtil.writeBytes(boundary, out);
        XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);
        XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
        XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
    }

    private long calTotalSize(String boundary) {
        long result = 0;
        final int fileDecorateSize = boundary.length() + XJavaHttpUtil.TWO_DASHES.length
                + 5*XJavaHttpUtil.CR_LF.length + XHttp.CONTENT_DISPOSITION.length()
                + 2*XJavaHttpUtil.FIELD_SEP.length + 31 + XHttp.CONTENT_TYPE.length();
        final int stringDecorateSize = boundary.length() + XJavaHttpUtil.TWO_DASHES.length
                + 4*XJavaHttpUtil.CR_LF.length + XHttp.CONTENT_DISPOSITION.length()
                + XJavaHttpUtil.FIELD_SEP.length + 18;
        final int endDecorateSize = boundary.length() + 2*XJavaHttpUtil.TWO_DASHES.length + 2*XJavaHttpUtil.CR_LF.length;
        for (Map.Entry<String, File> entry : mFileParams.entrySet()) {
            String key = entry.getKey();
            File file = entry.getValue();
            result = result + fileDecorateSize
                    + key.length()
                    + file.getName().length()
                    + XJavaHttpUtil.generateFileContentType(file).length()
                    + file.length();
        }
        for (Map.Entry<String, String> entry : mStringParams.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            result = result + stringDecorateSize
                    + key.length()
                    + value.length();
        }
        if (mFileParams.size() > 0 && mStringParams.size() >0) {
            result = result + endDecorateSize;
        }
        return result;
    }
}
