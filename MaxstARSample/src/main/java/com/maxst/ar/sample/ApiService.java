package com.maxst.ar.sample;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    @GET("search")
    Call<ApiResponse> searchMovies(@Query("term") String searchTerm);


    // TODo 3d model
    @GET("model/getFromMachine/{machineId}")
    Call<ModelResponse> getModels(@Path("machineId") String machineId);

    @GET("livedata/getFromMachine/{id}")
    Call <LiveResponse> getLivesFromMachine(@Path("id") String id);
}