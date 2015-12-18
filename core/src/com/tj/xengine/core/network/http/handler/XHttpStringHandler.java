package com.tj.xengine.core.network.http.handler;

import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.InputStream;

/**
 * Created by jason on 2015/10/28.
 */
public class XHttpStringHandler implements XHttpHandler<String> {

    @Override
    public String handleResponse(XHttpResponse response) {
        if (response == null)
            return null;

        InputStream inputStream = response.getContent();
        if (inputStream == null)
            return null;

        String str = XStringUtil.stream2String(inputStream, response.getContentType());
        response.consumeContent();
        return str;
    }
}
