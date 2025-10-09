package com.maxst.ar.sample.qrcodeFusionTracker;

class ARItem {
    String id;
    String type;
    String content;
    Position position;
    Properties properties;
}

class Position {
    float x;
    float y;
    float z;
}

class Properties {
    int fontSize;          // For text, image, and video
    String color;          // For text, image, and video
    float width;           // For image/video
    float height;          // For image/video
    boolean autoplay;      // For video only
    String visibility;     // "visible" or "hidden"
}