package com.example.imageshare.server.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 共享图片实体，对应 shared_images 表。
 */
@TableName("shared_images")
public class SharedImage {

    /** UUID 主键，由业务代码生成后写入 */
    @TableId(type = IdType.INPUT)
    private String id;

    private Long senderId;

    private Long recipientId;

    private String imagePath;

    /** 对应列 is_received */
    @TableField("is_received")
    private boolean received = false;

    public SharedImage() {
    }

    public SharedImage(String id, Long senderId, Long recipientId, String imagePath) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.imagePath = imagePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isReceived() {
        return received;
    }

    public void setReceived(boolean received) {
        this.received = received;
    }
}
