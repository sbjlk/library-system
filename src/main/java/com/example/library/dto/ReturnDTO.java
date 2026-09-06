package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 还书请求参数
 */
@Data
public class ReturnDTO {

    @NotNull(message = "借阅记录ID不能为空")
    private Long recordId;
}
