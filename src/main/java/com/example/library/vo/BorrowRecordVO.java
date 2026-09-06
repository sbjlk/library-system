package com.example.library.vo;

import lombok.Data;

import java.util.Date;

/**
 * 借阅记录视图（含用户名与书名）
 */
@Data
public class BorrowRecordVO {

    private Long id;

    private Long userId;

    private Long bookId;

    /** 借阅用户名 */
    private String username;

    /** 图书书名 */
    private String bookTitle;

    private Date borrowTime;

    private Date dueTime;

    private Date returnTime;

    /** 状态：0 借出中，1 已归还 */
    private Integer status;
}
