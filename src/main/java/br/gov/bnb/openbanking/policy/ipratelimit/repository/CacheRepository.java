package br.gov.bnb.openbanking.policy.ipratelimit.repository;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class CacheRepository {
    private static final Logger LOGGER = Logger.getLogger(CacheRepository.class.getName());
    private static final String JDG_USER = System.getenv("JDG_USER");
    private static final String JDG_PASSWORD = System.getenv("JDG_PASSWORD");
    private static final String JDG_PROTO = System.getenv("JDG_PROTO");
    private static final String JDG_HOST = System.getenv("JDG_HOST");
    private static final String JDG_CACHE_NAME = System.getenv("JDG_CACHE_NAME");
    private static String LIMIT_FOR_IP = System.getenv("LIMIT_FOR_IP");
    private static int nlimitForIP = 0;
    private static String urlCallCache;

    private static HttpClient createConnection() throws Exception {
        urlCallCache = JDG_PROTO + "://" + JDG_HOST + "/rest/" + JDG_CACHE_NAME + "/";
        HttpClientBuilder httpBuilder = HttpClientBuilder.create();

        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        }).build();

        httpBuilder.setSslcontext(sslContext);
        HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory).build();
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        httpBuilder.setConnectionManager(connMgr);
        CredentialsProvider provider = new BasicCredentialsProvider();

        provider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(JDG_USER, JDG_PASSWORD)
        );

        HttpClient client = httpBuilder
                .setDefaultCredentialsProvider(provider)
                .build();

        if (LIMIT_FOR_IP == null) {
            LIMIT_FOR_IP = "0";
            LOGGER.warning(":: ENVIRONMENT VARIABLE LIMIT_FOR_IP NOT FOUND");
        }

        nlimitForIP = Integer.parseInt(LIMIT_FOR_IP);
        return client;
    }

    public static boolean isAllowed(String clientHost) {
        String callCache = null;
        HttpClient httpClient = null;
        String valueCache = null;

        try {
            httpClient = createConnection();
            callCache = urlCallCache + "amount-ip-" + clientHost;
        } catch (Exception e) {
            LOGGER.severe(":: ERROR ACQUIRING DATAGRID CACHE [" + callCache + "]. Reason: " + e.getMessage());
            return false;
        }
        try {
            LOGGER.info(">>>> GET JDG " + callCache);
            valueCache = getValueCache(httpClient, callCache);
        } catch (Exception e) {
            LOGGER.severe(":: ERROR ACQUIRING DATAGRID CACHE [" + callCache + "]. Reason: " + e.getMessage());
            return false;
        }

        try {
            if (valueCache == null || valueCache.equals("")) {
                valueCache = "0";
            }
            int qtd = Integer.parseInt(valueCache);
            qtd++;
            if (qtd <= nlimitForIP) {
                LOGGER.info("\t:: DATAGRID PUT=" + callCache);
                updateCache(httpClient, callCache, String.valueOf(qtd));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            LOGGER.severe(":: ERROR ACQUIRING DATAGRID CACHE [" + callCache + "]. Reason: " + e.getMessage());
        }
        return false;

    }

    private static String getValueCache(HttpClient httpClient, String callCache) throws Exception {
        HttpGet get = new HttpGet(callCache);
        HttpResponse responseGet = httpClient.execute(get);
        HttpEntity entity = responseGet.getEntity();
        String valueCache = EntityUtils.toString(entity, "UTF-8");
        return valueCache;
    }

    private static void updateCache(HttpClient httpClient, String callCache, String newValueCache) throws Exception {
        HttpPut put = new HttpPut(callCache);
        put.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        StringEntity stringEntity = new StringEntity(newValueCache);
        put.setEntity(stringEntity);
        HttpResponse responsePut = httpClient.execute(put);

    }


}
