package com.thinkai4j.core.model;

import java.util.List;

public class EmbeddingRequest {

    private String model;
    private List<String> input;

    public EmbeddingRequest() {
    }

    public EmbeddingRequest(List<String> input) {
        this.input = input;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getInput() {
        return input;
    }

    public void setInput(List<String> input) {
        this.input = input;
    }
}
