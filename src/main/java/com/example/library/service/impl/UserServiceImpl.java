package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.common.BusinessException;
import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.entity.User;
import com.example.library.mapper.UserMapper;
import com.example.library.service.UserService;
import com.example.library.util.JwtUtil;
import com.example.library.vo.LoginVO;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = this.getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !user.getPassword().equals(md5(dto.getPassword()))) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole());
        return vo;
    }

    @Override
    public User register(RegisterDTO dto) {
        long count = this.count(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(md5(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole("USER");
        this.save(user);

        // 清空密码，避免返回泄露
        user.setPassword(null);
        return user;
    }

    private String md5(String raw) {
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
