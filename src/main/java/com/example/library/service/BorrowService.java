package com.example.library.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.entity.BorrowRecord;
import com.example.library.vo.BorrowRecordVO;

/**
 * 借阅服务接口
 */
public interface BorrowService extends IService<BorrowRecord> {

    /**
     * 借书
     */
    BorrowRecord borrowBook(Long bookId);

    /**
     * 还书
     */
    void returnBook(Long recordId);

    /**
     * 分页查询借阅记录
     */
    IPage<BorrowRecordVO> pageBorrow(Integer pageNum, Integer pageSize, Long userId, Integer status);
}
