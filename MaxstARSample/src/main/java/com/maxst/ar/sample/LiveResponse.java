package com.maxst.ar.sample;

import java.util.List;

public class LiveResponse {

    private boolean success;
    private List<DataLive> data;
    private String message;

    public boolean isSuccess() { return success; }
    public List<DataLive> getData() { return data; }
    public String getMessage() { return message; }
}
