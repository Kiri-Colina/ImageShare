package com.example.imageshare.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 待接收图片列表项，字段名沿用 Flask 版本的 snake_case，客户端无需改动 */
public class PendingImageDto {

    private final String id;

    @JsonProperty("sender_id")
    private final Long senderId;

    @JsonProperty("image_path")
    private final String imagePath;

    public PendingImageDto(String id, Long senderId, String imagePath) {
        this.id = id;
        this.senderId = senderId;
        this.imagePath = imagePath;
    }

    public String getId() {
        return id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getImagePath() {
        return imagePath;
    }
}
