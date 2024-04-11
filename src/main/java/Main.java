import java.nio.charset.StandardCharsets;
import java.util.Map;

import dataClasses.NewsData;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import runnables.MainPageLinkFetcher;
import runnables.NewsParser;
//import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    private static Map<String, Document> responsesMap;
    private final static String base_url = "https://www.m24.ru";

    public static void main(String[] args) {

        System.out.println("Hello and welcome!");

        MainPageLinkFetcher main_page_fetcher = new MainPageLinkFetcher(base_url);
        Thread t1 = new Thread(main_page_fetcher);
        t1.start();

        NewsParser news_parser = new NewsParser();
        Thread t2 = new Thread(news_parser);
        t2.start();

        System.out.println("fin");

    }

    public static NewsData parseNews(Document doc, String url) {
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
        System.out.println(newsData.toString());

        return newsData;
    }
}