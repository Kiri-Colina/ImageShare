package com.example.imageshare.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // 模拟器访问宿主机用 10.0.2.2；真机请改成后端所在电脑的局域网 IP，且必须以 / 结尾
    private static final String BASE_URL = "http://10.0.2.2:5000/";
    private static Retrofit retrofit = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}

