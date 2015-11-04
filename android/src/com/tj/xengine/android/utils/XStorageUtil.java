package com.tj.xengine.android.utils;

import android.os.StatFs;
import android.text.TextUtils;
import com.tj.xengine.core.utils.XFileUtil;

import java.io.File;

/**
 * �豸�洢������صĹ����ࡣ
 * Created by tujun on 2014/7/22.
 */
public abstract class XStorageUtil {

    private static final String TAG = XStorageUtil.class.getSimpleName();

    /**
     * �ж��ļ����ڵĴ��̴洢�ռ��Ƿ�������
     * ʣ��ռ��Ƿ�С�ڵ���minSize��
     * ���·�������ڣ����ݹ�����ϼ��ļ����жϣ�ֱ�����ϲ�ĸ�Ŀ¼��
     * @param path �ļ��ľ���·��
     * @return ���·�������ڣ�����false��һ�������������������true������false
     */
    public static boolean isFull(String path) {
        return isFull(path, 0);
    }

    /**
     * �ж��ļ����ڵĴ��̴洢�ռ��Ƿ�������
     * ʣ��ռ��Ƿ�С�ڵ���minSize��
     * ���·�������ڣ����ݹ�����ϼ��ļ����жϣ�ֱ�����ϲ�ĸ�Ŀ¼��
     * @param path �ļ��ľ���·��
     * @param minSize ʣ���������ٽ�ֵ��С�ڵ��ڴ�ֵ��Ϊ�洢�ռ�����
     * @return ���·�������ڣ�����false��һ�������������������true������false
     */
    public static boolean isFull(String path, long minSize) {
        long[] volume = getSdVolume(path);
        if (volume == null)// ���·�������ڣ�����false
            return false;

        long remainSize = volume[0];
        return remainSize <= minSize;
    }


    /**
     * ��ȡָ��Ŀ¼���ڵĴ洢�豸�����������
     * @param path ָ��·��
     * @return ����long������{ʣ��������������}��
     * ���ָ��·���������ļ��ж������ڣ�����null
     */
    public static long[] getSdVolume(String path) {
        File file = new File(path);
        // ������������ļ��У��򱾼��ļ��в����ڣ�������һ���ļ���
        while (file != null && (!file.exists() || !file.isDirectory())) {
            file = file.getParentFile();
        }
        if (file == null || !file.exists()) {// ���·�������ڣ�����
            return null;
        }

        String testPath = file.getAbsolutePath();
        StatFs statFs = new StatFs(testPath);
        long blockSize = statFs.getBlockSize();
        long blocksCount = statFs.getBlockCount();
        long residueBlocksCount = statFs.getAvailableBlocks();
        long remainSize = residueBlocksCount * blockSize;// ʣ������
        long totalSize = blocksCount * blockSize;// ������
        return new long[]{remainSize, totalSize};
    }


    /**
     * ����ĳ��·����IO��д�����Ƿ�������
     * �½�һ��test.dat�ļ���дһ�����ݣ��ٶ��������Ƚϣ����ɾ����
     * �����һϵ�в������ɹ������ʾ����������������
     * @param path ���Զ�д��·��
     * @return ���IO����������true�����򷵻�false
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
            // д���ݽ��ļ�
            if (!XFileUtil.string2File(testStr, testFile))
                return false;
            // ���ļ��ж�����
            String readStr = XFileUtil.file2String(testFile);
            if (readStr == null)
                return false;
            // �Ƚ��ַ�������
            if (!testStr.equals(readStr))
                return false;
        } finally {
            // ɾ�������ļ�
            File renameFile = new File(testFile.getAbsolutePath() + System.currentTimeMillis());
            boolean renameResult = testFile.renameTo(renameFile);// Ϊ�˽��EBUSY���⣬����������ɾ��
            if (renameResult)
                testFile = renameFile;
            deleteResult = testFile.delete();
        }
        return deleteResult;
    }
}
