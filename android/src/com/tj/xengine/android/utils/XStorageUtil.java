package com.tj.xengine.android.utils;

import android.os.StatFs;
import android.text.TextUtils;
import com.tj.xengine.core.utils.XFileUtil;

import java.io.File;

/**
 * 设备存储容量相关的工具类。
 * Created by tujun on 2014/7/22.
 */
public abstract class XStorageUtil {

    private static final String TAG = XStorageUtil.class.getSimpleName();

    /**
     * 判断文件所在的磁盘存储空间是否已满。
     * 剩余空间是否小于等于minSize。
     * 如果路径不存在，则会递归的向上级文件夹判断，直到最上层的根目录。
     * @param path 文件的绝对路径
     * @return 如果路径不存在，返回false。一般情况，容量已满返回true，否则false
     */
    public static boolean isFull(String path) {
        return isFull(path, 0);
    }

    /**
     * 判断文件所在的磁盘存储空间是否已满。
     * 剩余空间是否小于等于minSize。
     * 如果路径不存在，则会递归的向上级文件夹判断，直到最上层的根目录。
     * @param path 文件的绝对路径
     * @param minSize 剩余容量的临界值，小于等于此值认为存储空间已满
     * @return 如果路径不存在，返回false。一般情况，容量已满返回true，否则false
     */
    public static boolean isFull(String path, long minSize) {
        long[] volume = getSdVolume(path);
        if (volume == null)// 如果路径不存在，返回false
            return false;

        long remainSize = volume[0];
        return remainSize <= minSize;
    }


    /**
     * 获取指定目录所在的存储设备的容量情况。
     * @param path 指定路径
     * @return 返回long型数组{剩余容量，总容量}；
     * 如果指定路径的所有文件夹都不存在，返回null
     */
    public static long[] getSdVolume(String path) {
        File file = new File(path);
        // 如果本级不是文件夹，或本级文件夹不存在，跳到上一级文件夹
        while (file != null && (!file.exists() || !file.isDirectory())) {
            file = file.getParentFile();
        }
        if (file == null || !file.exists()) {// 如果路径不存在，结束
            return null;
        }

        String testPath = file.getAbsolutePath();
        StatFs statFs = new StatFs(testPath);
        long blockSize = statFs.getBlockSize();
        long blocksCount = statFs.getBlockCount();
        long residueBlocksCount = statFs.getAvailableBlocks();
        long remainSize = residueBlocksCount * blockSize;// 剩余容量
        long totalSize = blocksCount * blockSize;// 总容量
        return new long[]{remainSize, totalSize};
    }


    /**
     * 测试某个路径下IO读写操作是否正常。
     * 新建一个test.dat文件，写一段数据，再读出来，比较，最后删掉。
     * 如果这一系列操作都成功，则表示正常，否则不正常。
     * @param path 测试读写的路径
     * @return 如果IO正常，返回true；否则返回false
     */
    public static boolean isIOWorks(String path) {
        if (TextUtils.isEmpty(path))
            return false;

        File dir = new File(path);
        if (!dir.exists())
            dir.mkdirs();
        File testFile = new File(dir, "test.dat");
        String testStr = "test";
        boolean deleteResult;
        try {
            // 写数据进文件
            if (!XFileUtil.string2File(testStr, testFile))
                return false;
            // 从文件中读数据
            String readStr = XFileUtil.file2String(testFile);
            if (readStr == null)
                return false;
            // 比较字符串内容
            if (!testStr.equals(readStr))
                return false;
        } finally {
            // 删除测试文件
            File renameFile = new File(testFile.getAbsolutePath() + System.currentTimeMillis());
            boolean renameResult = testFile.renameTo(renameFile);// 为了解决EBUSY问题，重命名后再删除
            if (renameResult)
                testFile = renameFile;
            deleteResult = testFile.delete();
        }
        return deleteResult;
    }
}
