package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.common.BusinessException;
import com.example.library.common.UserContext;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.vo.BorrowRecordVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 借阅服务实现
 */
@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowService {

    /** 默认借阅天数 */
    private static final long BORROW_DAYS = 30L;

    @Resource
    private BookService bookService;

    @Resource
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord borrowBook(Long bookId) {
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (book.getStatus() != null && book.getStatus() == 0) {
            throw new BusinessException("该图书已下架，无法借阅");
        }

        // 原子扣减库存，避免并发超借
        boolean deducted = bookService.update(Wrappers.<Book>lambdaUpdate()
                .eq(Book::getId, bookId)
                .gt(Book::getAvailableCount, 0)
                .setSql("available_count = available_count - 1"));
        if (!deducted) {
            throw new BusinessException("库存不足，无法借阅");
        }

        BorrowRecord record = new BorrowRecord();
        record.setUserId(UserContext.getUserId());
        record.setBookId(bookId);
        record.setBorrowTime(new Date());
        record.setDueTime(new Date(System.currentTimeMillis() + BORROW_DAYS * 24 * 60 * 60 * 1000));
        record.setStatus(0);
        this.save(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void returnBook(Long recordId) {
        BorrowRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BusinessException("该记录已归还");
        }

        record.setReturnTime(new Date());
        record.setStatus(1);
        this.updateById(record);

        // 归还后回补库存
        bookService.update(Wrappers.<Book>lambdaUpdate()
                .eq(Book::getId, record.getBookId())
                .setSql("available_count = available_count + 1"));
    }

    @Override
    public IPage<BorrowRecordVO> pageBorrow(Integer pageNum, Integer pageSize, Long userId, Integer status) {
        Page<BorrowRecordVO> page = new Page<>(pageNum, pageSize);
        return borrowRecordMapper.selectBorrowPage(page, userId, status);
    }
}
