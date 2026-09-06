package com.example.library.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 借阅记录实体
 */
@Data
@TableName("borrow_record")
public class BorrowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    /** 借出时间 */
    private Date borrowTime;

    /** 应还时间 */
    private Date dueTime;

    /** 实际归还时间 */
    private Date returnTime;

    /** 状态：0 借出中，1 已归还 */
    private Integer status;

    private Date createdAt;

    private Date updatedAt;
}
