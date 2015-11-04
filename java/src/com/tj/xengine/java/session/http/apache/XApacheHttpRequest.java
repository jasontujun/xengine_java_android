package com.tj.xengine.java.session.http.apache;

import com.tj.xengine.core.session.http.XBaseHttpRequest;
import com.tj.xengine.core.session.http.XHttp;
import com.tj.xengine.core.utils.XStringUtil;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
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

    private XHttpTransferListener mListener;

    protected XApacheHttpRequest() {
        super();
    }

    protected XApacheHttpRequest setListener(XHttpTransferListener listener) {
        this.mListener = listener;
        return this;
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

    private HttpRequest createGetStyleRequest(HttpRequest request) {
        // 设置Accept-Encoding为gizp
        if (isGzip()) {
            request.addHeader(XHttp.ACCEPT_ENCODING, XHttp.GZIP);
        }
        // 设置用户自定义的http请求头
        if (mHeaders != null) {
            for (Map.Entry<String, String> header : mHeaders.entrySet())
                request.setHeader(header.getKey(), header.getValue());
        }
        return request;
    }

    private HttpRequest createPostStyleRequest(HttpEntityEnclosingRequest request) {
        // 设置用户自定义的http请求头
        createGetStyleRequest(request);
        // 含有上传文件
        if (mFileParams != null) {
            XMultipartEntity reqEntity = new XMultipartEntity
                    (HttpMultipartMode.BROWSER_COMPATIBLE, mListener);
            try {
                for (Map.Entry<String, File> fileParam : mFileParams.entrySet())
                    reqEntity.addPart(fileParam.getKey(), new FileBody(fileParam.getValue()));
                if (mStringParams != null) {
                    for (Map.Entry<String, String> strParam : mStringParams.entrySet()) {
                        if (getCharset() != null)
                            reqEntity.addPart(strParam.getKey(),
                                    new StringBody(strParam.getValue(), Charset.forName(getCharset())));
                        else
                            reqEntity.addPart(strParam.getKey(),
                                    new StringBody(strParam.getValue()));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            request.setEntity(reqEntity);
        }
        // 不含上传文件，只有字符串参数
        else {
            if (mStringParams != null) {
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
