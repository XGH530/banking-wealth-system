package com.bank.account.service.impl;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.config.JwtProperties;
import com.bank.account.config.JwtUtils;
import com.bank.account.dto.LoginRequestDTO;
import com.bank.account.dto.RegisterRequestDTO;
import com.bank.account.entity.SysUser;
import com.bank.account.mapper.SysUserMapper;
import com.bank.account.service.AuthService;
import com.bank.account.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

/**
 * 鉴权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    /** Redis 中存储旧 token 的黑名单前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "bank:jwt:blacklist:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequestDTO dto) {
        // 用户名唯一性校验
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);
        }

        // 身份证唯一性校验
        Long idCardExists = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getIdCard, dto.getIdCard()));
        if (idCardExists != null && idCardExists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "身份证号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        user.setRealName(dto.getRealName());
        user.setIdCard(dto.getIdCard());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        log.info("用户注册成功: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginRequestDTO dto) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        // 生成 token
        String access  = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        String refresh = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        return LoginVO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpireSeconds())
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .build();
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtils.parse(refreshToken);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        if (!"REFRESH".equals(claims.get("type"))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "非刷新令牌");
        }

        // 黑名单校验
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + refreshToken;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "令牌已失效");
        }

        Long userId = claims.get("uid") instanceof Integer
                ? ((Integer) claims.get("uid")).longValue()
                : (Long) claims.get("uid");
        String username = (String) claims.get("username");

        SysUser user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 旧刷新令牌加入黑名单
        redisTemplate.opsForValue().set(blacklistKey, "1",
                Duration.ofDays(jwtProperties.getRefreshExpireDays()));

        String access  = jwtUtils.generateAccessToken(userId, username);
        String newRefresh = jwtUtils.generateRefreshToken(userId, username);

        return LoginVO.builder()
                .accessToken(access)
                .refreshToken(newRefresh)
                .tokenType("Bearer")
                .expiresIn(jwtUtils.getExpireSeconds())
                .userId(userId)
                .username(username)
                .realName(user.getRealName())
                .build();
    }
}
