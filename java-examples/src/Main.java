import com.tj.xengine.core.network.http.*;
import com.tj.xengine.core.network.http.handler.XHttpStringHandler;
import com.tj.xengine.java.network.download.XHttpDownloader;
import com.tj.xengine.java.network.http.apache.XApacheHttpClient;
import com.tj.xengine.java.network.http.java.XJavaHttpClient;

import java.io.File;

/**
 * Created by jasontujun on 2015/10/28.
 */
public class Main {

    public static void main(String[] args) {
//        testHttp();
        testDownloadFile();
//        testUploadFile();
//        testContentDesposition();
    }

    private static void testContentDisposition() {
        String test = "attachment;filename=\"%E5%BC%80%E5%BF%83_%E4%B8%80%E5%88%BB%EF%BF%A5.txt\";filename*=utf-8'zh-CN'%E5%BC%80%E5%BF%83_%E4%B8%80%E5%88%BB%EF%BF%A5.txt";
        String result = XHttpUtil.parseFileNameFromContentDisposition(test);
        System.out.println(result);
    }

    private static void testHttp() {
//        String url = "http://www.google.com.hk";
//        String url = "https://www.google.com.hk";
        String url = "http://www.baidu.com";
//        String url = "https://www.baidu.com";
//        String url = "https://nodejs.org";
        XHttp httpClient = new XJavaHttpClient(XHttpConfig.builder()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36")
                .build());
//        XHttp httpClient = new XApacheHttpClient(XHttpConfig.builder()
//                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36")
//                .build());
        XHttpRequest request = httpClient.newRequest(url)
                .setMethod(XHttpRequest.Method.GET)
                .setConfig(XHttpConfig.builder()
//                        .setProxy(new XProxy() {
//                            @Override
//                            public Type getType() {
//                                return XProxy.Type.PROXY_SOCKS;
//                            }
//
//                            @Override
//                            public String getProxyAddress() {
//                                return "127.0.0.1";
//                            }
//
//                            @Override
//                            public int getProxyPort() {
//                                return 1080;
//                            }
//                        })
                        .setHandleCookie(true)
                        .setHandleRedirect(true)
                        .setConnectionTimeOut(60 * 1000)
                        .setResponseTimeOut(60 * 1000)
                        .build());
        XHttpResponse response = httpClient.execute(request);
        String contentStr = new XHttpStringHandler().handleResponse(response);
        if (response != null) {
            System.out.println("[http redirect]" + response.getRedirectLocations());
        }
        System.out.println("[http response]" + contentStr);
//        File f = new XHttpFileHandler("H:\\a.html").handleResponse(response);
//        System.out.println("[!!]" + (f == null ? "failed!!": "" + f.exists()));
    }

    private static void testDownloadFile() {
//        XHttp httpClient = new XJavaHttpClient();
        XHttp httpClient = new XApacheHttpClient(XHttpConfig.builder()
                .setProxy(new XProxy() {
                    @Override
                    public Type getType() {
                        return XProxy.Type.PROXY_SOCKS;
                    }

                    @Override
                    public String getProxyAddress() {
                        return "127.0.0.1";
                    }

                    @Override
                    public int getProxyPort() {
                        return 1080;
                    }
                })
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36")
                .setHandleCookie(true)
                .setHandleRedirect(true)
                .setConnectionTimeOut(60 * 1000)
                .setResponseTimeOut(60 * 1000)
                .build());
        XHttpDownloader downloader = new XHttpDownloader(httpClient);
        // https://clients2.google.com/service/update2/crx?response=redirect&prodversion=38.0&x=id%3Daaaaahnmcjcoomdncaekjkjedgagpnln%26installsource%3Dondemand%26uc
        String extId = "innpjfdalfhpcoinfnehdnbkglpmogdi";
        String extUrl = "https://clients2.google.com/service/update2/crx?response=redirect&prodversion=38.0&x=id%3D"
                + extId + "%26installsource%3Dondemand%26uc";
//        String extUrl = "https://www.yahoo.com";// chuncked
        downloader.addTask(extUrl, "H:\\extdata\\crx");
        downloader.startDownload();
    }

    private static void testUploadFile() {
        XHttp httpClient = new XJavaHttpClient(XHttpConfig.builder()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.71 Safari/537.36")
                .setConnectionTimeOut(60 * 1000)
                .setResponseTimeOut(60 * 1000)
                .build());
        XHttpRequest request = httpClient.newRequest("http://127.0.0.1:3000/test/upload")
                .setMethod(XHttpRequest.Method.POST)
                .setChunked(true)
                .addStringParam("test_text", "abc")
                .addFileParam("test_file", new File("H:\\服务器域名.txt"))
                .addFileParam("test_file2", new File("H:\\ddd.txt"));
        XHttpResponse response = httpClient.execute(request);
        String contentStr = new XHttpStringHandler().handleResponse(response);
        if (response != null) {
            System.out.println("[http redirect]" + response.getRedirectLocations());
        }
        System.out.println("[http response]" + contentStr);
    }
}
