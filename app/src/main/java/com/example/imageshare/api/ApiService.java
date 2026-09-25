package com.example.imageshare.api;

import com.example.imageshare.models.LoginResponse;
import com.example.imageshare.models.User;

import org.json.JSONObject;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

public interface ApiService {
    @GET
    Call<ResponseBody> getPendingImages(@Url String url);

    @POST("mark_image_received")
    Call<ResponseBody> markImageReceived(@Body JSONObject imageId);
    @POST("register")
    Call<ResponseBody> register(@Body User user);

    // 登录接口返回类型改为LoginResponse
    @POST("login")
    Call<LoginResponse> login(@Body User user);

    @GET("users")
    Call<List<User>> getAllUsers();

    @Multipart
    @POST("upload_image")
    Call<ResponseBody> uploadImage(
            @Part MultipartBody.Part image,
            @Part("recipientId") RequestBody recipientId,
            @Part("senderId") RequestBody senderId
    );
}