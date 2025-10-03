package com.maxst.ar.sample;

import com.google.gson.annotations.SerializedName;

public class Movie {
    @SerializedName("trackName")
    private String trackName;

    @SerializedName("artistName")
    private String artistName;

    @SerializedName("collectionName")
    private String collectionName;

    @SerializedName("trackViewUrl")
    private String trackViewUrl;

    @SerializedName("artworkUrl100")
    private String artworkUrl;

    public String getTrackName() {
        return trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public String getTrackViewUrl() {
        return trackViewUrl;
    }

    public String getArtworkUrl() {
        return artworkUrl;
    }
}

