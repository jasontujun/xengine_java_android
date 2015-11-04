package com.tj.xengine.java.session.http.apache;

/**
 * 对文件上传进度的监听者。
 */
public interface XHttpTransferListener {
    void transferred(long size);
}
