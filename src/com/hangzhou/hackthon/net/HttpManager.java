
package com.hangzhou.hackthon.net;

import android.content.Context;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.params.ConnRouteParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Created with IntelliJ IDEA.
 * User: haozi
 * Date: 13-5-11
 * Time: PM12:56
 * To change this template use File | Settings | File Templates.
 */
public class HttpManager {
    private static final int DEFAULT_MAX_CONNECTIONS = 30;
    private static final int DEFAULT_SOCKET_TIMEOUT = 20 * 1000;
    private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;

    private static DefaultHttpClient HTTPCLIENT;
    static {
        final HttpParams httpParams = new BasicHttpParams();

        ConnManagerParams.setTimeout(httpParams, 1000);
        ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(10));
        ConnManagerParams.setMaxTotalConnections(httpParams, DEFAULT_MAX_CONNECTIONS);

        HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams, "UTF-8");
        HttpConnectionParams.setStaleCheckingEnabled(httpParams, false);
        HttpClientParams.setRedirecting(httpParams, false);
        HttpProtocolParams.setUserAgent(httpParams, "Android client");
        HttpConnectionParams.setSoTimeout(httpParams, DEFAULT_SOCKET_TIMEOUT);
        HttpConnectionParams.setConnectionTimeout(httpParams, DEFAULT_SOCKET_TIMEOUT);
        HttpConnectionParams.setTcpNoDelay(httpParams, true);
        HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);
            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            schemeRegistry.register(new Scheme("https", sf, 443));
        } catch (Exception ex) {
            // do nothing, just keep not crash
        }

        ClientConnectionManager manager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);
        HTTPCLIENT = new DefaultHttpClient(manager, httpParams);
    }

    private HttpManager() {
    }

    public static HttpResponse execute(HttpHead head) throws IOException {
        return HTTPCLIENT.execute(head);
    }

    public static HttpResponse execute(HttpHost host, HttpGet get) throws IOException {
        get.addHeader("content-type", "text/json");
        return HTTPCLIENT.execute(host, get);
    }


    public static HttpResponse execute(HttpUriRequest post) throws IOException {
        post.addHeader("content-type", "text/json");
        return HTTPCLIENT.execute(post);
    }

    public static HttpResponse execute(HttpPut put) throws IOException {
        put.addHeader("content-type", "text/json");
        return HTTPCLIENT.execute(put);
    }

    public static HttpResponse execute(Context context, HttpPost post) throws IOException {
        post.addHeader("content-type", "text/json");

        final HttpHost host = (HttpHost)HTTPCLIENT.getParams().getParameter(
                ConnRouteParams.DEFAULT_PROXY);
        if (host != null) {
            HTTPCLIENT.getParams().removeParameter(ConnRouteParams.DEFAULT_PROXY);
        }
        return HTTPCLIENT.execute(post);
    }

    private static class MySSLSocketFactory extends SSLSocketFactory {
        SSLContext sslContext = SSLContext.getInstance("TLS");

        public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException,
                KeyManagementException, KeyStoreException, UnrecoverableKeyException {
            super(truststore);

            TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext.init(null, new TrustManager[] {
                    tm
            }, null);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
                throws IOException, UnknownHostException {
            return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket() throws IOException {
            return sslContext.getSocketFactory().createSocket();
        }
    }
}
