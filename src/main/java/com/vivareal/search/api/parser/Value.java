package com.vivareal.search.api.parser;

public class Value {

    private String content;

    public Value() {
        // do nothing
    }

    public Value(String content) {
        this.setContent(content);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}