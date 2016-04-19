package com.tj.xengine.android.network.http.handler;

import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.network.http.handler.XHttpHandler;
import com.tj.xengine.core.utils.XStringUtil;
import org.json.JSONObject;

import java.io.InputStream;

/**
 * Created by jason on 2016/4/19.
 */
public class XJsonObjectHandler implements XHttpHandler<JSONObject> {
    @Override
    public JSONObject handleResponse(XHttpResponse response) {
        if (response == null)
            return null;

        try {
            InputStream inputStream = response.getContent();
            if (inputStream == null)
                return null;

            String str = XStringUtil.stream2String(inputStream, response.getContentType());
            if (XStringUtil.isEmpty(str))
                return null;

            return new JSONObject(str);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            response.consumeContent();
        }
    }
}
