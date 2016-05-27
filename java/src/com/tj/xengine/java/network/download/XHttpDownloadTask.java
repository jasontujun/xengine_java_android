package com.tj.xengine.java.network.download;

import com.tj.xengine.core.network.download.XBaseHttpDownloadTask;
import com.tj.xengine.core.network.download.XDownloadBean;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.java.utils.XLog;
import com.tj.xengine.java.utils.XStorageUtil;

public class XHttpDownloadTask extends XBaseHttpDownloadTask {

    private static final String TAG = XHttpDownloadTask.class.getSimpleName();

    public XHttpDownloadTask(XDownloadBean bean, XHttp httpClient) {
        super(bean, httpClient);
    }

    public XHttpDownloadTask(XDownloadBean bean, int status, XHttp httpClient) {
        super(bean, status, httpClient);
    }

    @Override
    protected void log(String message) {
        XLog.log(TAG, message);
    }

    @Override
    protected boolean checkStorageFull(String dir, long minSize) {
        return XStorageUtil.isFull(dir, minSize);
    }

    @Override
    protected boolean checkIO(String dir) {
        return XStorageUtil.isIOWorks(dir);
    }
}
