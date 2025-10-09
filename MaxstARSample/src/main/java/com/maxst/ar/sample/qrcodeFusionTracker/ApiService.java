package com.maxst.ar.sample.qrcodeFusionTracker;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;


public interface ApiService {

    @GET("stepData/{stepId}")
    Call<JsonObject> getStepItems(
            @Path("stepId") String stepId,
            @Header("Authorization") String authToken
    );
}


