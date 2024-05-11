package runnables;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.elasticsearch.client.RestClient;

import org.apache.http.HttpHost;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.IOException;

public class ElasticWorker {
    private final ElasticsearchClient EsClient;
    private final String ElasticUrl = "http://localhost:9200";

    private RestClient EsRestClient;  // low-level client
    private ElasticsearchTransport EsTransport;


    public ElasticWorker() {

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

    public void createIndex(String index_name) throws IOException {
        BooleanResponse index_exists = EsClient.indices().exists(ex -> ex.index(index_name));

        if (index_exists.value()) { return; }



    }
}
