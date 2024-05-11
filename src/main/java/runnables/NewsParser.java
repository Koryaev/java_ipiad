package runnables;

import com.rabbitmq.client.*;
import dataClasses.NewsData;
import dataClasses.UrlData;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import utils.RequestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class NewsParser implements Runnable {
    private Channel linksChannel;
    private Channel parsedDataChannel;
    private Connection conn;

    private final String linksExchangeName = "ProducerLinks";
    private final String parsedDataExchangeName = "ParsedData";
    private final String tag = "NewsParserTag";

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "started");
       try {
           create_connection();
           handle_records();

       } catch (IOException | TimeoutException e) {
           throw new RuntimeException(e);

       }
    }

    public void finish() throws IOException, TimeoutException {
        linksChannel.close();
        parsedDataChannel.close();
        conn.close();
    }

    private void parse_news(String strJson) throws IOException{
        UrlData urlData = new UrlData();
        urlData.fromStrJson(strJson);

        String url = urlData.getUrl();

        Document doc = RequestUtils.makeGetRequest(url);

        String baseUrl = "https://www.m24.ru";
        NewsData newsData = new NewsData();
        newsData.setUrl(url);

        Element header_div = doc.selectFirst("div.b-material-before-body");

        newsData.setHeader(header_div.select("h1").text());

        Element rubrics = header_div.selectFirst("div.b-material__rubrics");

        newsData.setRubric(rubrics.select("a").text());
        newsData.setRubric_url(baseUrl + rubrics.select("a").attr("href"));

        newsData.setTime(rubrics.select("p").text());

//        newsData.printData();
//        System.out.println("<" + Thread.currentThread().getName() + "> " + newsData.toString());


        parsedDataChannel.basicPublish("", parsedDataExchangeName, null, newsData.toStrJson().getBytes());

    }

    private void handle_records() throws IOException {
        linksChannel.basicConsume(linksExchangeName, false, tag,
            new DefaultConsumer(linksChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String routingKey = envelope.getRoutingKey();
                    String contentType = properties.getContentType();
                    long deliveryTag = envelope.getDeliveryTag();

                    String message = new String(body, StandardCharsets.UTF_8);
                    parse_news(message);

                    linksChannel.basicAck(deliveryTag, false);
                }
            });

        parsedDataChannel.basicConsume(parsedDataExchangeName, false, tag,
            new DefaultConsumer(parsedDataChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String routingKey = envelope.getRoutingKey();
                    String contentType = properties.getContentType();
                    long deliveryTag = envelope.getDeliveryTag();

                    String message = new String(body, StandardCharsets.UTF_8);
                    System.out.println(message);

                    parsedDataChannel.basicAck(deliveryTag, false);
                }
            });
    }

    private void create_connection() throws TimeoutException, IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
//        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        conn = factory.newConnection();

        linksChannel = conn.createChannel();
        linksChannel.queueDeclare(  // create queue
                linksExchangeName,
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );
        linksChannel.basicQos(1);  // the number of messages that can be processed at the same time

        parsedDataChannel = conn.createChannel();
        parsedDataChannel.queueDeclare(  // create queue
                parsedDataExchangeName,
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );
        parsedDataChannel.basicQos(1);  // the number of messages that can be processed at the same time
    }
}
