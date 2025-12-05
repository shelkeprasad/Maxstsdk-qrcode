package com.maxst.ar.sample;

public class VideoConfig {
    public String id;
    public String name;
    public String path;
    public boolean initialEnabled = false;
    public Scale scale;
    public Translate translate;

    public static class Scale {
        public float widthFactor = 1f;
        public float heightFactor = 1f;
    }

    public static class Translate {
        public float xFactor = 0f;
        public float yFactor = 0f;
        public float z = 0f;
    }
}