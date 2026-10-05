package com.example.imageshare.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.imageshare.R;
import com.example.imageshare.api.ApiClient;
import com.example.imageshare.api.ApiService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;

public class UserListActivity extends AppCompatActivity {
    private static final String TAG = "UserListActivity";
    private ListView lvUsers;
    private ArrayList<String> userNames;
    private ArrayList<Integer> userIds;
    private int selectedUserId;
    private int currentUserId;
    private static final int REQUEST_PICK_IMAGE = 1;
    private static final String PREFS_NAME = "ImageSharePrefs";
    private static final String USER_ID_KEY = "userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        // 返回按钮事件
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // 获取当前用户ID并打印
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getInt(USER_ID_KEY, -1);
        Log.d(TAG, "当前发送者ID: " + currentUserId);

        if (currentUserId == -1) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        lvUsers = findViewById(R.id.lvUsers);
        userNames = new ArrayList<>();
        userIds = new ArrayList<>();

        getUsersFromServer();

        lvUsers.setOnItemClickListener((parent, view, position, id) -> {
            selectedUserId = userIds.get(position);
            // 打印选中的接收者ID
            Log.d(TAG, "选中的接收者ID: " + selectedUserId);
            pickImageFromGallery();
        });
    }

    private void getUsersFromServer() {
        String url = "http://10.0.2.2:5000/users";
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONArray jsonArray = new JSONArray(response);
                        userNames.clear();
                        userIds.clear();
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject user = jsonArray.getJSONObject(i);
                            int id = user.getInt("id");
                            String name = user.getString("username");
                            userNames.add(name);
                            userIds.add(id);
                            // 打印获取到的用户ID
                            Log.d(TAG, "用户列表 - ID: " + id + ", 用户名: " + name);
                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_list_item_1, userNames
                        ) {
                            @Override
                            public View getView(int position, View convertView, ViewGroup parent) {
                                View view = super.getView(position, convertView, parent);
                                TextView textView = view.findViewById(android.R.id.text1);
                                textView.setTextColor(getResources().getColor(R.color.purple_500));
                                return view;
                            }
                        };
                        lvUsers.setAdapter(adapter);
                    } catch (JSONException e) {
                        Log.e(TAG, "解析用户列表失败: " + e.getMessage());
                        Toast.makeText(this, "解析用户列表失败", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
            Log.e(TAG, "获取用户列表失败: " + error.getMessage());
            Toast.makeText(this, "获取用户列表失败", Toast.LENGTH_SHORT).show();
        });
        queue.add(stringRequest);
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            String picturePath = getRealPathFromURI(selectedImage);
            if (picturePath != null) {
                Log.d(TAG, "图片路径: " + picturePath);
                uploadImageToServer(picturePath);
            } else {
                Toast.makeText(this, "无法获取图片路径", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null) return null;
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(columnIndex);
        cursor.close();
        return path;
    }

    private void uploadImageToServer(String picturePath) {
        File imageFile = new File(picturePath);
        if (!imageFile.exists()) {
            Toast.makeText(this, "图片文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        RequestBody recipientIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(selectedUserId));
        RequestBody senderIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(currentUserId));

        ApiService apiService = ApiClient.getApiService();
        Call<ResponseBody> call = apiService.uploadImage(imagePart, recipientIdBody, senderIdBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseStr = response.body().string();
                        JSONObject json = new JSONObject(responseStr);
                        if (json.getBoolean("success")) {
                            Toast.makeText(UserListActivity.this, "图片上传成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(UserListActivity.this, "上传失败: " + json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析响应失败: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "上传失败，响应码: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "网络错误: " + t.getMessage());
            }
        });
    }
}