package runnables;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
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




}
