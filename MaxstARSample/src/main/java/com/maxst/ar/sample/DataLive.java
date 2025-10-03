package com.maxst.ar.sample;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DataLive {

    private String _id = "";
    private String tag = "";
    private String value = "";
    private String unit = "";
    private int type = 1;
    private String min = "";
    private String max = "";
    private String time = "";
  //  private ArrayList<JsonObject> previous = new ArrayList<>();

    @SerializedName("previous")
    private List<Previous> previous;

    public List<Previous> getPrevious() {
        return previous;
    }
    private String mid = "";

    public DataLive() {
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getMin() {
        return min;
    }

    public void setMin(String min) {
        this.min = min;
    }

    public String getMax() {
        return max;
    }

    public void setMax(String max) {
        this.max = max;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

//    public ArrayList<JsonObject> getPrevious() {
//        return previous;
//    }
//
//    public void setPrevious(ArrayList<JsonObject> previous) {
//        this.previous = previous;
//    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    //

    public class Previous {
        @SerializedName("time")
        private String time;

        @SerializedName("value")
        private String value;

        public String getTime() {
            return time;
        }

        public String getValue() {
            return value;
        }
    }

}
