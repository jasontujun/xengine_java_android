package com.tj.xengine.android.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

/**
 * 网络辅助类
 * Created by 赵之韵.
 * Modified by jasontujun
 * Date: 12-2-29
 * Time: 下午6:56
 */
public abstract class XNetworkUtil {

    /**
     * 当前网络是否可用
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr == null)
            return false;
        NetworkInfo info = mgr.getActiveNetworkInfo();
        return info != null && info.isAvailable();
    }

    /**
     * 当前网络是否已连接或正在连接
     * @param context
     * @return
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr == null)
            return false;
        NetworkInfo info = mgr.getActiveNetworkInfo();
        return info != null && info.isConnectedOrConnecting();
    }

    /**
     * Wifi是否打开
     * @param context
     * @return
     */
    public static boolean isWifiEnable(Context context) {
        WifiManager mgr = (WifiManager) context.
                getSystemService(Context.WIFI_SERVICE);
        if (mgr == null)
            return false;
        int state = mgr.getWifiState();
        return state == WifiManager.WIFI_STATE_ENABLED ||
                state ==  WifiManager.WIFI_STATE_ENABLING;
    }

    /**
     * Wifi是否已连接或正在连接
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr == null)
            return false;
        NetworkInfo info = mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (info == null)
            return false;
        NetworkInfo.State state = info.getState();
        return state == NetworkInfo.State.CONNECTED ||
                state == NetworkInfo.State.CONNECTING;
    }

    /**
     * 数据流量是否已连接或正在连接
     * @param context
     * @return
     */
    public static boolean isMobileConnected(Context context) {
        ConnectivityManager mgr = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mgr == null)
            return false;
        NetworkInfo info = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (info == null)
            return false;
        NetworkInfo.State state = info.getState();
        return state == NetworkInfo.State.CONNECTED ||
                state == NetworkInfo.State.CONNECTING;
    }

    /**
     * 返回GPS定位是否开启
     * @param context
     * @return
     */
    public static boolean isGPSLocation(Context context) {
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * 返回Wifi定位是否开启
     * @param context
     * @return
     */
    public static boolean isNetworkLocation(Context context) {
        LocationManager locationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * 获取手机的Mac地址
     * @param context
     * @return 返回手机的Mac地址；如果无法获取，返回null;
     */
    public static String getMacAddress(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm == null)
            return null;

        WifiInfo info = wm.getConnectionInfo();
        return info == null ? null : info.getMacAddress();
    }

    public static int getIpAddressInt(Context context) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null)
            return 0;
        WifiInfo info = wifiManager.getConnectionInfo();
        return info == null ? 0 : info.getIpAddress();
    }

    public static String getIpAddress(Context context) {
        int ipAddress = getIpAddressInt(context);
        return String.format("%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

    /**
     * 进入网络设置页面
     * @param context
     */
    public static void gotoNetworkSetting(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            // 没有发现位置设置
            intent.setAction(Settings.ACTION_SETTINGS);
            try {
                context.startActivity(intent);
            } catch (Exception e) {
            }
        }
    }
}
