package runnables;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import dataClasses.UrlData;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.RequestUtils;
import utils.Settings;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;


public class MainPageLinkFetcher implements Runnable{

    private final String startUrl;
    private Settings settings;

    public MainPageLinkFetcher(String startUrl, Settings sett) {
        this.startUrl = startUrl;
        this.settings = sett;
    }
    private static final Logger logger = LoggerFactory.getLogger(MainPageLinkFetcher.class);

    @Override
    public void run() {
        logger.info("Main page fetcher starts");
        try {
            Document doc = RequestUtils.makeGetRequest(startUrl);
            parseMainLinkBody(doc);
        } catch (IOException | TimeoutException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseMainLinkBody(Document doc) throws IOException, TimeoutException, NoSuchAlgorithmException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
//        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        String exchangeName = "ProducerLinks";
//        String routingKey = "testRoute";

        channel.queueDeclare(  // create queue
                exchangeName,
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );

        channel.basicQos(1);  // the number of messages that can be processed at the same time

        String baseUrl = "https://www.m24.ru";

        Elements ul = doc.select("div.b-sidebar-news-list > ul");
        Elements li = ul.select("li"); // select all li from ul

        for (Element current_li: li) {
            String path = current_li.select("a[href]").attr("href");
            String title = current_li.select("a[href]").text();
            String url = baseUrl + path;
            logger.info("url was founded <" + url + ">");

            if (!path.startsWith("/news")) {
                continue;
            }

            UrlData urlData = new UrlData();
            urlData.setUrl(url);
            urlData.setTitle(title);
            urlData.createHash();

            try {
                channel.basicPublish("", exchangeName, null, urlData.toStrJson().getBytes());
            } catch (IOException e) {
                channel.close();
                conn.close();
                throw new RuntimeException(e);
            }
            synchronized (this) { settings.incrementCount(); }

        }
        channel.close();
        conn.close();
    }
}
