package dataClasses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class NewsData {
    private String url;
    private String body;
    private String title;
    private String rubric;
    private String rubric_url;
    private String time;
    private String hash;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRubric() {
        return rubric;
    }

    public void setRubric(String rubric) {
        this.rubric = rubric;
    }

    public String getRubric_url() {
        return rubric_url;
    }

    public void setRubric_url(String rubric_url) {
        this.rubric_url = rubric_url;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "NewsData{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", rubric='" + rubric + '\'' +
                ", rubric_url='" + rubric_url + '\'' +
                ", time='" + time + '\'' +
                '}';
    }

    public String toStrJson() throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(this);
    }

    public void fromStrJson(String strJson) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(strJson);

        this.url = node.get("url").asText();
        this.body = node.get("body").asText();
        this.title = node.get("title").asText();
        this.rubric = node.get("rubric").asText();
        this.rubric_url = node.get("rubric_url").asText();
        this.time = node.get("time").asText();
        this.hash = node.get("hash").asText();
    }
}
