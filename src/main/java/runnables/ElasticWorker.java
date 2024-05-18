package runnables;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.FieldDateMath;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchPhrasePrefixQuery;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.MgetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dataClasses.NewsData;
import dataClasses.UrlData;
import org.elasticsearch.client.RestClient;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ElasticWorker {
    private final Settings settings;
    private final ElasticsearchClient EsClient;
    private RestClient EsRestClient;  // low-level client
    private ElasticsearchTransport EsTransport;

    private final String index_name;
    private static final Logger logger = LoggerFactory.getLogger(ElasticWorker.class);

    public ElasticWorker(Settings sett) {
        settings = sett;
        index_name = settings.getIndexName();

        // Create the low-level client
        EsRestClient = RestClient
                .builder(HttpHost.create(settings.getElasticUrl()))
                .build();

        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();

        EsTransport = new RestClientTransport(EsRestClient, new JacksonJsonpMapper(mapper));
        EsClient = new ElasticsearchClient(EsTransport);
        logger.info("Elastic was connected");
    }

    public void createIndex() throws IOException {
        BooleanResponse index_exists = EsClient.indices().exists(ex -> ex.index(index_name));


        if (index_exists.value()) {
            logger.info("Index '" + index_name + "' already exists");
//            delete_index();
//            System.exit(1);
            return;
        }

        EsClient.indices().create(c -> c.index(index_name).mappings(m -> m
                .properties("url", p -> p.text(d -> d.fielddata(true)))
                .properties("body", p -> p.text(d -> d.fielddata(true)))
                .properties("header", p -> p.text(d -> d.fielddata(true)))
                .properties("hash", p -> p.text(d -> d.fielddata(true)))
                .properties("rubric", p -> p.text(d -> d.fielddata(true)))
                .properties("rubric_url", p -> p.text(d -> d.fielddata(true)))
                .properties("time", p -> p.date(d -> d.format("strict_date_optional_time")))
        ));
        logger.info("Index '" + index_name + "' was created!");
    }

    public void delete_index() throws IOException {
        EsClient.indices().delete(d -> d.index(index_name));
        logger.info("Index '" + index_name + "' was deleted");
    }

    public void insert_data(NewsData data) {
        IndexResponse response;
        try {
             response = EsClient.index(i -> i
                    .index(index_name)
                    .document(data)
            );
        } catch (IOException ex) {
            logger.error("Error with inserting data to elastic <" + data.getUrl() + ">");
            logger.error(ex.getMessage());
            return;
        }
        logger.info("<" + data.getUrl() + "> was added to ES");
    }

    public boolean check_existence(UrlData data) {
        SearchResponse<NewsData> response = null;
        String hash = data.getHash();
        try {
            response = EsClient.search(s -> s
                            .index(index_name)
                            .query(q -> q
                                    .match(t -> t
                                            .field("hash")
                                            .query(hash)
                                    )
                            ),
                    NewsData.class
            );
        } catch (ElasticsearchException | IOException ex) {
            logger.error("Error with check existence for <" + data.getUrl() + ">");
            System.exit(1);
        }
        return response.hits().total().value() != 0;
    }


    public void customSearch() throws IOException, ElasticsearchException {
        logger.info("Start custom search!");

        Query rubricMatch = MatchQuery.of(m -> m
                .field("rubric")
                .query("происшествия")
        )._toQuery();

        Query titlePrefixMatch = MatchPhrasePrefixQuery.of(m -> m
                .field("title")
                .query("Суд")
        )._toQuery();


        // AND
        SearchResponse<NewsData> andResponse = EsClient.search(s -> s
                        .index(index_name)
                        .query(q -> q
                                .bool(b -> b
                                        .must(rubricMatch, titlePrefixMatch)
                                )
                        ),
                NewsData.class
        );
        printQuery(andResponse, "AND");

        // OR
        SearchResponse<NewsData> orResponse = EsClient.search(s -> s
                        .index(index_name)
                        .query(q -> q
                                .bool(b -> b
                                        .should(rubricMatch, titlePrefixMatch)
                                )
                        ),
                NewsData.class
        );
        printQuery(orResponse, "OR");

        // SCRIPT
        SearchResponse<NewsData> scriptResponse = EsClient.search(s -> s
                        .index(index_name)
                        .query(q -> q
                                .scriptScore(ss -> ss
                                        .query(q1 -> q1
                                                .matchAll(ma -> ma))
                                        .script(scr -> scr
                                                .inline(i -> i
                                                        .source("doc['url'].value.length()"))))),
                NewsData.class
        );
        printQuery(scriptResponse, "SCRIPT");

        // MULTIGET
        MgetResponse<NewsData> mgetResponse = EsClient.mget(mgq -> mgq
                        .index(index_name)
                        .docs(d -> d
                                .id("RP8fgY8Bkzx9pZDdU8U_")
                                .id("QP8fgY8Bkzx9pZDdTsV2")
                                .id("DrUliI8BUNlL19jmXjWl")),

                NewsData.class
        );
        List<NewsData> mgetHits = new ArrayList<>();
        System.out.println(mgetResponse.toString());
        mgetHits.add(mgetResponse.docs().getFirst().result().source());
        logger.info("Search results for <MULTIGET>");
        for (NewsData newsData: mgetHits) {
            assert newsData != null;
            logger.debug(newsData.toString());
        }
        System.out.println();

        // Date Histogram Aggregation
        Aggregation aggregation1 = Aggregation.of(a -> a
                .dateHistogram(dha -> dha
                        .field("time")
                        .calendarInterval(CalendarInterval.valueOf(String.valueOf(CalendarInterval.Day)))
                )
        );
        SearchResponse<?> dhAggregation = EsClient.search(s -> s
                        .index(index_name)
                        .aggregations("articles_per_day", aggregation1),
                NewsData.class
        );
        logger.info("Date Histogram Aggregation");
        logger.debug(dhAggregation.toString());

        // Date Range Aggregation
        Aggregation aggregation2 = Aggregation.of(a -> a.dateRange(dha -> dha.field("time")
                .ranges(dr -> dr
                        .from(FieldDateMath.of(fdm -> fdm.expr("2024-01-01")))
                        .to(FieldDateMath.of(fdm -> fdm.expr("2024-06-01"))))));
        SearchResponse<?> drAggregation = EsClient.search(s -> s
                        .index(index_name)
                        .aggregations("articles_in_range", aggregation2),
                NewsData.class
        );
        logger.info("Date Range Aggregation");
        logger.debug(drAggregation.toString());

        // Histogram Aggregation
        Aggregation aggregation3 = Aggregation.of(a -> a.histogram(dha -> dha.script(scr -> scr
                        .inline(i -> i
                                .source("doc['url'].value.length()")
                                .lang("painless"))
                ).interval(10.0)
        ));
        SearchResponse<?> hAggregation = EsClient.search(s -> s
                        .index(index_name)
                        .aggregations("url_length_histogram", aggregation3),
                NewsData.class
        );
        logger.info("Histogram Aggregation");
        logger.debug(hAggregation.toString());


        // Terms Aggregation
        Aggregation aggregation4 = Aggregation.of(a -> a.terms(t -> t
                        .field("rubric")
                )
        );
        SearchResponse<?> tAggregation = EsClient.search(s -> s
                        .index(index_name)
                        .aggregations("popular_rubric", aggregation4),
                NewsData.class
        );
        logger.info("Terms Aggregation");
        logger.debug(tAggregation.toString());


        // Filter Aggregation
        Aggregation aggregation5 = Aggregation.of(a -> a
                .avg(avg -> avg
                        .script(scr -> scr
                                .inline(i -> i
                                        .source("doc['rubric'].value.length()")
                                        .lang("painless"))
                        )
                )
        );
        Aggregation aggregation6 = Aggregation.of(a -> a
                .filter(q -> q.term(t -> t
                                .field("rubric")
                                .value("Общество")
                        )
                )
                .aggregations("avg_rubric_length", aggregation5)
        );
        SearchResponse<?> fAggregation = EsClient.search(s -> s
                        .index(index_name)
                        .aggregations("filtered_rubric", aggregation6),
                NewsData.class
        );
        logger.info("Filter Aggregation");
        logger.debug(fAggregation.toString());

        // Logs Aggregation
        Aggregation aggregation7 = Aggregation.of(a -> a.terms(t -> t
                        .field("stream.keyword")
                        .size(10)
                )
        );
        SearchResponse<?> lAggregation = EsClient.search(s -> s
                        .index("logs-generic-default")
                        .aggregations("streams", aggregation7)
                        .size(0),
                NewsData.class
        );
        logger.info("Logs Aggregation");
        logger.debug(lAggregation.toString());
    }

    public void printQuery(SearchResponse<NewsData> response, String responseType) {
        List<Hit<NewsData>> hits = response.hits().hits();

        if (hits.isEmpty()) {
            logger.warn("Empty " + responseType + " response");
            return;
        }

        logger.info("Search results for <" + responseType + ">");

        for (Hit<NewsData> hit: hits) {
            NewsData newsData = hit.source();
            assert newsData != null;
            logger.debug(newsData.toString());
        }
    }
}
