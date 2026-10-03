package com.example.imageshare.server.dto;

/** 注册 / 登录请求体，字段名与 Android 客户端 User 模型一致 */
public class AuthRequest {

    private String username;
    private String password;
    private String displayName;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
