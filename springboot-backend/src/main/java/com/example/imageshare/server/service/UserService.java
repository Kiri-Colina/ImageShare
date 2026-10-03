package com.example.imageshare.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.imageshare.server.dto.AuthRequest;
import com.example.imageshare.server.entity.User;
import com.example.imageshare.server.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 注册结果：成功 / 参数缺失 / 用户名已存在 */
    public enum RegisterResult {
        SUCCESS, MISSING_FIELDS, USERNAME_EXISTS
    }

    @Transactional
    public RegisterResult register(AuthRequest request) {
        if (isBlank(request.getUsername()) || isBlank(request.getPassword()) || isBlank(request.getDisplayName())) {
            return RegisterResult.MISSING_FIELDS;
        }
        if (userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()))) {
            return RegisterResult.USERNAME_EXISTS;
        }
        // 密码使用 BCrypt 加密存储
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName()
        );
        userMapper.insert(user);
        return RegisterResult.SUCCESS;
    }

    /** 登录校验：用户名 + BCrypt 密码匹配，成功返回用户 */
    public Optional<User> login(AuthRequest request) {
        if (isBlank(request.getUsername()) || isBlank(request.getPassword())) {
            return Optional.empty();
        }
        return Optional.ofNullable(userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())))
                .filter(user -> passwordEncoder.matches(request.getPassword(), user.getPassword()));
    }

    public List<User> getAllUsers() {
        return userMapper.selectList(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
