package hu.sinap86.metlifefundhistory.web;

import com.google.gson.JsonObject;
import hu.sinap86.metlifefundhistory.config.ApplicationConfig;
import hu.sinap86.metlifefundhistory.util.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.*;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
public class BaseHttpClient {

    private final BasicCookieStore cookieStore = new BasicCookieStore();
    private CloseableHttpClient httpclient;
    private RequestConfig requestProxyConfig;

    public BaseHttpClient(final boolean followRedirect) {
        httpclient = createHttpClient(followRedirect);
        requestProxyConfig = createRequestConfigForProxy();
    }

    private CloseableHttpClient createHttpClient(final boolean followRedirect) {
        // accept all certificates
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
        } catch (GeneralSecurityException e) {
            log.error("Cannot build SSL context:", e);
        }

        return HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .setDefaultCookieStore(cookieStore)
                .setRedirectStrategy(getRedirectStrategy(followRedirect))
                .build();
    }

    private RedirectStrategy getRedirectStrategy(final boolean followRedirect) {
        final RedirectStrategy redirectStrategy;
        if (followRedirect) {
            redirectStrategy = new DefaultRedirectStrategy() {
                public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
                    boolean isRedirect = false;
                    try {
                        isRedirect = super.isRedirected(request, response, context);
                    } catch (ProtocolException e) {
                        log.error("", e);
                    }
                    if (!isRedirect) {
                        int responseCode = response.getStatusLine().getStatusCode();
                        if (responseCode == HttpStatus.SC_MOVED_PERMANENTLY || responseCode == HttpStatus.SC_MOVED_TEMPORARILY) {
                            return true;
                        }
                    }
                    return isRedirect;
                }
            };
        } else {
            redirectStrategy = null;
        }
        return redirectStrategy;
    }

    private RequestConfig createRequestConfigForProxy() {
        final ApplicationConfig config = ApplicationConfig.getInstance();
        if (!config.isUseProxy()) {
            log.debug("No proxy config.");
            return null;
        }

        final String proxyHost = config.getProxyHost();
        final int proxyPort = config.getProxyPort().intValue();
        final String proxyScheme = config.getProxyScheme();
        log.debug("Proxy config = host: {}, port: {}, scheme: {}", proxyHost, proxyPort, proxyScheme);

        final HttpHost proxy = new HttpHost(proxyHost, proxyPort, proxyScheme);
        return RequestConfig.custom()
                .setProxy(proxy)
                .build();
    }

    public JsonObject executeGetRequestForJsonReply(final String requestUrl, final String... requestParams) throws IOException {
        final HttpUriRequest request = ArrayUtils.isEmpty(requestParams) ?
                new HttpGet(requestUrl) : new HttpGet(String.format(requestUrl, requestParams));
        final String responseString = execute(request);
        return CommonUtils.getAsJsonObject(responseString);
    }

    public String execute(final HttpUriRequest request) throws IOException {
        final String responseString;

        if (requestProxyConfig != null) {
            if (request instanceof HttpRequestBase) {
                ((HttpRequestBase) request).setConfig(requestProxyConfig);
            } else {
                log.error("Cannot set proxy settings in request!");
            }
        }

        log.debug("Executing request: {}", request.getRequestLine());
        log.debug("Headers: {}", request.getAllHeaders());
        final CloseableHttpResponse response = httpclient.execute(request);
        try {
            log.debug("Response: {}", response.getStatusLine());

            final HttpEntity entity = response.getEntity();
            final Header encodingHeader = entity.getContentEncoding();
            final Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 : Charsets.toCharset(encodingHeader.getValue());

            responseString = EntityUtils.toString(entity, encoding);
            dumpResponse(responseString);
            EntityUtils.consume(entity);

            dumpCookies();
        } finally {
            response.close();
        }
        return responseString;
    }

    private void dumpResponse(final String responseString) {
        // TODO runtime arg to check dump required
        log.debug("=== RAW RESPONSE ===");
        log.debug(responseString);
        log.debug("=== RAW RESPONSE ===");
    }

    private void dumpCookies() {
        // TODO runtime arg to check dump required
        final List<Cookie> cookies = cookieStore.getCookies();
        log.debug("=== COOKIES ===");
        for (int i = 0; i < cookies.size(); i++) {
            log.debug("\t" + cookies.get(i).toString());
        }
        log.debug("=== COOKIES ===");
    }

}
