package com.bank.account.config;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.common.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权拦截器
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final JwtProperties props;
    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // OPTIONS 预检放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String auth = request.getHeader(props.getHeader());
        if (!StringUtils.hasText(auth) || !auth.startsWith(props.getPrefix())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        String token = auth.substring(props.getPrefix().length()).trim();
        Claims claims;
        try {
            claims = jwtUtils.parse(token);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 校验是否为 access token
        Object type = claims.get("type");
        if (!"ACCESS".equals(type)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请使用访问令牌");
        }

        // 黑名单校验:登出后的 token 会被写入 Redis,在这里直接拒绝
        // (JWT 无状态,没有这一步"登出立即失效"就做不到)
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(
                    com.bank.account.common.BankConstants.CACHE_TOKEN_BLACKLIST_PREFIX + token))) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已失效,请重新登录");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 抖动不应该让所有请求都登录失败:记录告警后放行(可用性优先)
            org.slf4j.LoggerFactory.getLogger(JwtInterceptor.class)
                    .warn("校验 Token 黑名单失败,已降级放行: {}", e.getMessage());
        }

        // 写入上下文
        Object uid = claims.get("uid");
        Long userId = uid instanceof Integer ? ((Integer) uid).longValue() : (Long) uid;
        String username = (String) claims.get("username");
        UserContext.set(new UserContext.LoginUser(userId, username, null));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
