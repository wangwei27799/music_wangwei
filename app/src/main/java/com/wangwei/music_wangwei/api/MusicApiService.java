package com.wangwei.music_wangwei.api;

import com.wangwei.music_wangwei.entity.MusicInfo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MusicApiService {
    @GET("search")
    Call<List<MusicInfo>> searchMusic(@Query("keyword") String keyword);
}
