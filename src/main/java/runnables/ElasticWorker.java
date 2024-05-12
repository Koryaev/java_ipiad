package runnables;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import dataClasses.NewsData;
import dataClasses.UrlData;
import org.elasticsearch.client.RestClient;

import org.apache.http.HttpHost;

import java.io.IOException;

public class ElasticWorker {
    private final ElasticsearchClient EsClient;
    private final String ElasticUrl = "http://localhost:9200";

    private RestClient EsRestClient;  // low-level client
    private ElasticsearchTransport EsTransport;

    private final String index_name;

    public ElasticWorker(String name) {
       index_name = name;

        // Create the low-level client
        EsRestClient = RestClient
                .builder(HttpHost.create(ElasticUrl))
//                .setDefaultHeaders(new Header[]{
//                        new BasicHeader("Authorization", "ApiKey " + apiKey)
//                })
                .build();

        EsTransport = new RestClientTransport(EsRestClient, new JacksonJsonpMapper());
        EsClient = new ElasticsearchClient(EsTransport);
    }

    public void createIndex() throws IOException {
        BooleanResponse index_exists = EsClient.indices().exists(ex -> ex.index(index_name));


        if (index_exists.value()) {
//            EsClient.indices().delete(d -> d
//                    .index(index_name)
//            );
            return;
        }

        EsClient.indices().create(c -> c.index(index_name).mappings(m -> m
                .properties("url", p -> p.text(d -> d.fielddata(true)))
                .properties("body", p -> p.text(d -> d.fielddata(true)))
                .properties("header", p -> p.text(d -> d.fielddata(true)))
                .properties("hash", p -> p.text(d -> d.fielddata(true)))
                .properties("rubric", p -> p.text(d -> d.fielddata(true)))
                .properties("time", p -> p.text(d -> d.fielddata(true)))
                .properties("rubric_url", p -> p.text(d -> d.fielddata(true)))
        ));
    }

    public void insert_data(NewsData data) {
        IndexResponse response;
        try {
             response = EsClient.index(i -> i
                    .index(index_name)
                    .document(data)
            );
        } catch (IOException ex) {
            System.out.println("Error with inserting data to elastic <" + data.getUrl() + ">");
            System.out.println(ex.getMessage());
            return;
        }

        System.out.println(response.result().jsonValue());
    }

    public boolean check_existence(UrlData data) {
        SearchResponse<UrlData> response = null;
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
                    UrlData.class
            );
        } catch (ElasticsearchException | IOException ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
        return response.hits().total().value() != 0;
    }
}
