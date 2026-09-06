package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 图书实体
 */
@Data
@TableName("book")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String author;

    private String isbn;

    private String publisher;

    private String category;

    /** 馆藏总数 */
    private Integer totalCount;

    /** 可借数量 */
    private Integer availableCount;

    /** 状态：1 上架，0 下架 */
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
