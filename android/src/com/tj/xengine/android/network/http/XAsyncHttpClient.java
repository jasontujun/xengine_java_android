package com.tj.xengine.android.network.http;

import android.os.AsyncTask;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.network.http.XHttpRequest;
import com.tj.xengine.core.network.http.XHttpResponse;
import com.tj.xengine.core.network.http.handler.XHttpHandler;

/**
 * Created by jasontujun on 2016/4/19.
 */
public class XAsyncHttpClient implements XAsyncHttp {

    private XHttp mHttpClient;

    public XAsyncHttpClient(XHttp httpClient) {
        mHttpClient = httpClient;
    }

    @Override
    public XHttpRequest newRequest(String url) {
        return mHttpClient.newRequest(url);
    }

    @Override
    public XHttpRequest newRequest(String url, XHttpRequest.Method method) {
        return mHttpClient.newRequest(url).setMethod(method);
    }

    @Override
    public AsyncTask execute(String url, XHttpRequest.Method method, Listener listener) {
        return execute(newRequest(url, method), listener);
    }

    @Override
    public AsyncTask execute(final XHttpRequest request, final Listener listener) {
        return execute(request, null, listener);
    }

    @Override
    public <T> AsyncTask execute(String url, XHttpRequest.Method method,
                            XHttpHandler<T> handler, Listener<T> listener) {
        return execute(newRequest(url, method), handler, listener);
    }

    @Override
    public <T> AsyncTask execute(final XHttpRequest request,
                            final XHttpHandler<T> handler,
                            final Listener<T> listener) {
        if (request == null)
            return null;
        return new AsyncTask<Void, Void, XHttpResponse>() {
            private T handlerResult;
            @Override
            protected XHttpResponse doInBackground(Void... param) {
                XHttpResponse res = mHttpClient.execute(request);
                if (isCancelled()) {
                    return null;
                }
                // 在异步线程中调用XHttpHandler
                if (handler != null && res != null &&
                        200 <= res.getStatusCode() && res.getStatusCode() < 300) {
                    handlerResult = handler.handleResponse(res);
                }
                return res;
            }

            @Override
            protected void onPostExecute(XHttpResponse result) {
                if (listener != null) {
                    if (result == null) {
                        listener.onNetworkError();
                    } else {
                        if (200 <= result.getStatusCode() && result.getStatusCode() < 300) {
                            listener.onFinishSuccess(result, handlerResult);
                        } else {
                            listener.onFinishError(result);
                        }
                    }
                }
            }

            @Override
            protected void onCancelled(XHttpResponse result) {
                if (listener != null)
                    listener.onCancelled();
            }
        }.execute();
    }
}
