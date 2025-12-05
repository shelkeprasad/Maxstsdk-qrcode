package com.maxst.ar.sample;

public class ModelConfig {
    public String id;
    public String name;
    public String path;
    public boolean initialEnabled = false;
    public float size = 1.0f;
    public float scaleRelativeToTargetWidth = 1.0f;
    public AnimationConfig animation;

    public static class AnimationConfig {
        public boolean autoPlay;
        public boolean loop;
        public int animationIndex;
    }
}