package com.bank.account.config;

import com.bank.account.common.ResultCode;
import com.bank.account.common.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtils {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtUtils(JwtProperties props) {
        this.props = props;
        byte[] keyBytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        // HS512 要求至少 64 字节
        if (keyBytes.length < 64) {
            byte[] padded = new byte[64];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问 token
     */
    public String generateAccessToken(Long userId, String username) {
        long expireMs = props.getExpireMinutes() * 60 * 1000;
        return buildToken(userId, username, "ACCESS", expireMs);
    }

    /**
     * 生成刷新 token
     */
    public String generateRefreshToken(Long userId, String username) {
        long expireMs = props.getRefreshExpireDays() * 24 * 60 * 60 * 1000L;
        return buildToken(userId, username, "REFRESH", expireMs);
    }

    private String buildToken(Long userId, String username, String type, long expireMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireMs);
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", userId);
        claims.put("username", username);
        claims.put("type", type);
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("解析 token 失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "token 无效或已过期");
        }
    }

    /**
     * 校验 token 是否有效
     */
    public boolean verify(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 token 获取用户 ID
     */
    public Long getUserId(String token) {
        Object uid = parse(token).get("uid");
        if (uid instanceof Integer) {
            return ((Integer) uid).longValue();
        }
        return (Long) uid;
    }

    /**
     * 从 token 获取用户名
     */
    public String getUsername(String token) {
        return (String) parse(token).get("username");
    }

    /**
     * 获取 token 过期秒数
     */
    public long getExpireSeconds() {
        return props.getExpireMinutes() * 60;
    }
}
