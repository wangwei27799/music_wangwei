package com.wangwei.music_wangwei.api;

import com.wangwei.music_wangwei.response.HomePageResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("music/homePage")
    Call<HomePageResponse> getHomePageData(
            @Query("current") int current,
            @Query("size") int size
    );
}