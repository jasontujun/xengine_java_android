package com.tj.xengine.android.network.http.apache;

import com.tj.xengine.core.network.http.XBaseHttpResponse;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.utils.XStringUtil;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-6
 * Time: 下午7:55
 * To change this template use File | Settings | File Templates.
 */
class XApacheHttpResponse extends XBaseHttpResponse {

    private HttpEntity mEntity;

    protected void setEntity(HttpEntity entity) {
        mEntity = entity;
    }

    @Override
    public void consumeContent() {
        super.consumeContent();
        if (mEntity != null)
            try {
                mEntity.consumeContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public Charset getContentType() {
        if (mEntity == null)
            return null;

        Header header = mEntity.getContentType();
        if (header == null)
            return null;
        HeaderElement[] elements = header.getElements();
        if (elements.length == 0)
            return null;
        HeaderElement ctElement = elements[0];
        NameValuePair param = ctElement.getParameterByName("charset");
        if (param == null)
            return null;
        // 获取字符编码
        String charsetName = param.getValue();
        Charset charset = !XStringUtil.isEmpty(charsetName) ? Charset.forName(charsetName) : null;
        // 如果解析失败，则使用默认编码
        if (charset == null)
            charset = XHttp.DEF_CONTENT_CHARSET;

        return charset;
    }
}
