package com.example.library.common;

/**
 * 业务异常。
 *
 * <p>默认错误码为 400（客户端请求不合法），而不是 500。
 * 500 的语义是"服务端系统异常"，若把业务规则拒绝（如"库存不足""重复借阅"）
 * 也标成 500，会导致前端与监控无法区分"用户操作有误"和"服务真的挂了"，
 * 也会让告警系统被正常的业务拒绝淹没。</p>
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        this(ResultCode.BAD_REQUEST.getCode(), message);
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
