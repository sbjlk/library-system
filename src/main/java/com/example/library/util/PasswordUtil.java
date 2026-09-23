package com.example.library.util;

import com.example.library.common.BusinessException;
import org.springframework.util.DigestUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码加密工具：PBKDF2-HMAC-SHA256（加盐 + 多轮迭代）。
 *
 * <p>存储格式（自描述，便于将来平滑升级算法）：</p>
 * <pre>pbkdf2$迭代次数$盐(Base64)$哈希(Base64)</pre>
 *
 * <p>为什么不用 MD5：MD5 是"快速摘要"而不是"密码哈希"——无盐、计算速度极快（GPU 每秒可算数十亿次），
 * 且同一密码的摘要固定，彩虹表可直接反查。密码存储必须使用"故意慢"的算法并加随机盐。</p>
 *
 * <p>为什么用 PBKDF2 而不是 BCrypt：PBKDF2 由 JDK 原生提供（Java 8 起支持），
 * 无需引入额外依赖；生产环境若已有 Spring Security，可直接替换为 {@code BCryptPasswordEncoder}
 * （BCrypt 对 GPU 更不友好，且盐值内置于哈希串）。</p>
 *
 * <p>向后兼容：仍可校验历史遗留的 32 位 MD5 密码，
 * {@link #needsUpgrade(String)} 会识别旧格式，登录成功后由业务层自动重写为新格式。</p>
 */
public class PasswordUtil {

    /** 当前算法版本标识 */
    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final char SEPARATOR = '$';

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 加密明文密码。
     */
    public String encode(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return PBKDF2_PREFIX + SEPARATOR + ITERATIONS + SEPARATOR
                + encoder.encodeToString(salt) + SEPARATOR
                + encoder.encodeToString(hash);
    }

    /**
     * 校验密码。
     *
     * <p>新旧两种格式都支持：新格式走 PBKDF2，旧格式（32 位 MD5）走兼容分支。
     * 比较时统一使用 {@link MessageDigest#isEqual} —— 恒定时间比较，避免通过响应耗时推测密码。</p>
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        // 兼容历史 MD5 数据
        if (!storedPassword.startsWith(PBKDF2_PREFIX + SEPARATOR)) {
            return md5Matches(rawPassword, storedPassword);
        }

        String[] parts = storedPassword.split("\\" + SEPARATOR);
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] salt = decoder.decode(parts[2]);
            byte[] expected = decoder.decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断该密码是否需要用当前算法重新加密（例如历史 MD5 密码首次登录成功后）。
     */
    public boolean needsUpgrade(String storedPassword) {
        return storedPassword == null || !storedPassword.startsWith(PBKDF2_PREFIX + SEPARATOR);
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new BusinessException("密码加密失败");
        } finally {
            spec.clearPassword();
        }
    }

    private boolean md5Matches(String rawPassword, String storedPassword) {
        String md5 = DigestUtils.md5DigestAsHex(rawPassword.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(
                md5.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
    }
}
