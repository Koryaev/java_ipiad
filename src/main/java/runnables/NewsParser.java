package runnables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rabbitmq.client.*;
import dataClasses.NewsData;
import dataClasses.UrlData;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.RequestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class NewsParser implements Runnable {
    private Channel linksChannel;
    private Channel parsedDataChannel;
    private Connection conn;

    private ElasticWorker esWorker;

    private final String linksExchangeName = "ProducerLinks";
    private final String parsedDataExchangeName = "ParsedData";
    private final String tag = "NewsParserTag";

    public NewsParser(ElasticWorker esClient) {
        esWorker = esClient;
    }

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

        boolean hashInEs = esWorker.check_existence(urlData);
        // if hash in elastic -> scip
        if (hashInEs) {
            return;
        }

        String url = urlData.getUrl();
        Document doc = RequestUtils.makeGetRequest(url);

        String baseUrl = "https://www.m24.ru";
        NewsData newsData = new NewsData();
        newsData.setUrl(url);
        newsData.setHash(urlData.getHash());

        Element header_div = doc.selectFirst("div.b-material-before-body");

        newsData.setTitle(header_div.select("h1").text());

        Element rubrics = header_div.selectFirst("div.b-material__rubrics");

        newsData.setRubric(rubrics.select("a").text());
        newsData.setRubric_url(baseUrl + rubrics.select("a").attr("href"));

        newsData.setTime(rubrics.select("p").text());

        Element body_div = doc.selectFirst("div.b-material-body");
        Elements paragraphs = body_div.select("p:not([class])");

        StringBuilder body = new StringBuilder();

        for (Element current_paragraph: paragraphs) {
            if (!current_paragraph.text().isEmpty()) {
                body.append(current_paragraph.text()).append("\n");
            }
        }

        newsData.setBody(body.toString());

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
                    process_and_sent_to_es(message);

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

    private void process_and_sent_to_es(String message) throws JsonProcessingException {
        NewsData newsData = new NewsData();
        newsData.fromStrJson(message);
        esWorker.insert_data(newsData);
    }
}
