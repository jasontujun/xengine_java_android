package com.tj.xengine.core.network.http;

import com.tj.xengine.core.network.http.handler.XHttpHandler;

/**
 * 异步Http客户端接口。
 * 简化了XHttp通信的调用方式：在主线程调用XAsyncHttp的接口，
 * 实际是在异步线程执行http请求，并返回主线程执行XAsyncHttp.Listener的相关回调。
 * Created by jasontujun on 2016/4/19.
 */
public interface XAsyncHttp {

    interface Listener<T> {
        void onNetworkError();

        void onFinishError(XHttpResponse response);

        void onFinishSuccess(XHttpResponse response, T result);
    }

    /**
     * 创建一个Http请求对象(默认为GET请求)。
     * @param url http请求的url
     * @return 返回http请求对象。
     */
    XHttpRequest newRequest(String url);

    /**
     * 创建一个指定类型的Http请求对象。
     * @param url http请求的url
     * @param method http请求的类型
     * @return 返回http请求对象。
     */
    XHttpRequest newRequest(String url, XHttpRequest.Method method);

    /**
     * 执行http请求。
     * @param url http请求的url
     * @param method http请求的类型
     * @param listener http执行结果的回调
     */
    void execute(String url, XHttpRequest.Method method, Listener listener);

    /**
     * 执行http请求。
     * @param request http请求对象
     * @param listener http执行结果的回调
     */
    void execute(XHttpRequest request, Listener listener);

    /**
     * 执行http请求。
     * @param url http请求的url
     * @param method http请求的类型
     * @param handler http结果的处理器
     * @param listener http执行结果的回调
     * @param <T> http执行结果的类型，取决于XHttpHandler的类型。
     */
    <T> void execute(String url, XHttpRequest.Method method,
                           XHttpHandler<T> handler, Listener<T> listener);

    /**
     * 执行http请求。
     * @param request http请求对象
     * @param handler http结果的处理器
     * @param listener http执行结果的回调
     * @param <T> http执行结果的类型，取决于XHttpHandler的类型。
     */
    <T> void execute(XHttpRequest request, XHttpHandler<T> handler, Listener<T> listener);
}
