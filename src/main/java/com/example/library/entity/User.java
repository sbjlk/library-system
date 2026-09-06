package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

/**
 * 用户实体
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码（MD5 加密），序列化时忽略，避免泄露 */
    @JsonIgnore
    private String password;

    private String nickname;

    /** 角色：USER / ADMIN */
    private String role;

    private Date createdAt;

    private Date updatedAt;
}
