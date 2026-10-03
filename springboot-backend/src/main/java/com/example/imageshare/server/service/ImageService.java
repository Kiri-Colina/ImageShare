package com.example.imageshare.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.imageshare.server.dto.PendingImageDto;
import com.example.imageshare.server.entity.SharedImage;
import com.example.imageshare.server.mapper.SharedImageMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private final SharedImageMapper sharedImageMapper;
    private final Path uploadFolder;

    public ImageService(SharedImageMapper sharedImageMapper,
                        @Value("${app.upload-dir}") String uploadDir) {
        this.sharedImageMapper = sharedImageMapper;
        this.uploadFolder = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(uploadFolder);
        log.info("图片存储目录: {}", uploadFolder);
    }

    /** 服务关闭时清理所有未接收图片（文件 + 数据库记录） */
    @PreDestroy
    public void cleanupPendingImages() {
        log.info("开始清理未接收图片...");
        int deleted = 0;
        for (SharedImage image : sharedImageMapper.selectList(new LambdaQueryWrapper<SharedImage>()
                .eq(SharedImage::isReceived, false))) {
            if (deleteFileQuietly(image.getImagePath())) {
                deleted++;
            }
            sharedImageMapper.deleteById(image.getId());
        }
        log.info("已清理 {} 张未接收图片及相关数据库记录", deleted);
    }

    /**
     * 保存上传图片并写入数据库；数据库写入失败时回滚删除已保存的文件。
     *
     * @return 新图片的 UUID
     */
    @Transactional
    public String uploadImage(MultipartFile image, Long senderId, Long recipientId) throws IOException {
        String imageId = UUID.randomUUID().toString();
        Path target = uploadFolder.resolve(imageId + ".jpg");
        try (var in = image.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            SharedImage entity = new SharedImage(imageId, senderId, recipientId, target.toString());
            sharedImageMapper.insert(entity);
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        log.info("图片上传 - 图片ID: {}, 发送者ID: {}, 接收者ID: {}", imageId, senderId, recipientId);
        return imageId;
    }

    /** 查询某用户待接收的图片列表（文件名只返回 basename） */
    public List<PendingImageDto> getPendingImages(Long userId) {
        List<SharedImage> images = sharedImageMapper.selectList(new LambdaQueryWrapper<SharedImage>()
                .eq(SharedImage::getRecipientId, userId)
                .eq(SharedImage::isReceived, false));
        log.info("查询待接收图片 - 用户ID: {}, 找到 {} 张", userId, images.size());
        return images.stream()
                .map(img -> new PendingImageDto(
                        img.getId(),
                        img.getSenderId(),
                        Paths.get(img.getImagePath()).getFileName().toString()))
                .toList();
    }

    /**
     * 标记图片为已接收并删除文件。
     *
     * @return true 处理成功；false 图片不存在或已被接收
     */
    @Transactional
    public boolean markImageReceived(String imageId) {
        SharedImage image = sharedImageMapper.selectById(imageId);
        if (image == null || image.isReceived()) {
            return false;
        }
        image.setReceived(true);
        sharedImageMapper.updateById(image);
        deleteFileQuietly(image.getImagePath());
        log.info("图片已接收并删除: {}", image.getImagePath());
        return true;
    }

    /** 按文件名读取图片字节（用于 /uploads/{filename}），文件不存在返回 empty */
    public Optional<byte[]> loadImage(String filename) throws IOException {
        // 防止路径穿越，只允许纯文件名
        Path file = uploadFolder.resolve(filename).normalize();
        if (!file.startsWith(uploadFolder) || !Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readAllBytes(file));
    }

    private boolean deleteFileQuietly(String path) {
        try {
            return Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("删除图片失败 {}: {}", path, e.getMessage());
            return false;
        }
    }
}

