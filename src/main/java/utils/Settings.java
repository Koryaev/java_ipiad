package utils;

public class Settings {
    private static final String URL = "https://www.m24.ru";
    private static final int requestsRetryCount = 3;
    private static final String indexName = "news";
    private int messageCount = 0;
    private static final String linksExchangeName = "ProducerLinks";
    private static final String parsedDataExchangeName = "ParsedData";
    private static final String tag = "NewsParserTag";
    private static final String rabbitName = "rabbitmq";
    private static final String rabbitPassword = "rabbitmq";
    private static final String rabbitHost = "127.0.0.1";
    private static final int rabbitPort = 5672;
    private static final String elasticUrl = "http://localhost:9200";

    public String getURL() {
        return URL;
    }
    public String getIndexName() {
        return indexName;
    }
    public String getElasticUrl() {
        return elasticUrl;
    }
    public String getRabbitName() {
        return rabbitName;
    }
    public String getRabbitPassword() {
        return rabbitPassword;
    }
    public String getRabbitHost() {
        return rabbitHost;
    }
    public int getRabbitPort() {
        return rabbitPort;
    }
    public String getLinksExchangeName() {
        return linksExchangeName;
    }
    public String getParsedDataExchangeName() {
        return parsedDataExchangeName;
    }
    public String getTag() {
        return tag;
    }

    public int getRequestsRetryCount() {
        return requestsRetryCount;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public void incrementCount() {
        messageCount++;
    }

    public void decrementCount() {
        messageCount--;
    }
}
