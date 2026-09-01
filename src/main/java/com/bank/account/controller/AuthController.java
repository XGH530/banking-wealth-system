package com.bank.account.controller;

import com.bank.account.common.Result;
import com.bank.account.dto.LoginRequestDTO;
import com.bank.account.dto.RegisterRequestDTO;
import com.bank.account.service.AuthService;
import com.bank.account.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 鉴权接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "鉴权接口")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "注册")
    public Result<Long> register(@Valid @RequestBody RegisterRequestDTO dto) {
        Long userId = authService.register(dto);
        return Result.created(userId);
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    public Result<LoginVO> refresh(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return Result.fail(com.bank.account.common.ResultCode.UNAUTHORIZED);
        }
        String refreshToken = header.substring(7).trim();
        return Result.success(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录(客户端清除 token 即可)")
    public Result<Void> logout() {
        // 无状态 JWT,服务端不存储;如有需要可将 token 加入黑名单
        return Result.success();
    }
}
