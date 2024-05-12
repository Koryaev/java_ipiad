import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.Scanner;

import runnables.ElasticWorker;
import runnables.MainPageLinkFetcher;
import runnables.NewsParser;
import utils.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private final static String base_url = "https://www.m24.ru";

    private Thread start_thread;
    private Thread news_parser_thread;
    private MainPageLinkFetcher main_page_fetcher;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private Settings settings = new Settings();

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        logger.info("Program starts!");
        Main program = new Main();
        program.start();
    }

    public void start() throws IOException, TimeoutException, InterruptedException {
        ElasticWorker Elastic = new ElasticWorker("news");
        Elastic.createIndex();

        NewsParser news_parser = new NewsParser(Elastic, settings);
        news_parser.get_mes_count();
        news_parser_thread = new Thread(news_parser);
        news_parser_thread.start();

        int currentMesCount = 0;

        while (true) {
            synchronized (this) { currentMesCount = settings.getMessageCount(); }

            // if messages in RabbitQueue -> sleep until they will be consumed
            if (currentMesCount != 0) {
                Thread.sleep(2000);
                continue;
            }

            boolean needToParseMainPage = askUser();

            if (needToParseMainPage) {
                main_page_fetcher = new MainPageLinkFetcher(base_url, settings);
                start_thread = new Thread(main_page_fetcher);
                start_thread.start();
                Thread.sleep(1000);
            } else {
                news_parser.finish();
                System.exit(0);
            }
        }
    }

    synchronized boolean askUser() {
        Scanner in = new Scanner(System.in);
        System.out.print("Print 1 to fetch main page, other to exit: ");
        String input = in.nextLine();
        return (Objects.equals(input, "1"));
    }
}