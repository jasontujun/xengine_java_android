package com.tj.xengine.core.utils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jasontujun.
 * Date: 12-3-3
 * Time: 下午7:35
 */
public abstract class XStringUtil {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4',
            '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    private static final String RANDOM_NUMBERS = "1234567890";
    private static final String RANDOM_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static boolean isEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    /**
     * 返回字符串的字节个数（中文当2个字节计算）
     * @param s
     * @return
     */
    public static int stringSize(String s) {
        try {
            String anotherString = new String(s.getBytes("GBK"), "ISO8859_1");
            return anotherString.length();
        }
        catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    /**
     * 验证字符串是否符合email格式
     * @param email 验证的字符串
     * @return 如果字符串为空或者为Null返回false
     *         如果不为空或Null则验证其是否符合email格式，
     *         符合则返回true,不符合则返回false
     */
    public static boolean isEmail(String email) {
        if (isEmpty(email))
            return false;
        //通过正则表达式验证Emial是否合法
        return email.matches("^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@" +
                "([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$");
    }

    /**
     * 验证字符串是否是数字（允许以0开头的数字）
     * 通过正则表达式验证。
     * @param numStr
     * @return
     */
    public static boolean isNumber(String numStr) {
        if (isEmpty(numStr))
            return false;
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher match = pattern.matcher(numStr);
        return match.matches();
    }

    public static String stream2String(InputStream is) {
        Charset charset = null;
        return stream2String(is, charset);
    }

    public static String stream2String(InputStream is, String charsetName) {
        Charset charset = !isEmpty(charsetName) ? Charset.forName(charsetName) : null;
        return stream2String(is, charset);
    }

    /**
     * 将数据流转换成字符串.
     * @param is
     * @param charset
     * @return
     */
    public static String stream2String(InputStream is, Charset charset) {
        if (is == null)
            return null;
        InputStreamReader isr = charset == null ?
                new InputStreamReader(is) : new InputStreamReader(is, charset);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(LINE_SEPARATOR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
                isr.close();
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static String date2str(long time) {
        return date2str(new Date(time));
    }

    public static String date2str(Date date) {
        if (date == null)
            return null;

        return date2str(date.getYear() + 1900, date.getMonth() + 1,
                date.getDate(), date.getHours(), date.getMinutes());
    }

    public static String date2str(int year, int month, int day, int hour, int minute) {
        String hourStr = "" + hour;
        String minStr = "" + minute;
        if (hour < 10) {
            hourStr = "0" + hourStr;
        }
        if (minute < 10) {
            minStr = "0" +minStr;
        }
        return year + "-" + month + "-" + day + " " + hourStr + ":" + minStr;
    }



    public static String date2calendarStr(Date date) {
        return date2calendarStr(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
    }

    public static String date2calendarStr(int year, int month, int day) {
        return year+"-"+month+"-"+day;
    }


    public static String calendar2str(Calendar c) {
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int day = c.get(Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + day;
    }

    public static Calendar str2calendar(String str) {
        if (isEmpty(str))
            return null;

        String[] strList = str.split("-");
        if (strList.length != 3 || !isNumber(strList[0]) ||
                !isNumber(strList[1]) || !isNumber(strList[2]))
            return null;

        int year = Integer.parseInt(strList[0]);
        int month = Integer.parseInt(strList[1]);
        int day = Integer.parseInt(strList[2]);
        Calendar result = Calendar.getInstance();
        result.set(year, month - 1, day);
        return result;
    }

    /**
     * 将对象数组转化为指定连接符连接而成的字符串。
     * 实际上会调用对象的toString()方法。
     * @param values 字符串数组
     * @param connector 连接符
     * @return
     */
    public static String array2String(Object[] values, String connector) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            sb.append(values[i].toString());
            if (connector != null && i < values.length - 1) {
                sb.append(connector);
            }
        }
        return sb.toString();
    }

    /**
     * 将对象列表转化为指定连接符连接而成的字符串。
     * 实际上会调用对象的toString()方法。
     * @param values 字符串数组
     * @param connector 连接符
     * @return
     */
    public static String list2String(List values, String connector) {
        if (values == null || values.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            sb.append(values.get(i).toString());
            if (connector != null && i < values.size() - 1) {
                sb.append(connector);
            }
        }
        return sb.toString();
    }

    /**
     * 百分比转字符串
     * @param percent
     * @return
     */
    public static String percent2str(double percent) {
        int percentInt = (int) (percent * 100);
        return percentInt + "%";
    }

    /**
     * 字符串转为md5编码
     * @param str 源字符串
     * @return md5编码后的字符串
     */
    public static String str2md5(String str) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(str.getBytes());
            byte[] bytes = algorithm.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                hexString.append(HEX_DIGITS[b >> 4 & 0xf]);
                hexString.append(HEX_DIGITS[b & 0xf]);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将文件大小转换（保留两位小数）
     * @param size 文件大小，单位:byte
     * @return 文件大小
     */
    public static String fileSize2String(long size) {
        return fileSize2String(size, 2);
    }

    private static final float KB = 1024;
    private static final float MB = 1024*1024;
    private static final float GB = 1024*1024*1024;
    /**
     * 将文件大小转换
     * @param size 文件大小，单位:byte
     * @param scale 小数位数
     * @return 文件大小
     */
    public static String fileSize2String(long size, int scale) {
        float result;
        String unit;
        if (size <= 0) {
            result = 0;
            unit = "B";
        } else if (size < KB) {
            result = size;
            unit = "B";
        } else if (size < MB) {
            result = size/KB;
            unit = "KB";
        } else if (size < GB) {
            result = size/MB;
            unit = "MB";
        } else {
            result = size/GB;
            unit = "GB";
        }
        BigDecimal bg = new BigDecimal(result);
        float f1 = bg.setScale(scale, BigDecimal.ROUND_HALF_UP).floatValue();
        return f1 + unit;
    }

    /**
     * 将毫秒转换为 "*日*小时*分钟*秒"
     * @param mss 要转换的毫秒数
     * @return
     */
    public static String formatDuring(long mss) {
        long days = mss / (1000 * 60 * 60 * 24);
        long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss % (1000 * 60)) / 1000;

        if (days != 0) {
            if (hours > 12) {
                return days+"天+";
            } else {
                return days+"天";
            }
        } else if(hours !=0) {
            if (minutes >30) {
                return hours + "小时+";
            } else {
                return hours + "小时";
            }
        } else if (minutes != 0) {
            return minutes + "分钟";
        } else {
            return seconds + "秒";
        }
    }

    /**
     * 将两日期的间隔转换为 "*日*小时*分钟*秒"
     * @param begin 时间段的开始
     * @param end   时间段的结束
     * @return  输入的两个Date类型数据之间的时间间格用
     * * days * hours * minutes * seconds的格式展示
     */
    public static String formatDuring(Date begin, Date end) {
        return formatDuring(end.getTime() - begin.getTime());
    }

    /**
     * 处理文件名，去掉后缀
     * @param srcName
     * @return
     */
    public static String removeFileNameSuffix(String srcName) {
        int dotIndex = srcName.lastIndexOf(".");
        if (dotIndex == -1)
            return srcName;
        else
            return srcName.substring(0, dotIndex);
    }

    /**
     * 获取随机字符串
     * @param length 字符串长度
     * @return 返回随机字符串
     */
    public static String getRandomString(int length) {
        if (length <= 0)
            return null;

        StringBuffer sb = new StringBuffer();
        Random r = new Random();
        int range = RANDOM_CHARS.length();
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(r.nextInt(range)));
        }
        return sb.toString();
    }

