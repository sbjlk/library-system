package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.common.BusinessException;
import com.example.library.common.ResultCode;
import com.example.library.common.UserContext;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.entity.User;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.service.BookService;
import com.example.library.service.BorrowService;
import com.example.library.service.UserService;
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

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BorrowRecord borrowBook(Long bookId) {
        Long userId = UserContext.getUserId();
        Book book = bookService.getById(bookId);
        if (book == null) {
            throw new BusinessException("图书不存在");
        }
        if (book.getStatus() != null && book.getStatus() == 0) {
            throw new BusinessException("该图书已下架，无法借阅");
        }

        // 业务规则：同一用户不能重复借阅同一本尚未归还的书
        Long borrowing = borrowRecordMapper.selectCount(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getBookId, bookId)
                .eq(BorrowRecord::getStatus, 0));
        if (borrowing != null && borrowing > 0) {
            throw new BusinessException("你已借阅该书且尚未归还");
        }

        // 原子扣减库存，避免并发超借：
        // 把"判断库存 > 0"和"扣减库存"合并成一条 SQL，由数据库行锁保证互斥，
        // 受影响行数为 0 即代表库存已被别人抢完
        boolean deducted = bookService.update(Wrappers.<Book>lambdaUpdate()
                .eq(Book::getId, bookId)
                .gt(Book::getAvailableCount, 0)
                .setSql("available_count = available_count - 1"));
        if (!deducted) {
            throw new BusinessException("库存不足，无法借阅");
        }

        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
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
        Long userId = UserContext.getUserId();
        BorrowRecord record = this.getById(recordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BusinessException("该记录已归还");
        }

        // 越权校验：普通用户只能归还自己的借阅记录，管理员可代还
        if (!isAdmin(userId) && !record.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作他人的借阅记录");
        }

        record.setReturnTime(new Date());
        record.setStatus(1);
        this.updateById(record);

        // 归还后回补库存，并限制不超过馆藏总数（防止 total_count 被改小后库存溢出）
        bookService.update(Wrappers.<Book>lambdaUpdate()
                .eq(Book::getId, record.getBookId())
                .apply("available_count < total_count")
                .setSql("available_count = available_count + 1"));
    }

    /**
     * 判断当前登录用户是否为管理员。
     * <p>以数据库中的最新角色为准（而非 token 中的快照），避免用户角色变更后旧 token 仍按旧角色放行。</p>
     */
    private boolean isAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userService.getById(userId);
        return user != null && "ADMIN".equals(user.getRole());
    }

    @Override
    public IPage<BorrowRecordVO> pageBorrow(Integer pageNum, Integer pageSize, Long userId, Integer status) {
        Page<BorrowRecordVO> page = new Page<>(pageNum, pageSize);
        return borrowRecordMapper.selectBorrowPage(page, userId, status);
    }
}
