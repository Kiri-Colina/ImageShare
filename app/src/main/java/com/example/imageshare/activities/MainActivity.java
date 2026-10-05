package com.example.imageshare.activities;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.imageshare.R;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int REQUEST_SELECT_USER = 1;
    private static final String PREFS_NAME = "ImageSharePrefs";
    private static final String USER_ID_KEY = "userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 返回按钮事件 - 修改为跳转到登录界面
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            // 清除登录状态（可选，根据需求决定是否需要）
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove(USER_ID_KEY).apply();

            // 跳转到登录界面
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            // 清除活动栈，避免返回时回到主界面
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // 关闭当前主界面
        });

        Button btnShareImage = findViewById(R.id.btnShareImage);
        Button btnReceiveImage = findViewById(R.id.btnReceiveImage);

        btnShareImage.setOnClickListener(v -> {
            if (checkStoragePermission()) {
                startUserListActivity();
            } else {
                requestStoragePermission();
            }
        });

        btnReceiveImage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ReceiveImageActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                    REQUEST_CODE_PERMISSION
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_PERMISSION
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUserListActivity();
            } else {
                Toast.makeText(this, "需要权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startUserListActivity() {
        Intent userListIntent = new Intent(MainActivity.this, UserListActivity.class);
        startActivityForResult(userListIntent, REQUEST_SELECT_USER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SELECT_USER && resultCode == RESULT_OK) {
            // 用户已选择，可在此处添加处理逻辑
        }
    }
}