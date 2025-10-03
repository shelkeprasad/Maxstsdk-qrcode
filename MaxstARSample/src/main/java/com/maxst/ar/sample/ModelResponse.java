package com.maxst.ar.sample;

import java.util.List;

public class ModelResponse {
    private boolean success;
    private List<ModelData> data;
    private String message;

    public boolean isSuccess() { return success; }
    public List<ModelData> getData() { return data; }
    public String getMessage() { return message; }
}

