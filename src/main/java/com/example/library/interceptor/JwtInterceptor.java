package com.example.library.interceptor;

import com.example.library.annotation.RequireRole;
import com.example.library.common.BusinessException;
import com.example.library.common.ResultCode;
import com.example.library.common.UserContext;
import com.example.library.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;

/**
 * JWT 登录拦截器：校验请求头 Authorization 中的 token，并完成接口级角色授权。
 *
 * <p>职责分两层：</p>
 * <ol>
 *   <li><b>认证（Authentication）</b>：token 是否存在、是否有效、是否过期。</li>
 *   <li><b>授权（Authorization）</b>：读取目标方法/类上的 {@link RequireRole}，
 *       判断当前用户角色是否在允许列表内。</li>
 * </ol>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行跨域预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 认证：解析并校验 token
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
            token = token.substring(BEARER_PREFIX.length());
        }
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            UserContext.set(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", String.class));
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 2. 授权：校验接口声明的角色要求
        checkRole(handler);
        return true;
    }

    /**
     * 读取处理器方法（找不到时回退到所在类）上的 {@link RequireRole} 并校验角色。
     * <p>未标注该注解的接口视为"登录即可访问"。</p>
     */
    private void checkRole(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(
                handlerMethod.getMethod(), RequireRole.class);
        if (requireRole == null) {
            requireRole = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getBeanType(), RequireRole.class);
        }
        if (requireRole == null) {
            return;
        }

        String role = UserContext.getRole();
        if (role == null || !Arrays.asList(requireRole.value()).contains(role)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 必须清理：Tomcat 复用线程，ThreadLocal 不 remove() 会串用户数据
        UserContext.clear();
    }
}
