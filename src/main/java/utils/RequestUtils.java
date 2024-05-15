package utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RequestUtils {
    private final Settings settings;
    private static final Logger logger = LoggerFactory.getLogger(RequestUtils.class);

    public RequestUtils(Settings sett) {
        settings = sett;
    }
    public Document makeGetRequest(String url) throws IOException {
        Document doc = null;

        for (int i = 0; i < settings.getRequestsRetryCount(); ++i) {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(url);

                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    int responseStatus = response.getStatusLine().getStatusCode();

                    if (responseStatus == 404) {
                        logger.error("Get 404 NOT FOUND for <" + url + ">. SCIP!");
                        return doc;
                    }

                    if (responseStatus != 200) {
                        logger.error("Get " + responseStatus + " " + response.getStatusLine().getReasonPhrase() + " " +
                                "for <" + url + ">");
                        logger.warn("Retry get response for <" + url + ">");
                        continue;
                    }

//                response.getProtocolVersion()              // HTTP/1.1
//                response.getStatusLine().getStatusCode()   // 200
//                response.getStatusLine().getReasonPhrase() // OK
//                response.getStatusLine().toString()        // HTTP/1.1 200 OK

                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        // return it as a String
                        doc = Jsoup.parse(entity.getContent(), "UTF-8", url);
                    } else {
                    logger.error("Get NULL response entity for <" + url + ">. SCIP!");
                    return null;
                    }
                }
            } catch (IOException ex) {
                logger.error("Get error: " + ex.getMessage() + " for <" + url + ">. SCIP!");
                return null;
            }
        }
        return doc;
    }
}
