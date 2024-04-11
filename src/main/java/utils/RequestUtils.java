package utils;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;

public class RequestUtils {
    public static Document makeGetRequest(String url) throws IOException {
        Document doc = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {

                if (response.getStatusLine().getStatusCode() == 404) {
                    System.out.println("404 NOT FOUND for url: " + url);
                }

                // Get HttpResponse Status
//                System.out.println(response.getProtocolVersion());              // HTTP/1.1
                System.out.println(response.getStatusLine().getStatusCode());   // 200
//                System.out.println(response.getStatusLine().getReasonPhrase()); // OK
//                System.out.println(response.getStatusLine().toString());        // HTTP/1.1 200 OK

                HttpEntity entity = response.getEntity();

                if (entity != null) {
                    // return it as a String
                    doc = Jsoup.parse(entity.getContent(), "UTF-8", url);
                }
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        return doc;
    }
}
