package com.tj.xengine.android.utils;

import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.util.Locale;

/**
 * <pre>
 * 设备相关工具类
 * User: jasontujun
 * Date: 14-5-26
 * Time: 下午3:37
 * </pre>
 */
public abstract class XDeviceUtil {

    /**
     * 获取代表手机的唯一Id。
     * 1.优先获取手机的IMEI号；
     * 2.如果手机没有IMEI号，则把手机的Mac地址作为唯一Id;
     * 3.如果手机Mac地址也为空，则返回16位长随机字符串；
     * @param context
     * @return 返回手机唯一串号，不会为空
     */
    public static String getDeviceId(Context context) {
        String imei = null;
        // 获取手机IMEI号
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null)
            imei = tm.getDeviceId();
        // 如果imei号为空，则获取手机的Mac地址
        if (TextUtils.isEmpty(imei)) {
            String macStr = XNetworkUtil.getMacAddress(context);
            if (!TextUtils.isEmpty(macStr))
                imei = macStr.replace(".", "").replace(":", "")
                    .replace("-", "").replace("_", "");
        }
        // 如果手机的Mac地址为空，则获取16位长随机字符串
        if (TextUtils.isEmpty(imei)) {
            imei = XStringUtil.getRandomString(16);
        }
        return imei;
    }

    public static String getOS() {
        return "Android";
    }

    public static String getOSVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getDevice() {
        return Build.MODEL;
    }

    public static String getBrand(){
        return Build.BRAND;
    }

    public static String getCPUInfo() {
        RandomAccessFile reader = null;
        try {
            byte[] bs = new byte[1024];
            reader = new RandomAccessFile("/proc/cpuinfo", "r");
            reader.read(bs);
            String ret = new String(bs);
            int index = ret.indexOf(0);
            return index != -1 ? ret.substring(0, index) : ret;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 获取手机相关信息
     * @return
     */
    public static String getMobileInfo() {
        StringBuffer sb = new StringBuffer();
        try {

            Field[] fields = Build.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                String name = field.getName();
                String value = field.get(null).toString();
                sb.append(name + "=" + value);
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 获取屏幕分辨率
     * @param context
     * @return 分辨率结果为“宽x高”；如果无法获取，返回null
     */
    public static String getResolution(Context context) {
        WindowManager wm = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        if (wm == null)
            return null;

        Display display = wm.getDefaultDisplay();
        if (display == null)
            return null;

        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.widthPixels + "x" + metrics.heightPixels;
    }

    /**
     * 获取屏幕的宽度
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    /**
     * 获取屏幕的高度
     * @param context
     * @return
     */
    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    /**
     * 获取手机的运营商
     * @param context
     * @return
     */
    public static String getCarrier(Context context) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        return tm == null ? null : tm.getNetworkOperatorName();
    }

    /**
     * 获取手机的语言
     * @return
     */
    public static String getLanguage() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage();
    }

    /**
     * 获取手机所属的国家
     * @return
     */
    public static String getCountry() {
        Locale locale = Locale.getDefault();
        return locale.getCountry();
    }

    /**
     * 获取手机的字符集
     * @return
     */
    public static String getCharset() {
        return System.getProperty("file.encoding");
    }
}
