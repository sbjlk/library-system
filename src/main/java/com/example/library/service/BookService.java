package com.example.library.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.library.dto.BookDTO;
import com.example.library.entity.Book;

/**
 * 图书服务接口
 */
public interface BookService extends IService<Book> {

    /**
     * 新增图书
     */
    void addBook(BookDTO dto);

    /**
     * 修改图书
     */
    void updateBook(Long id, BookDTO dto);

    /**
     * 删除图书
     */
    void deleteBook(Long id);
}
