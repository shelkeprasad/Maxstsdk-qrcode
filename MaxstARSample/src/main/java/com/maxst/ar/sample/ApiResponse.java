package com.maxst.ar.sample;

import java.util.List;

public class ApiResponse {
    private int resultCount;
    private List<MovieItem> results;

    public int getResultCount() {
        return resultCount;
    }

    public List<MovieItem> getResults() {
        return results;
    }
}

