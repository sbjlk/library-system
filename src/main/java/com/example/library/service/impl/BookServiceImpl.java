package com.example.library.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.library.common.BusinessException;
import com.example.library.dto.BookDTO;
import com.example.library.entity.Book;
import com.example.library.entity.BorrowRecord;
import com.example.library.mapper.BookMapper;
import com.example.library.mapper.BorrowRecordMapper;
import com.example.library.service.BookService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 图书服务实现
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    @Resource
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    public void addBook(BookDTO dto) {
        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        book.setAvailableCount(dto.getTotalCount());
        book.setStatus(1);
        this.save(book);
    }

    @Override
    public void updateBook(Long id, BookDTO dto) {
        Book old = this.getById(id);
        if (old == null) {
            throw new BusinessException("图书不存在");
        }

        // 馆藏数量变化时同步调整可借数量
        int diff = dto.getTotalCount() - old.getTotalCount();

        Book book = new Book();
        BeanUtils.copyProperties(dto, book);
        book.setId(id);
        book.setAvailableCount(Math.max(0, old.getAvailableCount() + diff));
        this.updateById(book);
    }

    @Override
    public void deleteBook(Long id) {
        if (this.getById(id) == null) {
            throw new BusinessException("图书不存在");
        }

        long activeCount = borrowRecordMapper.selectCount(Wrappers.<BorrowRecord>lambdaQuery()
                .eq(BorrowRecord::getBookId, id)
                .eq(BorrowRecord::getStatus, 0));
        if (activeCount > 0) {
            throw new BusinessException("该图书存在未归还的借阅记录，无法删除");
        }

        this.removeById(id);
    }
}
