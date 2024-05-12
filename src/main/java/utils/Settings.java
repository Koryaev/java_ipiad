package utils;

public class Settings {
    public static final String URL = "https://www.m24.ru";

    private int messageCount = 0;

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
