package com.example.library.controller;

import com.example.library.common.Result;
import com.example.library.dto.LoginDTO;
import com.example.library.dto.RegisterDTO;
import com.example.library.entity.User;
import com.example.library.service.UserService;
import com.example.library.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 认证接口
 */
@Api(tags = "认证管理")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    @Resource
    private UserService userService;

    @ApiOperation("登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @ApiOperation("注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody @Valid RegisterDTO dto) {
        return Result.success(userService.register(dto));
    }
}
