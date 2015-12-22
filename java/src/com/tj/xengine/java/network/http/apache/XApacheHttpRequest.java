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
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
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
                File file = fileParam.getValue();
                ContentBody partBody = null;
                if (isChunked()) {
                    try {
                        partBody = new InputStreamBody(new FileInputStream(file),
                                generateFileContentType(file), file.getName());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return null;// error! cannot create request because cannot open file
                    }
                } else {
                    partBody = new FileBody(file, generateFileContentType(file));
                }
                reqEntityBuilder.addPart(fileParam.getKey(), partBody);
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

    /**
     * 生成Post类型带文件内容ContentType的值。
     * 图片格式为image/png,image/jpg等。
     * 音频格式为audio/mpeg,audio/mp4等。
     * 默认为application/octet-stream。
     * @param f 文件
     * @return 返回对应的ContentType
     */
    private static ContentType generateFileContentType(File f) {
        int dotIndex = f.getAbsolutePath().lastIndexOf(".");
        if (dotIndex < 0) {
            return ContentType.DEFAULT_BINARY;
        }

        String suffix = f.getAbsolutePath().substring(dotIndex).toLowerCase();
        if ("jpg".equals(suffix) || "jpeg".equals(suffix))
            return ContentType.create("image/jpeg");
        else if ("png".equals(suffix) || "gif".equals(suffix))
            return ContentType.create("image/" + suffix);
        else if ("mp3".equals(suffix) || "mpeg".equals(suffix))
            return ContentType.create("audio/mpeg");
        else if ("mp4".equals(suffix) || "ogg".equals(suffix))
            return ContentType.create("audio/" + suffix);
        else
            return ContentType.DEFAULT_BINARY;
    }
}
