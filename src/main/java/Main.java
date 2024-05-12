import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.Scanner;

import runnables.ElasticWorker;
import runnables.MainPageLinkFetcher;
import runnables.NewsParser;

public class Main {
    private final static String base_url = "https://www.m24.ru";

    private Thread start_thread;
    private Thread news_parser_thread;
    private MainPageLinkFetcher main_page_fetcher;

    public static void main(String[] args) throws IOException, TimeoutException {

        System.out.println("Hello and welcome!");
        Main program = new Main();
        program.start();
    }

    public void start() throws IOException, TimeoutException {
        ElasticWorker Elastic = new ElasticWorker("news");
        Elastic.createIndex();

        NewsParser news_parser = new NewsParser(Elastic);
        news_parser_thread = new Thread(news_parser);
        news_parser_thread.start();

        Scanner in = new Scanner(System.in);

        while (true) {
            System.out.print("Print 1 to resend urls: ");
            String input = in.nextLine();

            if (Objects.equals(input, "1")) {
                main_page_fetcher = new MainPageLinkFetcher(base_url);
                start_thread = new Thread(main_page_fetcher);
                start_thread.start();
                try {
                    start_thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                news_parser.finish();
                break;
            }
        }
    }
}