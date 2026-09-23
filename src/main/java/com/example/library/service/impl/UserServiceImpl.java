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
import com.example.library.util.PasswordUtil;
import com.example.library.vo.LoginVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private PasswordUtil passwordUtil;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = this.getOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        // 用户名不存在与密码错误使用同一提示，避免账号枚举
        if (user == null || !passwordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 历史 MD5 密码在登录成功后静默升级为 PBKDF2，无需用户重置密码
        if (passwordUtil.needsUpgrade(user.getPassword())) {
            User upgrade = new User();
            upgrade.setId(user.getId());
            upgrade.setPassword(passwordUtil.encode(dto.getPassword()));
            this.updateById(upgrade);
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

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
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole("USER");
        this.save(user);

        // 清空密码，避免返回泄露（User.password 上另有 @JsonIgnore 兜底）
        user.setPassword(null);
        return user;
    }
}
