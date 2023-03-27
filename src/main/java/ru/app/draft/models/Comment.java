package ru.app.draft.models;

import java.io.Serializable;
import java.util.UUID;

public class Comment implements Serializable {
    String id = UUID.randomUUID().toString();
    Integer number;
    String author;
    String date;
    String content;

    public Comment() {
    }

    public Comment(String author, String date, String content, Integer number) {
        this.author = author;
        this.date = date;
        this.content = content;
        this.number = number;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
