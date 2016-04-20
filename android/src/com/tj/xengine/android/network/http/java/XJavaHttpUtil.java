package com.tj.xengine.android.network.http.java;

import com.tj.xengine.core.network.http.XHttp;
import com.tj.xengine.core.utils.XStringUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Map;
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
    public static final byte[] FIELD_SEP = encode(XHttp.DEFAULT_CHARSET, ": ");
    public static final byte[] CR_LF = encode(XHttp.DEFAULT_CHARSET, "\r\n");
    public static final byte[] TWO_DASHES = encode(XHttp.DEFAULT_CHARSET, "--");


    private static byte[] encode(final Charset charset, final String string) {
        ByteBuffer encoded = charset.encode(CharBuffer.wrap(string));
        return encoded.array();
    }


    public static void writeBytes(final byte[] b, final OutputStream out) throws IOException {
        out.write(b, 0, b.length);
    }

    public static void writeBytes(final String s, final Charset charset,
                                  final OutputStream out) throws IOException {
        writeBytes(encode(charset, s), out);
    }

    public static void writeBytes(final String s, final OutputStream out) throws IOException {
        writeBytes(encode(XHttp.DEFAULT_CHARSET, s), out);
    }

    /**
     * 生成Http消息头中的ContentType的值。
     * Content-Type为multipart/form-data
     * @param boundary 分割字符串
     * @param charset 字符编码
     * @return 返回对应的ContentType字符串
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
     * @param charset 字符编码
     * @return 返回对应的ContentType字符串
     */
    public static String generateStringContentType(final String charset) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("application/x-www-form-urlencoded");
        if (charset != null) {
            buffer.append(";charset=");
            buffer.append(charset);
        }
        return buffer.toString();
    }

    /**
     * 生成Post类型带文件内容ContentType的值。
     * 图片格式为image/png,image/jpg等。
     * 音频格式为audio/mpeg,audio/mp4等。
     * 默认为application/octet-stream。
     * @param f 文件
     * @return 返回对应的ContentType字符串
     */
    public static String generateFileContentType(File f) {
        int dotIndex = f.getAbsolutePath().lastIndexOf(".");
        if (dotIndex < 0) {
            return "application/octet-stream";
        }

        String suffix = f.getAbsolutePath().substring(dotIndex).toLowerCase();
        if ("jpg".equals(suffix) || "jpeg".equals(suffix))
            return "image/jpeg";
        else if ("png".equals(suffix) || "gif".equals(suffix))
            return "image/" + suffix;
        else if ("mp3".equals(suffix) || "mpeg".equals(suffix))
            return "audio/mpeg";
        else if ("mp4".equals(suffix) || "ogg".equals(suffix))
            return "audio/" + suffix;
        else
            return "application/octet-stream";
    }

    /**
     * 生成一个随机的Boundary字符串
     * @return 返回Boundary字符串
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
     * @return 返回Content-Type中指定的字符编码;解析失败返回null
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


    /**
     * Unreserved characters, i.e. alphanumeric, plus: {@code _ - ! . ~ ' ( ) *}
     * <p>
     *  This list is the same as the {@code unreserved} list in
     *  <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
     */
    private static final BitSet UNRESERVED   = new BitSet(256);
    /**
     * Punctuation characters: , ; : $ & + =
     * <p>
     * These are the additional characters allowed by userinfo.
     */
    private static final BitSet PUNCT        = new BitSet(256);
    /** Characters which are safe to use in userinfo,
     * i.e. {@link #UNRESERVED} plus {@link #PUNCT}uation */
    private static final BitSet USERINFO     = new BitSet(256);
    /** Characters which are safe to use in a path,
     * i.e. {@link #UNRESERVED} plus {@link #PUNCT}uation plus / @ */
    private static final BitSet PATHSAFE     = new BitSet(256);
    /** Characters which are safe to use in a query or a fragment,
     * i.e. {@link #RESERVED} plus {@link #UNRESERVED} */
    private static final BitSet URIC     = new BitSet(256);
    /**
     * Reserved characters, i.e. {@code ;/?:@&=+$,[]}
     * <p>
     *  This list is the same as the {@code reserved} list in
     *  <a href="http://www.ietf.org/rfc/rfc2396.txt">RFC 2396</a>
     *  as augmented by
     *  <a href="http://www.ietf.org/rfc/rfc2732.txt">RFC 2732</a>
     */
    private static final BitSet RESERVED     = new BitSet(256);


    /**
     * Safe characters for x-www-form-urlencoded data, as per java.net.URLEncoder and browser behaviour,
     * i.e. alphanumeric plus {@code "-", "_", ".", "*"}
     */
    private static final BitSet URLENCODER   = new BitSet(256);

    static {
        // unreserved chars
        // alpha characters
        for (int i = 'a'; i <= 'z'; i++) {
            UNRESERVED.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            UNRESERVED.set(i);
        }
        // numeric characters
        for (int i = '0'; i <= '9'; i++) {
            UNRESERVED.set(i);
        }
        UNRESERVED.set('_'); // these are the charactes of the "mark" list
        UNRESERVED.set('-');
        UNRESERVED.set('.');
        UNRESERVED.set('*');
        URLENCODER.or(UNRESERVED); // skip remaining unreserved characters
        UNRESERVED.set('!');
        UNRESERVED.set('~');
        UNRESERVED.set('\'');
        UNRESERVED.set('(');
        UNRESERVED.set(')');
        // punct chars
        PUNCT.set(',');
        PUNCT.set(';');
        PUNCT.set(':');
        PUNCT.set('$');
        PUNCT.set('&');
        PUNCT.set('+');
        PUNCT.set('=');
        // Safe for userinfo
        USERINFO.or(UNRESERVED);
        USERINFO.or(PUNCT);

        // URL path safe
        PATHSAFE.or(UNRESERVED);
        PATHSAFE.set('/'); // segment separator
        PATHSAFE.set(';'); // param separator
        PATHSAFE.set(':'); // rest as per list in 2396, i.e. : @ & = + $ ,
        PATHSAFE.set('@');
        PATHSAFE.set('&');
        PATHSAFE.set('=');
        PATHSAFE.set('+');
        PATHSAFE.set('$');
        PATHSAFE.set(',');

        RESERVED.set(';');
        RESERVED.set('/');
        RESERVED.set('?');
        RESERVED.set(':');
        RESERVED.set('@');
        RESERVED.set('&');
        RESERVED.set('=');
        RESERVED.set('+');
        RESERVED.set('$');
        RESERVED.set(',');
        RESERVED.set('['); // added by RFC 2732
        RESERVED.set(']'); // added by RFC 2732

        URIC.or(RESERVED);
        URIC.or(UNRESERVED);
    }
    private static final int RADIX = 16;

    private static String encodeFormFields(final String content, final String charset) {
        if (content == null) {
            return null;
        }
        return urlEncode(content, charset != null ? Charset.forName(charset) : XHttp.UTF_8, URLENCODER, true);
    }

    private static String urlEncode(final String content, final Charset charset,
                                    final BitSet safechars, final boolean blankAsPlus) {
        if (content == null) {
            return null;
        }
        final StringBuilder buf = new StringBuilder();
        final ByteBuffer bb = charset.encode(content);
        while (bb.hasRemaining()) {
            final int b = bb.get() & 0xff;
            if (safechars.get(b)) {
                buf.append((char) b);
            } else if (blankAsPlus && b == ' ') {
                buf.append('+');
            } else {
                buf.append("%");
                final char hex1 = Character.toUpperCase(Character.forDigit((b >> 4) & 0xF, RADIX));
                final char hex2 = Character.toUpperCase(Character.forDigit(b & 0xF, RADIX));
                buf.append(hex1);
                buf.append(hex2);
            }
        }
        return buf.toString();
    }

    public static String format(final Map<String, String> parameters, final String charset) {
        return format(parameters, '&', charset);
    }

    public static String format(final Map<String, String> parameters,
                                final char parameterSeparator, final String charset) {
        final StringBuilder result = new StringBuilder();
        String encodedName;
        String encodedValue;
        for (final Map.Entry<String, String> parameter : parameters.entrySet()) {
            encodedName = encodeFormFields(parameter.getKey(), charset);
            encodedValue = encodeFormFields(parameter.getValue(), charset);
            // 如果参数的值为空，则忽略该参数
            if (XStringUtil.isEmpty(encodedValue))
                continue;
            if (result.length() > 0) {
                result.append(parameterSeparator);
            }
            result.append(encodedName);
            result.append("=");
            result.append(encodedValue);
        }
        return result.toString();
    }
}
