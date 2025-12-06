package com.maxst.ar.sample;

public class VideoConfig {
    public String id;
    public String name;
    public String path;
    public int type;
    public boolean initialEnabled = false;
    public Scale scale;
    public Translate translate;

    public static class Scale {
        public float width = 1f;
        public float height = 1f;
    }

    public static class Translate {
        public float x = 0f;
        public float y = 0f;
        public float z = 0f;
    }
}