package com.sk.mymassenger.pixabay;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface PixabayApi {

    @GET("api")
    Call<ImageResponse> getImages(@Query("key") String key, @Query("q") String query);
}
