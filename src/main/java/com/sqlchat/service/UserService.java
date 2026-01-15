package com.sqlchat.service;

import com.sqlchat.entity.User;
import com.sqlchat.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 用户服务
 * @author sqlChat
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 用户注册
     */
    public User register(String username, String password, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setEmail(email);

        return userRepository.save(user);
    }

    /**
     * 用户登录
     */
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        if (!user.getPassword().equals(encryptPassword(password))) {
            throw new RuntimeException("用户名或密码错误");
        }

        return user;
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    /**
     * 加密密码（简单MD5，生产环境应使用BCrypt）
     */
    private String encryptPassword(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }
}
