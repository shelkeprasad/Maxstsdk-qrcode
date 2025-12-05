package com.maxst.ar.sample;

import java.util.List;

public class TrackerConfig {
    public String id;
    public String name;
    public String mapPath;
    public Assets assets;
    public static class Assets {
        public List<ModelConfig> models;
        public List<VideoConfig> videos;
        public List<LabelConfig> labels;
    }
}
