package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.entity.User;
import com.example.library.vo.LoginVO;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 注册
     */
    User register(RegisterDTO dto);
}
