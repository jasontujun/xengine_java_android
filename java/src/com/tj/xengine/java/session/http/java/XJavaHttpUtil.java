package com.tj.xengine.java.session.http.java;

import com.tj.xengine.core.session.http.XHttp;
import com.tj.xengine.core.utils.XStringUtil;
import org.apache.http.util.ByteArrayBuffer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Random;

/**
 * 用于生成XJavaHttpRequest的工具类。
 * @see XJavaHttpRequest
 * Created with IntelliJ IDEA.
 * User: tujun
 * Date: 13-9-6
 * Time: 下午7:06
 * To change this template use File | Settings | File Templates.
 */
public abstract class XJavaHttpUtil {

    /**
     * The pool of ASCII chars to be used for generating a multipart boundary.
     */
    private final static char[] MULTIPART_CHARS =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    public static final ByteArrayBuffer FIELD_SEP = encode(XHttp.DEFAULT_CHARSET, ": ");
    public static final ByteArrayBuffer CR_LF = encode(XHttp.DEFAULT_CHARSET, "\r\n");
    public static final ByteArrayBuffer TWO_DASHES = encode(XHttp.DEFAULT_CHARSET, "--");


    private static ByteArrayBuffer encode(final Charset charset, final String string) {
        ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
        ByteArrayBuffer bab = new ByteArrayBuffer(encoded.remaining());
        bab.append(encoded.array(), encoded.position(), encoded.remaining());
        return bab;
    }


    public static void writeBytes(final ByteArrayBuffer b, final OutputStream out) throws IOException {
        out.write(b.buffer(), 0, b.length());
    }

    public static void writeBytes(final String s, final Charset charset,
                                  final OutputStream out) throws IOException {
        ByteArrayBuffer b = encode(charset, s);
        writeBytes(b, out);
    }

    public static void writeBytes(final String s, final OutputStream out) throws IOException {
        ByteArrayBuffer b = encode(XHttp.DEFAULT_CHARSET, s);
        writeBytes(b, out);
    }

    /**
     * 生成Http消息头中的ContentType的值。
     * Content-Type为multipart/form-data
     * @param boundary
     * @param charset
     * @return
     */
    public static String generateMultiContentType(final String boundary, final String charset) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("multipart/form-data; boundary=");
        buffer.append(boundary);
        if (charset != null) {
            buffer.append("; charset=");
            buffer.append(charset);
        }
        return buffer.toString();
    }

    /**
     * 生成Post类型带字符串内容Http消息头中的ContentType的值
     * Content-Type为application/x-www-form-urlencoded
     * @param charset
     * @return
     */
    public static String generatePostStringContentType(final String charset) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("application/x-www-form-urlencoded");
        if (charset != null) {
            buffer.append(";charset=");
            buffer.append(charset);
        }
        return buffer.toString();
    }

    /**
     * 生成Post类型带文件内容ContentType的值，图片格式为image/png,image/jpg等。
     * 非图片为application/octet-stream
     * @param f
     * @return
     */
    public static String generatePostFileContentType(File f) {
        int dotIndex = f.getAbsolutePath().lastIndexOf(".");
        if (dotIndex < 0) {
            return "application/octet-stream";
        }

        String suffix = f.getAbsolutePath().substring(dotIndex).toLowerCase();
        if ("jpg".equals(suffix)
                || "jpeg".equals(suffix))
            return "image/jpeg";
        else if ("png".equals(suffix)
                || "gif".equals(suffix))
            return "image/" + suffix;
        else if ("mp3".equals(suffix)
                || "mpeg".equals(suffix))
            return "audio/mpeg";
        else if ("mp4".equals(suffix)
                || "ogg".equals(suffix))
            return "audio/" + suffix;

        return "application/octet-stream";
    }

    /**
     * 生成一个随机的Boundary字符串
     * @return
     */
    public static String generateBoundary() {
        StringBuilder buffer = new StringBuilder();
        Random rand = new Random();
        int count = rand.nextInt(11) + 30; // a random size from 30 to 40
        for (int i = 0; i < count; i++) {
            buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
        }
        return buffer.toString();
    }

    /**
     * 输入Content-Type的值，获取响应头中的字符编码。例：
     * text/html;charset=ISO-8859-1
     * application/x-www-form-urlencoded;charset=UTF-8
     * @param contentTypeValue Content-Type的值
     * @return
     */
    public static Charset getResponseCharset(String contentTypeValue) {
        if (XStringUtil.isEmpty(contentTypeValue))
            return null;

        // 去除空格
        contentTypeValue = contentTypeValue.replace(" ", "");
        // 提取charset字段的值
        final String tag = "charset";
        int tagIndex = contentTypeValue.indexOf(tag);
        if (tagIndex == -1)
            return null;

        // 获取字符编码
        String charsetName;
        int semicolonIndex = contentTypeValue.indexOf(";", tagIndex);
        if (semicolonIndex == -1)
            charsetName = contentTypeValue.substring(tagIndex + tag.length() + 1);
        else
            charsetName = contentTypeValue.substring(tagIndex + tag.length() + 1, semicolonIndex);
        Charset charset = !XStringUtil.isEmpty(charsetName) ? Charset.forName(charsetName) : null;
        // 如果解析失败，则使用默认编码
        if (charset == null)
            charset = XHttp.DEF_CONTENT_CHARSET;

        return charset;
    }

}