    /**
     * 获取指定位数随机十进制整数(最高位不为0)
     * @param length 整数的位数
     * @return 返回随机整数
     */
    public static String getRandomNumber(int length) {
        if (length <= 0)
            return null;

        StringBuffer sb = new StringBuffer();
        Random r = new Random();
        int range = RANDOM_NUMBERS.length();
        sb.append(RANDOM_NUMBERS.charAt(r.nextInt(range - 1)));// 最高位不为0
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_NUMBERS.charAt(r.nextInt(range)));
        }
        return sb.toString();
    }

    /**
     * 截取url中的host域
     * @param url url地址
     * @return 返回url中的host域；截取失败返回null
     */
    public static String getHost(String url){
        if(url == null || "".equals(url.trim()))
            return null;

        Pattern p =  Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
        Matcher matcher = p.matcher(url);
        if (matcher.find())
            return matcher.group();
        else
            return null;
    }

    /**
     * 去掉字符串两端的"
     * @param s 原字符串
     * @return 返回去掉"的字符串；如果处理失败，返回原字符串
     */
    public static String unquote(String s) {
        return unquote(s, "\"");
    }

    /**
     * 去掉字符串两端的指定的引号字符
     * @param s 原字符串
     * @param quote 指定的引号字符
     * @return 返回去掉指定的引号字符的字符串；如果处理失败，返回原字符串
     */
    public static String unquote(String s, String quote) {
        if (!isEmpty(s) && !isEmpty(quote) && s.length() >= 2 * quote.length()) {
            if (s.startsWith(quote) && s.endsWith(quote)) {
                return s.substring(quote.length(), s.length() - quote.length());
            }
        }
        return s;
    }
}
