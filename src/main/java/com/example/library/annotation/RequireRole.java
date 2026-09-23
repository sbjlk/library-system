package com.example.library.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级角色要求注解。
 *
 * <p>标注在 Controller 类或方法上，由 {@code JwtInterceptor} 在 preHandle 阶段读取并校验：
 * 当前登录用户的角色必须命中 {@link #value()} 中的任意一项，否则返回 403。</p>
 *
 * <p>设计说明：认证（你是谁）由拦截器统一完成，授权（你能不能做）用注解声明在接口上，
 * 避免在业务代码里散落 if 判断。不标注该注解的接口默认"登录即可访问"。</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {

    /**
     * 允许访问的角色列表，命中任意一个即放行。
     */
    String[] value();
}
