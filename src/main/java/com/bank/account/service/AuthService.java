package com.bank.account.service;

import com.bank.account.dto.LoginRequestDTO;
import com.bank.account.dto.RegisterRequestDTO;
import com.bank.account.vo.LoginVO;

/**
 * 鉴权服务
 */
public interface AuthService {

    /** 注册 */
    Long register(RegisterRequestDTO dto);

    /** 登录 */
    LoginVO login(LoginRequestDTO dto);

    /**
     * 登出:把当前 access token 加入 Redis 黑名单,使其立即失效
     *
     * 【为什么 JWT 需要黑名单】JWT 是无状态的,签发后在有效期内一直有效,
     * 服务端无法"收回"。登出/改密码/异地踢下线这类场景必须靠黑名单补齐:
     * 只存"未过期且已登出"的 token,TTL 设为 token 剩余有效期,过期自动清理。
     */
    void logout(String accessToken);

    /** 刷新令牌 */
    LoginVO refresh(String refreshToken);
}
