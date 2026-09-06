package com.example.library.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 图书新增/修改请求参数
 */
@Data
public class BookDTO {

    @NotBlank(message = "书名不能为空")
    private String title;

    private String author;

    private String isbn;

    private String publisher;

    private String category;

    @NotNull(message = "馆藏数量不能为空")
    @Min(value = 1, message = "馆藏数量至少为 1")
    private Integer totalCount;
}
