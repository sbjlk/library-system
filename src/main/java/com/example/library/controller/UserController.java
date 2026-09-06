package com.example.library.controller;

import com.example.library.common.Result;
import com.example.library.common.UserContext;
import com.example.library.entity.User;
import com.example.library.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 用户接口
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/info")
    public Result<User> info() {
        User user = userService.getById(UserContext.getUserId());
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
}
