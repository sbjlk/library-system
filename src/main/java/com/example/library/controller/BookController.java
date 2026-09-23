package com.example.library.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.annotation.RequireRole;
import com.example.library.common.Result;
import com.example.library.dto.BookDTO;
import com.example.library.entity.Book;
import com.example.library.service.BookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 图书接口
 */
@Api(tags = "图书管理")
@RestController
@RequestMapping("/api/book")
public class BookController {

    @Resource
    private BookService bookService;

    @ApiOperation("分页查询图书列表")
    @GetMapping("/list")
    public Result<IPage<Book>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String author,
                                    @RequestParam(required = false) String category) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        return Result.success(bookService.page(page, Wrappers.<Book>lambdaQuery()
                .like(StringUtils.hasText(title), Book::getTitle, title)
                .like(StringUtils.hasText(author), Book::getAuthor, author)
                .eq(StringUtils.hasText(category), Book::getCategory, category)
                .orderByDesc(Book::getId)));
    }

    @ApiOperation("根据ID查询图书")
    @GetMapping("/{id}")
    public Result<Book> getById(@PathVariable Long id) {
        return Result.success(bookService.getById(id));
    }

    @ApiOperation("新增图书")
    @PostMapping
    @RequireRole("ADMIN")
    public Result<Void> add(@RequestBody @Valid BookDTO dto) {
        bookService.addBook(dto);
        return Result.success();
    }

    @ApiOperation("修改图书")
    @PutMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid BookDTO dto) {
        bookService.updateBook(id, dto);
        return Result.success();
    }

    @ApiOperation("删除图书")
    @DeleteMapping("/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }
}
