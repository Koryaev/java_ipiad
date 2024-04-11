package runnables;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.RequestUtils;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class MainPageLinkFetcher implements Runnable{

    private final String startUrl;

    public MainPageLinkFetcher(String startUrl) {
        this.startUrl = startUrl;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "started");
        try {
            Document doc = RequestUtils.makeGetRequest(startUrl);
            parseMainLinkBody(doc);
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public static void parseMainLinkBody(Document doc) throws IOException, TimeoutException {
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
            String url = baseUrl + path;
            System.out.println(url);

            if (!path.startsWith("/news")) {
                continue;
            }

            try {
                channel.basicPublish("", exchangeName, null, url.getBytes());
//                Document news_doc = makeGetRequest(url);
//                parseNews(news_doc, url);
            } catch (IOException e) {
                channel.close();
                conn.close();
                throw new RuntimeException(e);
            }

//            break;
        }
        channel.close();
        conn.close();

    }
}
