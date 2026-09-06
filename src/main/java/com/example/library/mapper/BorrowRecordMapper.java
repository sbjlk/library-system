package com.example.library.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.library.entity.BorrowRecord;
import com.example.library.vo.BorrowRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 借阅记录 Mapper
 */
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 分页查询借阅记录，联表查询用户名与书名
     */
    @Select("<script>" +
            "SELECT r.id, r.user_id, r.book_id, r.borrow_time, r.due_time, r.return_time, r.status, " +
            "u.username AS username, b.title AS book_title " +
            "FROM borrow_record r " +
            "LEFT JOIN `user` u ON r.user_id = u.id " +
            "LEFT JOIN book b ON r.book_id = b.id " +
            "<where>" +
            "  <if test='userId != null'> AND r.user_id = #{userId} </if>" +
            "  <if test='status != null'> AND r.status = #{status} </if>" +
            "</where>" +
            " ORDER BY r.id DESC" +
            "</script>")
    IPage<BorrowRecordVO> selectBorrowPage(Page<BorrowRecordVO> page,
                                           @Param("userId") Long userId,
                                           @Param("status") Integer status);
}
