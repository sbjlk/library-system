package com.example.library.vo;

import lombok.Data;

/**
 * 登录返回视图
 */
@Data
public class LoginVO {

    private String token;

    private Long id;

    private String username;

    private String nickname;

    private String role;
}
