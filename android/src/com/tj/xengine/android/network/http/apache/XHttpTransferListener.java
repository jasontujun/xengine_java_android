package com.tj.xengine.android.network.http.apache;

/**
 * 对文件上传进度的监听者。
 */
public interface XHttpTransferListener {
    void transferred(long size);
}
