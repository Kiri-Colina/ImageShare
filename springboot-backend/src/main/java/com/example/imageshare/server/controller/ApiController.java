package com.example.imageshare.server.controller;

import com.example.imageshare.server.dto.AuthRequest;
import com.example.imageshare.server.dto.MarkReceivedRequest;
import com.example.imageshare.server.dto.PendingImageDto;
import com.example.imageshare.server.entity.User;
import com.example.imageshare.server.service.ImageService;
import com.example.imageshare.server.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 接口路径 / 字段名 / 状态码与 Flask 版本完全一致，Android 客户端零改动。
 */
@RestController
public class ApiController {

    private final UserService userService;
    private final ImageService imageService;

    public ApiController(UserService userService, ImageService imageService) {
        this.userService = userService;
        this.imageService = imageService;
    }

    @GetMapping("/")
    public String test() {
        return "Server is running!";
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody AuthRequest request) {
        UserService.RegisterResult result = userService.register(request);
        return switch (result) {
            case SUCCESS -> ResponseEntity.ok(
                    Map.of("success", true, "message", "Registration successful"));
            case MISSING_FIELDS -> ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Missing required fields"));
            case USERNAME_EXISTS -> ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Username already exists"));
        };
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody AuthRequest request) {
        if (isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Missing required fields"));
        }
        Optional<User> user = userService.login(request);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid credentials"));
        }
        return ResponseEntity.ok(Map.of("success", true, "user", user.get()));
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping("/upload_image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "recipientId", required = false) String recipientId,
            @RequestParam(value = "senderId", required = false) String senderId) throws IOException {

        if (image == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No image provided"));
        }
        if (image.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "No selected image"));
        }
        Long recipient = parseLong(recipientId);
        Long sender = parseLong(senderId);
        if (recipient == null || sender == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid ID format (must be integer)"));
        }

        imageService.uploadImage(image, sender, recipient);
        return ResponseEntity.ok(
                Map.of("success", true, "message", "Image uploaded successfully"));
    }

    @GetMapping("/uploads/{filename}")
    public ResponseEntity<byte[]> uploadedFile(@PathVariable String filename) throws IOException {
        Optional<byte[]> data = imageService.loadImage(filename);
        return data.map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(bytes))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/get_pending_images")
    public ResponseEntity<?> getPendingImages(@RequestParam(value = "userId", required = false) String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Missing user ID"));
        }
        Long id = parseLong(userId);
        if (id == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid user ID format"));
        }
        List<PendingImageDto> images = imageService.getPendingImages(id);
        return ResponseEntity.ok(images);
    }

    @PostMapping("/mark_image_received")
    public ResponseEntity<Map<String, Object>> markImageReceived(@RequestBody MarkReceivedRequest request) {
        String imageId = request.getImageId();
        if (imageId == null || imageId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Missing image ID"));
        }
        if (!imageService.markImageReceived(imageId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "Image not found or already received"));
        }
        return ResponseEntity.ok(
                Map.of("success", true, "message", "Image received and deleted successfully"));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
