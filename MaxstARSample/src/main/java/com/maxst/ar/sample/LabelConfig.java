package com.maxst.ar.sample;

public class LabelConfig {
    public String id;
    public String text;
    public String icon;
    public Anchor anchor;
    public OffsetPx offsetPx;
    public OnClickAction onClickAction;
    public static class Anchor {
        public float xRel, yRel, zRel;
    }

    public static class OffsetPx {
        public int x, y;
    }

    public static class OnClickAction {
        public String type;
        public String targetModelId;
        public String targetVideoId;
        public Boolean enableModel;

    }
}