package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 借书请求参数
 */
@Data
public class BorrowDTO {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;
}
