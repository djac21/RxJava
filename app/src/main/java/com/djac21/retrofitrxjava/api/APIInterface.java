package com.djac21.retrofitrxjava.api;

import com.djac21.retrofitrxjava.models.NewsModel;

import io.reactivex.Single;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface APIInterface {
    @GET("top-headlines?country=us")
    Single<NewsModel> fetchAllNews(@Header("X-Api-Key") String apiKey, @Query("category") String category);
}