package com.bank.account.config;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.common.UserContext;
import com.bank.account.entity.SysUser;
import com.bank.account.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台管理鉴权拦截器
 *
 * 【设计说明】
 * 前端隐藏入口不算权限控制,真正的边界在服务端。
 * 本拦截器挂在 /api/admin/** 上,先由 JwtInterceptor 解析出登录用户,
 * 再查库校验其 role 是否为 ADMIN —— 非管理员直接 403。
 *
 * 这样即便有人直接构造请求访问 /api/admin/users,也拿不到任何数据。
 */
@Component
@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final SysUserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "该账号不是管理员,无权访问后台管理接口");
        }
        return true;
    }
}