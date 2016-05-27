package com.tj.xengine.android.network.download;

import com.tj.xengine.android.utils.XLog;
import com.tj.xengine.android.utils.XStorageUtil;
import com.tj.xengine.core.network.download.XDownloadBean;
import com.tj.xengine.core.network.download.XBaseHttpDownloadTask;
import com.tj.xengine.core.network.http.XHttp;

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
        XLog.d(TAG, message);
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
