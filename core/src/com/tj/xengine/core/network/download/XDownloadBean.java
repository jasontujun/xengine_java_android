package com.tj.xengine.core.network.download;

import com.tj.xengine.core.toolkit.task.XTaskBean;

import java.io.Serializable;

/**
 * Created by jasontujun on 2015/10/29.
 */
public class XDownloadBean implements XTaskBean, Serializable {

    private static final long serialVersionUID = -7276732366418609011L;

    private String url;
    private String folder;
    private String fileName;
    private String downloadingSuffix;

    private int status;
    private int type;
    private long totalSize;

    public XDownloadBean() {
    }

    public XDownloadBean(String url, String folder) {
        this.url = url;
        this.folder = folder;
    }

    public XDownloadBean(String url, String folder, String fileName) {
        this.url = url;
        this.folder = folder;
        this.fileName = fileName;
    }

    @Override
    public String getId() {
        return url;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getDownloadingSuffix() {
        return downloadingSuffix;
    }

    public void setDownloadingSuffix(String downloadingSuffix) {
        this.downloadingSuffix = downloadingSuffix;
    }
}
