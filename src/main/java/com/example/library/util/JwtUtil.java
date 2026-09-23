package com.example.library.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private long expire;

    /** 缓存派生后的密钥，避免每次请求都重新计算 */
    private volatile SecretKey cachedKey;

    private SecretKey key() {
        if (cachedKey == null) {
            byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
            // HS256 要求密钥不少于 256 位（32 字节）。若配置的密钥过短，
            // Keys.hmacShaKeyFor 会抛 WeakKeyException，表现为"应用能启动、一登录就 500"。
            // 这里用 SHA-256 派生为定长 32 字节，既满足长度要求也不损失熵。
            if (raw.length < 32) {
                try {
                    raw = MessageDigest.getInstance("SHA-256").digest(raw);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("初始化 JWT 密钥失败", e);
                }
            }
            cachedKey = Keys.hmacShaKeyFor(raw);
        }
        return cachedKey;
    }

    /**
     * 生成 token
     *
     * <p>payload 中写入 userId(sub)、username、role 三项。
     * 注意：role 写入 token 后即"快照"，签发后修改用户角色不会让旧 token 立即失效——
     * 这是无状态令牌的固有取舍，生产环境可通过"角色变更时强制重新登录"或
     * "在服务端校验角色"来弥补。</p>
     */
    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expire))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
