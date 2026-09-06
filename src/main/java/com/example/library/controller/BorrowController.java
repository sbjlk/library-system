package com.example.library.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.library.common.Result;
import com.example.library.common.UserContext;
import com.example.library.dto.BorrowDTO;
import com.example.library.dto.ReturnDTO;
import com.example.library.entity.BorrowRecord;
import com.example.library.service.BorrowService;
import com.example.library.vo.BorrowRecordVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * 借阅接口
 */
@Api(tags = "借阅管理")
@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    @Resource
    private BorrowService borrowService;

    @ApiOperation("借书")
    @PostMapping("/borrow")
    public Result<BorrowRecord> borrow(@RequestBody @Valid BorrowDTO dto) {
        return Result.success(borrowService.borrowBook(dto.getBookId()));
    }

    @ApiOperation("还书")
    @PostMapping("/return")
    public Result<Void> returnBook(@RequestBody @Valid ReturnDTO dto) {
        borrowService.returnBook(dto.getRecordId());
        return Result.success();
    }

    @ApiOperation("分页查询借阅记录")
    @GetMapping("/list")
    public Result<IPage<BorrowRecordVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) Long userId,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(borrowService.pageBorrow(pageNum, pageSize, userId, status));
    }

    @ApiOperation("查询我的借阅记录")
    @GetMapping("/my")
    public Result<IPage<BorrowRecordVO>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(borrowService.pageBorrow(pageNum, pageSize, UserContext.getUserId(), null));
    }
}
