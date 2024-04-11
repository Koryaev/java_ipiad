package dataClasses;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class NewsData {
    private String url;
    private String body;
    private String header;
    private String rubric;
    private String rubric_url;
    private String time;

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

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
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

    public void printData() {
        String text = String.format("Time: %s | URL: %s\nRubric: %s\nHeader: %s",
                this.time, this.url, this.rubric, this.header);
        System.out.println(text);
    }

    @Override
    public String toString() {
        return "NewsData{" +
                "url='" + url + '\'' +
                ", header='" + header + '\'' +
                ", rubric='" + rubric + '\'' +
                ", rubric_url='" + rubric_url + '\'' +
                ", time='" + time + '\'' +
                '}';
    }

    public String toStrJson() throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(this);
    }
}
