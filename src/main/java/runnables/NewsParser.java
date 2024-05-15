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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Settings;

public class NewsParser implements Runnable {
    private Channel linksChannel;
    private Channel parsedDataChannel;
    private Connection conn;
    private final ElasticWorker esWorker;
    private static final Logger logger = LoggerFactory.getLogger(NewsParser.class);
    private final Settings settings;
    private final RequestUtils requestUtils;

    public NewsParser(ElasticWorker esClient, Settings set, RequestUtils reqUtil) {
        esWorker = esClient;
        settings = set;
        requestUtils = reqUtil;

        try {
            create_connection();
        } catch (TimeoutException | IOException ex) {
            logger.error("Can not connect to rabbit:" + ex.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void run() {
        logger.info("News parser starts!");
       try {
           handle_records();
       } catch (IOException e) {
           throw new RuntimeException(e);
       }
    }

    public void finish() throws IOException, TimeoutException {
        logger.info("News parser finish");
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
            synchronized (this) { settings.decrementCount(); }
            logger.info("URL <" + urlData.getUrl() + "> was founded in ES, Scip! Hash <" + urlData.getHash() + ">");
            return;
        }

        String url = urlData.getUrl();
        Document doc = requestUtils.makeGetRequest(url);

        if (doc == null) {
            synchronized (this) { settings.decrementCount(); }
            logger.info("URL <" + urlData.getUrl() + "> request had error, Scip! Hash <" + urlData.getHash() + ">");
            return;
        }

        NewsData newsData = new NewsData();
        newsData.setUrl(url);
        newsData.setHash(urlData.getHash());

        Element header_div = doc.selectFirst("div.b-material-before-body");

        newsData.setTitle(header_div.select("h1").text());

        Element rubrics = header_div.selectFirst("div.b-material__rubrics");

        newsData.setRubric(rubrics.select("a").text());
        newsData.setRubric_url(settings.getURL() + rubrics.select("a").attr("href"));

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
        synchronized (this) { settings.decrementCount(); }
        parsedDataChannel.basicPublish("", settings.getParsedDataExchangeName(),
                null, newsData.toStrJson().getBytes());
    }

    private void handle_records() throws IOException {
        linksChannel.basicConsume(settings.getLinksExchangeName(), false, settings.getTag(),
            new DefaultConsumer(linksChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();

                    String message = new String(body, StandardCharsets.UTF_8);
                    parse_news(message);

                    linksChannel.basicAck(deliveryTag, false);
                }
            });

        parsedDataChannel.basicConsume(settings.getParsedDataExchangeName(), false, settings.getTag(),
            new DefaultConsumer(parsedDataChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    long deliveryTag = envelope.getDeliveryTag();

                    String message = new String(body, StandardCharsets.UTF_8);
                    process_and_sent_to_es(message);

                    parsedDataChannel.basicAck(deliveryTag, false);
                }
            });
    }

    private void create_connection() throws TimeoutException, IOException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(settings.getRabbitName());
        factory.setPassword(settings.getRabbitPassword());
        factory.setHost(settings.getRabbitHost());
        factory.setPort(settings.getRabbitPort());
        conn = factory.newConnection();

        linksChannel = conn.createChannel();
        linksChannel.queueDeclare(  // create queue
                settings.getLinksExchangeName(),
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );
        linksChannel.basicQos(1);  // the number of messages that can be processed at the same time

        parsedDataChannel = conn.createChannel();
        parsedDataChannel.queueDeclare(  // create queue
                settings.getParsedDataExchangeName(),
                true,      // durable
                false,        // exclusive
                false,        // autoDelete
                null          // arguments
        );
        parsedDataChannel.basicQos(1);  // the number of messages that can be processed at the same time
        logger.info("RabbitMQ was connected");
    }

    private void process_and_sent_to_es(String message) throws JsonProcessingException {
        NewsData newsData = new NewsData();
        newsData.fromStrJson(message);
        esWorker.insert_data(newsData);
    }

    public void get_mes_count() {
        try {
            AMQP.Queue.DeclareOk links_response = linksChannel.queueDeclarePassive(settings.getLinksExchangeName());
            settings.setMessageCount(links_response.getMessageCount());
            logger.info(links_response.getMessageCount() + " links was in Rabbit before start");
        } catch (IOException ex) {
            logger.error("Can not get message count: " + ex.getMessage());
            System.exit(1);
        }
    }
}
