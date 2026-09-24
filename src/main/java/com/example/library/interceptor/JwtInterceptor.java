package com.example.library.interceptor;

import com.example.library.annotation.RequireRole;
import com.example.library.common.BusinessException;
import com.example.library.common.ResultCode;
import com.example.library.common.UserContext;
import com.example.library.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

        try {
            // 1. 认证：解析并校验 token
            String token = request.getHeader("Authorization");
            if (StringUtils.hasText(token) && token.startsWith(BEARER_PREFIX)) {
                token = token.substring(BEARER_PREFIX.length());
            }
            if (!StringUtils.hasText(token)) {
                log.warn("[AUTH] 未携带 token: {} {}", request.getMethod(), request.getRequestURI());
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }

            try {
                Claims claims = jwtUtil.parseToken(token);
                UserContext.set(
                        Long.valueOf(claims.getSubject()),
                        claims.get("username", String.class),
                        claims.get("role", String.class));
            } catch (Exception e) {
                // 只把失败原因写进日志，不返回给前端：避免攻击者据此区分
                // "签名错误 / 已过期 / 格式非法"，从而缩小爆破范围。
                log.warn("[AUTH] token 校验失败: {} {} 原因={}",
                        request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName());
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }

            // 2. 授权：校验接口声明的角色要求
            checkRole(handler);
            return true;

        } catch (RuntimeException e) {
            // 【关键】preHandle 抛异常时，Spring MVC 不会回调本拦截器的 afterCompletion。
            // 原因：只有 preHandle 成功返回过的拦截器才会被记入"已完成"名单，
            //      抛异常的这个拦截器不在名单里，因此 triggerAfterCompletion 会跳过它。
            // 而此处 UserContext 可能已经被 set（例如 token 合法但角色不足），
            // 若不清理，残留的用户身份会在 Tomcat 复用该线程时泄漏给下一个请求。
            // 所以必须在这里主动清理，再原样抛出，交给全局异常处理器。
            log.debug("[UserContext] preHandle 异常，兜底清理 userId={} uri={} 异常={}",
                    UserContext.getUserId(), request.getRequestURI(), e.getClass().getSimpleName());
            UserContext.clear();
            throw e;
        }
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
            // 角色不匹配：日志里记清"谁、想访问什么、需要什么角色"，
            // 便于排查误配权限；但响应仍只返回通用的"无权限"。
            log.warn("[AUTH] 角色校验失败: userId={} role={} 需要={} handler={}.{}",
                    UserContext.getUserId(), role, Arrays.toString(requireRole.value()),
                    handlerMethod.getBeanType().getSimpleName(), handlerMethod.getMethod().getName());
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 ThreadLocal，防止 Tomcat 复用线程时把上一个请求的用户身份泄漏给下一个请求。
        //
        // 注意边界（已实测）：本方法只在 preHandle 成功返回过的情况下才会被 Spring MVC 回调。
        // 若 preHandle 自身抛出异常（如角色不足返回 403），本方法不会被调用，
        // 因此 preHandle 里另有一处兜底 clear()，两处共同保证任何路径下都会被清理。
        log.debug("[UserContext] afterCompletion 清理 userId={} uri={}",
                UserContext.getUserId(), request.getRequestURI());
        UserContext.clear();
    }
}
