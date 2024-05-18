import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.Scanner;

import runnables.ElasticWorker;
import runnables.MainPageLinkFetcher;
import runnables.NewsParser;
import utils.RequestUtils;
import utils.Settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private Thread start_thread;
    private Thread news_parser_thread_first;
    private Thread news_parser_thread_second;
    private MainPageLinkFetcher main_page_fetcher;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private final Settings settings = new Settings();
    private final RequestUtils requestUtils = new RequestUtils(settings);

    public static void main(String[] args) throws IOException, TimeoutException, InterruptedException {
        logger.info("Program starts!");
        Main program = new Main();
        program.start();
    }

    public void start() throws IOException, TimeoutException, InterruptedException {
        ElasticWorker Elastic = new ElasticWorker(settings);
        Elastic.createIndex();

        NewsParser news_parser_first = new NewsParser(Elastic, settings, requestUtils);
        NewsParser news_parser_second = new NewsParser(Elastic, settings, requestUtils);

        news_parser_first.get_mes_count();  // get mes count in RabbitMQ query if they exists

        news_parser_thread_first = new Thread(news_parser_first);
        news_parser_thread_second = new Thread(news_parser_second);

        news_parser_thread_first.start();
        news_parser_thread_second.start();

        int currentMesCount;

        while (true) {
            synchronized (this) { currentMesCount = settings.getMessageCount(); }

            // if messages in RabbitQueue -> sleep until they will be consumed
            if (currentMesCount != 0) {
                Thread.sleep(2000);
                continue;
            }

            boolean needToParseMainPage = askUser();

            if (needToParseMainPage) {
                main_page_fetcher = new MainPageLinkFetcher(settings, requestUtils);
                start_thread = new Thread(main_page_fetcher);
                start_thread.start();
                Thread.sleep(5000);
            } else {
                news_parser_first.finish();
                news_parser_second.finish();

                Scanner in = new Scanner(System.in);
                System.out.print("Print 2 to run search query with Aggregation, other to exit: ");
                String input = in.nextLine();
                if (Objects.equals(input, "2")) {
                    Elastic.customSearch();
                }
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