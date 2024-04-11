package runnables;

import com.rabbitmq.client.*;
import dataClasses.NewsData;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import utils.RequestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class NewsParser implements Runnable {
    private Channel channel;
    private Connection conn;

    private final String exchangeName = "ProducerLinks";
    private final String tag = "NewsParserTag";

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "started");
       try {
           create_connection();
           handle_records();

//           channel.close();
//           conn.close();
       } catch (IOException | TimeoutException e) {
           throw new RuntimeException(e);

       }
    }

    private void parse_news(String url) throws IOException{
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
        System.out.println("<" + Thread.currentThread().getName() + "> " + newsData.toString());
    }

    private void handle_records() throws IOException {
        channel.basicConsume(exchangeName, false, tag,
                new DefaultConsumer(channel) {
                    @Override
                    public void handleDelivery(String consumerTag,
                                               Envelope envelope,
                                               AMQP.BasicProperties properties,
                                               byte[] body)
                            throws IOException
                    {
                        String routingKey = envelope.getRoutingKey();
                        String contentType = properties.getContentType();
                        long deliveryTag = envelope.getDeliveryTag();

                        String message = new String(body, StandardCharsets.UTF_8);
                        parse_news(message);
//                        System.out.println(" [x] Received '" + message + "'" + "  " + Thread.currentThread());

                        channel.basicAck(deliveryTag, false);
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
        channel = conn.createChannel();
        String exchangeName = "ProducerLinks";

        channel.queueDeclare(  // create queue
                exchangeName,
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );
        channel.basicQos(1);  // the number of messages that can be processed at the same time
    }
}
