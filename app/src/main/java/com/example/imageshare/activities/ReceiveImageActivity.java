package com.example.imageshare.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.imageshare.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReceiveImageActivity extends AppCompatActivity {
    private static final String TAG = "ReceiveImageActivity";
    private ImageView ivReceivedImage;
    private Button btnAcceptImage;
    private ArrayList<String> pendingImageIds;
    private ArrayList<String> pendingImagePaths;
    private int currentImageIndex = 0;
    private int currentUserId;
    private static final String PREFS_NAME = "ImageSharePrefs";
    private static final String USER_ID_KEY = "userId";
    // 线程池和主线程处理器
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_image);

        // 返回按钮事件
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 获取当前用户ID
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getInt(USER_ID_KEY, -1);
        Log.d(TAG, "当前接收者ID: " + currentUserId);

        if (currentUserId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        ivReceivedImage = findViewById(R.id.ivReceivedImage);
        btnAcceptImage = findViewById(R.id.btnAcceptImage);
        pendingImageIds = new ArrayList<>();
        pendingImagePaths = new ArrayList<>();

        getPendingImagesFromServer();

        btnAcceptImage.setOnClickListener(v -> acceptImage());
    }

    private void getPendingImagesFromServer() {
        // 构建正确的查询URL（无重复uploads）
        String url = "http://10.0.2.2:5000/get_pending_images?userId=" + currentUserId;
        Log.d(TAG, "查询图片URL: " + url);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        Log.d(TAG, "查询到图片数量: " + jsonArray.length());

                        pendingImageIds.clear();
                        pendingImagePaths.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject image = jsonArray.getJSONObject(i);
                            pendingImageIds.add(image.getString("id"));
                            pendingImagePaths.add(image.getString("image_path"));
                        }

                        if (pendingImagePaths.isEmpty()) {
                            Toast.makeText(this, "没有待接收的图片", Toast.LENGTH_SHORT).show();
                            ivReceivedImage.setVisibility(View.GONE);
                            btnAcceptImage.setVisibility(View.GONE);
                        } else {
                            // 加载第一张图片
                            loadImage(pendingImagePaths.get(0));
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析失败: " + e.getMessage());
                        Toast.makeText(this, "解析失败", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
            Log.e(TAG, "查询失败: " + error.getMessage());
            Toast.makeText(this, "网络错误", Toast.LENGTH_SHORT).show();
        });
        queue.add(stringRequest);
    }

    // 加载图片（核心修复：URL路径正确拼接）
    private void loadImage(String imagePath) {
        // 正确的URL：只拼接一次uploads/
        String imageUrl = "http://10.0.2.2:5000/uploads/" + imagePath;
        Log.d(TAG, "加载图片URL: " + imageUrl);

        // 子线程加载图片
        executor.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000); // 5秒超时
                connection.setReadTimeout(5000);
                connection.connect();

                if (connection.getResponseCode() == 200) {
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();

                    // 主线程更新UI
                    handler.post(() -> {
                        ivReceivedImage.setImageBitmap(bitmap);
                        ivReceivedImage.setVisibility(View.VISIBLE);
                        btnAcceptImage.setVisibility(View.VISIBLE);
                    });
                } else {
                    handler.post(() -> {
                        try {
                            Toast.makeText(this, "图片加载失败（" + connection.getResponseCode() + "）", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "加载图片异常: " + e.getMessage());
                handler.post(() -> Toast.makeText(this, "加载图片异常", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void acceptImage() {
        if (currentImageIndex >= pendingImagePaths.size()) return;

        String imageId = pendingImageIds.get(currentImageIndex);
        String imagePath = pendingImagePaths.get(currentImageIndex);

        // 保存图片
        saveImage(imagePath);
        // 标记为已接收
        markAsReceived(imageId);

        currentImageIndex++;
        if (currentImageIndex < pendingImagePaths.size()) {
            loadImage(pendingImagePaths.get(currentImageIndex));
        } else {
            Toast.makeText(this, "所有图片已处理", Toast.LENGTH_SHORT).show();
            ivReceivedImage.setVisibility(View.GONE);
            btnAcceptImage.setVisibility(View.GONE);
        }
    }

    private void saveImage(String imagePath) {
        String imageUrl = "http://10.0.2.2:5000/uploads/" + imagePath;
        executor.execute(() -> {
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();

                // 保存到相册
                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File imageFile = new File(storageDir, System.currentTimeMillis() + ".jpg");
                FileOutputStream output = new FileOutputStream(imageFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.close();
                input.close();

                // 通知系统相册更新
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(imageFile);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);

                handler.post(() -> Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e(TAG, "保存失败: " + e.getMessage());
                handler.post(() -> Toast.makeText(this, "保存图片失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void markAsReceived(String imageId) {
        String url = "http://10.0.2.2:5000/mark_image_received";
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                response -> {
                    try {
                        if (new JSONObject(response).getBoolean("success")) {
                            Log.d(TAG, "图片 " + imageId + " 标记为已接收");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析标记响应失败: " + e.getMessage());
                    }
                }, error -> Log.e(TAG, "标记失败: " + error.getMessage())) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("image_id", imageId);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}