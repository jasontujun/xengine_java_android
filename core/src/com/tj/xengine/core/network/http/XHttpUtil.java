package com.tj.xengine.core.network.http;

import com.tj.xengine.core.utils.XStringUtil;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * XHttp的工具类。
 * Created by jason on 2015/10/30.
 */
public abstract class XHttpUtil {

    private static final String FILE_NAME_TAG = "filename=";
    private static final String FILE_NAME_TAG2 = "filename*=";

    /**
     * 解析http请求资源的文件名。
     * 该方法传入url和Content-Disposition的值，优先根据Content-Disposition的值提取出URL-Decode后的文件名；
     * 如果为空或解析失败了，则截取url字符串中的ULR-Decode后的文件名。
     * @param url http请求的完整url
     * @param contentDisposition http请求响应的Content-Disposition的值
     * @return 返回解析并URL-Decode后的文件名；如果解析失败，返回null;
     */
    public static String parseFileNameFromHttp(String url, String contentDisposition) {
        String fileName = parseFileNameFromContentDisposition(contentDisposition);
        return XStringUtil.isEmpty(fileName) ? parseFileNameFromUrl(url) : fileName;
    }

    public static String parseFileNameFromContentDisposition(String contentDisposition) {
        // eg: Content-Disposition: attachment;filename="encoded_text";filename*=utf-8'zh-CN'encoded_text
        String fileName = null;
        if (!XStringUtil.isEmpty(contentDisposition)) {
            contentDisposition = contentDisposition.replace(" ", "");
            int startIndex = contentDisposition.indexOf(FILE_NAME_TAG2);
            if (startIndex != -1) {
                int endIndex = contentDisposition.indexOf(";", startIndex + FILE_NAME_TAG2.length() + 1);
                // eg: value="utf-8'zh-CN'encoded_text"
                String value = endIndex == -1 ? contentDisposition.substring(startIndex + FILE_NAME_TAG2.length())
                        : contentDisposition.substring(startIndex + FILE_NAME_TAG2.length(), endIndex);
                value = XStringUtil.unquote(value);
                int charsetEndIndex = value.indexOf("'");
                if (charsetEndIndex != -1) {
                    int nameStartIndex = value.indexOf("'", charsetEndIndex + 1);
                    if (nameStartIndex != -1) {
                        String charsetName = value.substring(0, charsetEndIndex);
                        String encodedFileName = value.substring(nameStartIndex + 1);
                        try {
                            fileName = urlDecodeByRFC3986(encodedFileName, charsetName);
                            if (!XStringUtil.isEmpty(fileName))
                                return fileName;
                        } catch (Exception e) {
                            e.printStackTrace();
                            fileName = null;
                        }
                    }
                }
            }
            startIndex = contentDisposition.indexOf(FILE_NAME_TAG);
            if (startIndex != -1) {
                int endIndex = contentDisposition.indexOf(";", startIndex + FILE_NAME_TAG.length() + 1);
                // eg: value="encoded_text"
                String value = endIndex == -1 ? contentDisposition.substring(startIndex + FILE_NAME_TAG.length())
                        : contentDisposition.substring(startIndex + FILE_NAME_TAG.length(), endIndex);
                value = XStringUtil.unquote(value);
                try {
                    fileName = urlDecodeByRFC3986(value, "UTF-8");
                    if (!XStringUtil.isEmpty(fileName))
                        return fileName;
                } catch (Exception e) {
                    e.printStackTrace();
                    fileName = null;
                }
            }
        }
        return fileName;
    }

    public static String parseFileNameFromUrl(String url) {
        // eg: url="http://example.org";
        // eg: url="http://example.org/";
        // eg: url="http://example.org/bbb/file.xml";
        // eg: url="http://example.org/bbb/file.xml?";
        // eg: url="http://example.org/bbb/file.xml?a=123&b=456";
        // eg: url="http://example.org/bbb/file.xml#anchor";
        // eg: url="http://example.org/bbb/file.xml#/p=foo&q=bar";
        String fileName = null;
        if (!XStringUtil.isEmpty(url)) {
            try {
                String path = new URL(url).getPath();
                String fileNameEncoded = path.substring(path.lastIndexOf("/") + 1);
                try {
                    fileName = urlDecodeByRFC3986(fileNameEncoded, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return fileName;
    }

    /**
     * 基于RFC-3986的URL编码规范的解码方法. 规则如下:
     * <ul>
     *     <li>字母 {@code "a"} 到 {@code "z"}, {@code "A"} 到 {@code "Z"}, 以及 {@code "0"} 到 {@code "9"} 保持不变.</li>
     *     <li>特殊字符 {@code "-"}, {@code "_"}, {@code "."}, 以及 {@code "*"} 保持不变.</li>
     *     <li>形如 "<code>%<i>xy</i></code>" 的字符串会被解释为一个字符的16进制表示.</li>
     * </ul>
     * @param source 原字符串
     * @param encoding 字符编码
     * @return 解码后的字符串
     * @throws IllegalArgumentException when the given source contains invalid encoded sequences
     * @throws UnsupportedEncodingException when the given encoding parameter is not supported
     * @see java.net.URLDecoder#decode(String, String)
     */
    public static String urlDecodeByRFC3986(String source, String encoding) throws UnsupportedEncodingException {
        if (XStringUtil.isEmpty(source))
            return source;

        final int length = source.length();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
        boolean changed = false;
        for (int i = 0; i < length; i++) {
            int ch = source.charAt(i);
            if (ch == '%') {
                if ((i + 2) < length) {
                    char hex1 = source.charAt(i + 1);
                    char hex2 = source.charAt(i + 2);
                    int u = Character.digit(hex1, 16);
                    int l = Character.digit(hex2, 16);
                    if (u == -1 || l == -1) {
                        throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                    }
                    bos.write((char) ((u << 4) + l));
                    i += 2;
                    changed = true;
                }
                else {
                    throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
            }
            else {
                bos.write(ch);
            }
        }
        return changed ? (XStringUtil.isEmpty(encoding) ? new String(bos.toByteArray())
                : new String(bos.toByteArray(), encoding)) : source;
    }
}
