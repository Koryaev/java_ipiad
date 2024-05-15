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
    private final Settings settings;
    private final RequestUtils requestUtils;

    public MainPageLinkFetcher(Settings sett, RequestUtils reqUtil) {
        this.settings = sett;
        this.requestUtils = reqUtil;
    }
    private static final Logger logger = LoggerFactory.getLogger(MainPageLinkFetcher.class);

    @Override
    public void run() {
        logger.info("Main page fetcher starts");
        try {
            Document doc = requestUtils.makeGetRequest(settings.getURL());
            if (doc == null) {
                logger.error("CAN NOT GET MAIN PAGE REQUEST! Nothing was added to RabbitMQ! Url <" + settings.getURL()
                        + ">");
                return;
            }
            parseMainLinkBody(doc);
        } catch (IOException | TimeoutException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void parseMainLinkBody(Document doc) throws IOException, TimeoutException, NoSuchAlgorithmException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(settings.getRabbitName());
        factory.setPassword(settings.getRabbitPassword());
        factory.setHost(settings.getRabbitHost());
        factory.setPort(settings.getRabbitPort());
        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();

        channel.queueDeclare(  // create queue
                settings.getLinksExchangeName(),
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );

        channel.basicQos(1);  // the number of messages that can be processed at the same time

        Elements ul = doc.select("div.b-sidebar-news-list > ul");
        Elements li = ul.select("li"); // select all li from ul

        for (Element current_li: li) {
            String path = current_li.select("a[href]").attr("href");
            String title = current_li.select("a[href]").text();
            String url = settings.getURL() + path;
            logger.info("url was founded <" + url + ">");

            if (!path.startsWith("/news")) {
                continue;
            }

            UrlData urlData = new UrlData();
            urlData.setUrl(url);
            urlData.setTitle(title);
            urlData.createHash();

            try {
                channel.basicPublish("", settings.getLinksExchangeName(),
                        null, urlData.toStrJson().getBytes());
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
