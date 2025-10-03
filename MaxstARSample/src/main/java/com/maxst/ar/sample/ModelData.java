package com.maxst.ar.sample;

import java.io.Serializable;
import java.util.List;

public class ModelData implements Serializable {
    private String _id;
    private String desc;
    private String mid;
    private String url;
    private String name;
    private String created_at;

    public List<MarkerData> getMarkers() {
        return markers;
    }

    public void setMarkers(List<MarkerData> markers) {
        this.markers = markers;
    }

    private List<MarkerData> markers;

    public String getId() { return _id; }
    public String getDesc() { return desc; }
    public String getMid() { return mid; }
    public String getUrl() { return url; }
    public String getName() { return name; }
    public String getCreatedAt() { return created_at; }
}

