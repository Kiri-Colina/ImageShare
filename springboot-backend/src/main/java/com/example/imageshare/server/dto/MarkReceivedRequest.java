package com.example.imageshare.server.dto;

/** mark_image_received 请求体：{"imageId": "..."} */
public class MarkReceivedRequest {

    private String imageId;

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }
}
