package com.tj.xengine.android.network.download;

import com.tj.xengine.core.network.download.XBaseHttpDownloader;
import com.tj.xengine.core.network.download.XDownloadBean;
import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.toolkit.taskmgr.XMgrTaskExecutor;
import com.tj.xengine.core.toolkit.taskmgr.XTaskMgr;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgr;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialMgrImpl;
import com.tj.xengine.core.toolkit.taskmgr.serial.XSerialSpeedMonitor;

public class XHttpDownloader extends XBaseHttpDownloader {

    public XHttpDownloader(XHttp http) {
        super(http);
    }

    @Override
    protected XTaskMgr<XMgrTaskExecutor<XDownloadBean>, XDownloadBean> createTaskMgr() {
        XSerialMgr<XDownloadBean> taskMgr = new XSerialMgrImpl<XDownloadBean>();
        taskMgr.setSpeedMonitor(new XSerialSpeedMonitor<XDownloadBean>(taskMgr));
        return taskMgr;
    }

    @Override
    protected XMgrTaskExecutor<XDownloadBean> createTask(XDownloadBean bean) {
        return new XHttpDownloadTask(bean, mHttpClient);
    }
}
