package com.tj.xengine.core.session.http.handler;

import com.tj.xengine.core.session.http.XHttp;
import com.tj.xengine.core.session.http.XHttpResponse;
import com.tj.xengine.core.session.http.XHttpUtil;
import com.tj.xengine.core.utils.XFileUtil;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.*;
import java.util.List;

/**
 * Created by jason on 2015/10/28.
 */
public class XHttpFileHandler implements XHttpHandler<File> {

    private boolean mOverride;// 是否重复覆盖
    private String mUrl;
    private String mFolder;
    private String mFileName;

    public XHttpFileHandler(boolean override, String url, String folder) {
        mOverride = override;
        mUrl = url;
        mFolder = folder;
    }

    public XHttpFileHandler(boolean override, String url, String folder, String fileName) {
        mOverride = override;
        mUrl = url;
        mFolder = folder;
        mFileName = fileName;
    }

    @Override
    public File handleResponse(XHttpResponse response) {
        if (response == null)
            return null;

        InputStream is = response.getContent();
        if (is == null)
            return null;

        // 如果文件名未指定，则从url和Content-Disposition中获取文件名
        if (XStringUtil.isEmpty(mFileName)) {
            List<String> cdHeader = response.getHeader(XHttp.CONTENT_DISPOSITION);
            String cd = (cdHeader != null && cdHeader.size() > 0) ? cdHeader.get(0) : null;
            mFileName = XHttpUtil.parseFileNameFromHttp(mUrl, cd);
            // 如果从http请求和响应中获取不到文件名，则用一个32位的随机字符串作为文件名
            if (XStringUtil.isEmpty(mFileName)) {
                mFileName = XStringUtil.getRandomString(32);
            }
        }

        // 如果文件夹不存在，则创建
        File dir = new File(mFolder);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 如果文件已存在，且不能覆盖，则换成另一个文件名
        File file = new File(mFolder, mFileName);
        if (!mOverride && file.exists()) {
            String appendSuffix = "_" + System.currentTimeMillis();
            int dotIndex = mFileName.lastIndexOf(".");
            if (dotIndex != -1) {
                String baseName = mFileName.substring(0, dotIndex);
                String suffix = mFileName.substring(dotIndex + 1);
                mFileName = baseName + appendSuffix + "." + suffix;
            } else {
                mFileName = mFileName + appendSuffix;
            }
            file = new File(mFolder, mFileName);
        }

        // 写文件
        if (!XFileUtil.stream2File(is, file)) {
            file.delete();
            response.consumeContent();
            return null;
        } else {
            response.consumeContent();
            return file;
        }
    }
}
