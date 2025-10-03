package com.maxst.ar.sample;

import java.io.Serializable;
//
public class MarkerData implements Serializable {
    private Position position;

    public Normal getNormal() {
        return normal;
    }

    public void setNormal(Normal normal) {
        this.normal = normal;
    }

    private Normal normal;
    private String text;
    private String color;
    private String sensor;
    private String value;
    private boolean visible;

    public static class Position implements Serializable {
        public float x;
        public float y;
        public float z;
    }

    public static class Normal implements Serializable {
        public float x;
        public float y;
        public float z;
    }

    public Position getPosition() { return position; }

    public String getText() { return text; }
    public String getColor() { return color; }
    public String getSensor() { return sensor; }
    public String getValue() { return value; }
    public boolean isVisible() { return visible; }
}






//
//public class MarkerData implements Serializable {
//
//    private Position position;
//    private Position normal;
//    private String text;
//    private String color;
//    private String sensor;
//    private String value;
//    private boolean visible;
//
//    public static class Position implements Serializable {
//        public float x;
//        public float y;
//        public float z;
//
//        public Position() {}
//
//        public Position(float x, float y, float z) {
//            this.x = x;
//            this.y = y;
//            this.z = z;
//        }
//
//        @Override
//        public String toString() {
//            return "Position{" +
//                    "x=" + x +
//                    ", y=" + y +
//                    ", z=" + z +
//                    '}';
//        }
//    }
//
//    public Position getPosition() {
//        return position;
//    }
//
//    public void setPosition(Position position) {
//        this.position = position;
//    }
//
//    public Position getNormal() {
//        return normal;
//    }
//
//    public void setNormal(Position normal) {
//        this.normal = normal;
//    }
//
//    public String getText() {
//        return text;
//    }
//
//    public void setText(String text) {
//        this.text = text;
//    }
//
//    public String getColor() {
//        return color;
//    }
//
//    public void setColor(String color) {
//        this.color = color;
//    }
//
//    public String getSensor() {
//        return sensor;
//    }
//
//    public void setSensor(String sensor) {
//        this.sensor = sensor;
//    }
//
//    public String getValue() {
//        return value;
//    }
//
//    public void setValue(String value) {
//        this.value = value;
//    }
//
//    public boolean isVisible() {
//        return visible;
//    }
//
//    public void setVisible(boolean visible) {
//        this.visible = visible;
//    }
//}
