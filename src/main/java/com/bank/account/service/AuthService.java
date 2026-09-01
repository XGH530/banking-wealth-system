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

    /** 刷新令牌 */
    LoginVO refresh(String refreshToken);
}
