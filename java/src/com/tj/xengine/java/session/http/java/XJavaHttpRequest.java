package com.tj.xengine.java.session.http.java;

import com.tj.xengine.core.session.http.XBaseHttpRequest;
import com.tj.xengine.core.session.http.XHttp;
import com.tj.xengine.core.utils.XFileUtil;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
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

    protected HttpURLConnection toJavaHttpRequest() {
        return toJavaHttpRequest(null);
    }

    protected HttpURLConnection toJavaHttpRequest(Proxy proxy) {
        if (XStringUtil.isEmpty(getUrl()))
            return null;

        HttpURLConnection request = null;
        switch (getMethod()) {
            case GET:
                request = createGetStyleRequest("GET", proxy);
                break;
            case POST:
                request = createPostStyleRequest("POST", proxy);
                break;
            case PUT:
                request = createPostStyleRequest("PUT", proxy);
                break;
            case DELETE:
                request = createGetStyleRequest("DELETE", proxy);
                break;
        }
        return request;
    }

    /**
     * 创建不含Body的请求，包括GET、DELETE类型的请求。
     * @param method 请求类型
     * @return 返回HttpURLConnection请求对象
     */
    private HttpURLConnection createGetStyleRequest(String method, Proxy proxy) {
        try {
            URL requestUrl = new URL(getUrl());
            HttpURLConnection request = (HttpURLConnection) (proxy == null ?
                    requestUrl.openConnection() : requestUrl.openConnection(proxy));
            request.setRequestMethod(method);
            request.setDoOutput(false);
            request.setDoInput(true);
            request.setUseCaches(false);
            request.setConnectTimeout(getConfig().getConnectionTimeOut());
            request.setReadTimeout(getConfig().getResponseTimeOut());
            request.setInstanceFollowRedirects(false);
            // 设置userAgent
            if (!XStringUtil.isEmpty(getConfig().getUserAgent()))
                request.addRequestProperty(XHttp.USER_AGENT, getConfig().getUserAgent());
            // 设置Accept-Encoding为gizp
            if (isGzip()) {
                request.addRequestProperty(XHttp.ACCEPT_ENCODING, XHttp.GZIP);
            }
            // 设置用户自定义的http请求头
            if (mHeaders != null) {
                for (Map.Entry<String, String> header : mHeaders.entrySet())
                    request.setRequestProperty(header.getKey(), header.getValue());
            }
            return request;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建含有Body的请求，包括POST、PUT类型的请求。
     * @param method 请求类型
     * @return 返回HttpURLConnection请求对象
     */
    private HttpURLConnection createPostStyleRequest(String method, Proxy proxy) {
        try {
            URL requestUrl = new URL(getUrl());
            HttpURLConnection request = (HttpURLConnection) (proxy == null ?
                    requestUrl.openConnection() : requestUrl.openConnection(proxy));
            request.setRequestMethod(method);
            request.setDoOutput(true);
            request.setDoInput(true);
            request.setUseCaches(false);
            request.setConnectTimeout(getConfig().getConnectionTimeOut());
            request.setReadTimeout(getConfig().getResponseTimeOut());
            request.setInstanceFollowRedirects(false);
            // 设置userAgent
            if (!XStringUtil.isEmpty(getConfig().getUserAgent()))
                request.addRequestProperty(XHttp.USER_AGENT, getConfig().getUserAgent());
            // 设置Accept-Encoding为gizp
            if (isGzip()) {
                request.addRequestProperty(XHttp.ACCEPT_ENCODING, XHttp.GZIP);
            }
            // 设置用户自定义的http请求头
            if (mHeaders != null) {
                for (Map.Entry<String, String> header : mHeaders.entrySet())
                    request.addRequestProperty(header.getKey(), header.getValue());
            }
            // 含有上传文件，Content-Type:multipart/form-data
            if (mFileParams != null) {
                // 设置ContentType
                String boundary = XJavaHttpUtil.generateBoundary();
                String contentType = XJavaHttpUtil.generateMultiContentType(boundary, getCharset());
                request.setRequestProperty(XHttp.CONTENT_TYPE, contentType);
                // 设置内容entity
                OutputStream out = request.getOutputStream();
                writeFileParams(out, boundary);
                if (mStringParams != null)
                    writeStringParams(out, boundary);
                if (mFileParams != null && mStringParams != null)
                    paramsEnd(out, boundary);
                out.flush();
                out.close();
            }
            // 只有字符串参数，Content-Type:application/x-www-form-urlencoded
            else {
                // 设置ContentType
                String contentType = XJavaHttpUtil.generatePostStringContentType(getCharset());
                request.setRequestProperty(XHttp.CONTENT_TYPE, contentType);
                // 设置内容entity
                OutputStream out = request.getOutputStream();
                String charset = getCharset() != null ? getCharset() : XHttp.DEF_CONTENT_CHARSET.name();
                String content = XJavaHttpUtil.format(mStringParams, charset);
                out.write(content.getBytes(charset));
                out.flush();
                out.close();
            }

            return request;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    //普通字符串数据
    private void writeStringParams(OutputStream out, String boundary) {
        try {
            for (Map.Entry<String, String> entry : mStringParams.entrySet()) {
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);
                XJavaHttpUtil.writeBytes(boundary, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                XJavaHttpUtil.writeBytes(XHttp.CONTENT_DISPOSITION, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.FIELD_SEP, out);
                XJavaHttpUtil.writeBytes("form-data; name=\"", out);
                XJavaHttpUtil.writeBytes(entry.getKey(), out);
                XJavaHttpUtil.writeBytes("\"", out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                XJavaHttpUtil.writeBytes(entry.getValue(), out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //文件数据
    private void writeFileParams(OutputStream out, String boundary) {
        try {
            for (Map.Entry<String, File> entry : mFileParams.entrySet()) {
                File file = entry.getValue();
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);
                XJavaHttpUtil.writeBytes(boundary, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                XJavaHttpUtil.writeBytes(XHttp.CONTENT_DISPOSITION, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.FIELD_SEP, out);
                XJavaHttpUtil.writeBytes("form-data; name=\"", out);
                XJavaHttpUtil.writeBytes(entry.getKey(), out);
                XJavaHttpUtil.writeBytes("\"; filename=\"", out);
                XJavaHttpUtil.writeBytes(file.getName(), out);
                XJavaHttpUtil.writeBytes("\"", out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                XJavaHttpUtil.writeBytes(XHttp.CONTENT_TYPE, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.FIELD_SEP, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.generatePostFileContentType(file), out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
                out.write(XFileUtil.file2byte(file));
                XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //添加结尾数据
    private void paramsEnd(OutputStream out, String boundary) {
        try {
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);
            XJavaHttpUtil.writeBytes(boundary, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.TWO_DASHES, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
            XJavaHttpUtil.writeBytes(XJavaHttpUtil.CR_LF, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
