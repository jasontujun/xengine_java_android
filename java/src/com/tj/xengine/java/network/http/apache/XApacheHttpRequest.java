package com.tj.xengine.java.network.http.apache;

import com.tj.xengine.core.network.http.XBaseHttpRequest;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpConfig;
import com.tj.xengine.core.utils.XStringUtil;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 用于XApacheHttpClient的XHttpRequest实现类。
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-3
 * Time: 下午6:13
 */
class XApacheHttpRequest extends XBaseHttpRequest {

    protected XApacheHttpRequest() {
        super();
    }

    protected HttpRequest toApacheHttpRequest() {
        if (XStringUtil.isEmpty(getUrl()))
            return null;
        switch (getMethod()) {
            case GET:
                return createGetStyleRequest(new HttpGet(getUrl()));
            case POST:
                return createPostStyleRequest(new HttpPost(getUrl()));
            case PUT:
                return createPostStyleRequest(new HttpPut(getUrl()));
            case DELETE:
                return createGetStyleRequest(new HttpDelete(getUrl()));
            default:
                return null;
        }
    }

    private HttpRequest createGetStyleRequest(HttpRequestBase request) {
        // 设置只作用于该请求的RequestConfig
        XHttpConfig localConfig = getConfig();
        if (localConfig != null) {
            request.setConfig(XApacheHttpClient.toRequestConfig(localConfig));
        }
        // 设置Accept-Encoding为gizp
        if (isGzip()) {
            request.addHeader(XHttp.ACCEPT_ENCODING, XHttp.GZIP);
        }
        // 设置用户自定义的http请求头
        if (mHeaders.size() > 0) {
            for (Map.Entry<String, String> header : mHeaders.entrySet())
                request.setHeader(header.getKey(), header.getValue());
        }
        return request;
    }

    private HttpRequest createPostStyleRequest(HttpEntityEnclosingRequestBase request) {
        // 设置用户自定义的http请求头
        createGetStyleRequest(request);
        // 含有上传文件
        if (mFileParams.size() > 0) {
            MultipartEntityBuilder reqEntityBuilder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            for (Map.Entry<String, File> fileParam : mFileParams.entrySet()) {
                reqEntityBuilder.addPart(fileParam.getKey(), new FileBody(fileParam.getValue()));
            }
            if (mStringParams.size() > 0) {
                for (Map.Entry<String, String> strParam : mStringParams.entrySet()) {
                    if (getCharset() != null) {
                        Charset charset = null;
                        try {
                            charset = Charset.forName(getCharset());
                        } catch (UnsupportedCharsetException e) {
                            e.printStackTrace();
                        }
                        reqEntityBuilder.addPart(strParam.getKey(),
                                new StringBody(strParam.getValue(), ContentType.create("text/plain", charset)));
                    } else {
                        reqEntityBuilder.addPart(strParam.getKey(),
                                new StringBody(strParam.getValue(), ContentType.TEXT_PLAIN));
                    }
                }
            }
            request.setEntity(reqEntityBuilder.build());
        }
        // 不含上传文件，只有字符串参数
        else {
            if (mStringParams.size() > 0) {
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                for (Map.Entry<String, String> strParam : mStringParams.entrySet())
                    params.add(new BasicNameValuePair(strParam.getKey(), strParam.getValue()));
                try {
                    if (getCharset() != null)
                        request.setEntity(new UrlEncodedFormEntity(params, getCharset()));
                    else
                        request.setEntity(new UrlEncodedFormEntity(params));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return request;
    }
}
