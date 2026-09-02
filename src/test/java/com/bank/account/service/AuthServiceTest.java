package com.bank.account.service;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.config.JwtProperties;
import com.bank.account.config.JwtUtils;
import com.bank.account.dto.LoginRequestDTO;
import com.bank.account.entity.SysUser;
import com.bank.account.mapper.SysUserMapper;
import com.bank.account.service.impl.AuthServiceImpl;
import com.bank.account.vo.LoginVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 鉴权服务单元测试
 * 纯 Mockito, 不启动 Spring 容器, 快速验证核心业务逻辑
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtProperties jwtProperties;
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthServiceImpl authService;

    private SysUser testUser;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("testSecretKeyForJwtSigningPleaseChangeMeInProduction2024");
        jwtProperties.setExpireMinutes(120);
        jwtProperties.setRefreshExpireDays(7);
        jwtProperties.setHeader("Authorization");
        jwtProperties.setPrefix("Bearer ");

        jwtUtils = new JwtUtils(jwtProperties);

        // 用反射把真实 jwtUtils 注入 authService
        try {
            java.lang.reflect.Field field = AuthServiceImpl.class.getDeclaredField("jwtUtils");
            field.setAccessible(true);
            field.set(authService, jwtUtils);
            field = AuthServiceImpl.class.getDeclaredField("jwtProperties");
            field.setAccessible(true);
            field.set(authService, jwtProperties);
            field = AuthServiceImpl.class.getDeclaredField("redisTemplate");
            field.setAccessible(true);
            field.set(authService, redisTemplate);
        } catch (Exception e) {
            // ignore
        }

        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("zhangsan");
        // "123456" 的 BCrypt hash
        testUser.setPassword("$2a$10$7ec96oWfHL7nDOsRpejb7uda5WA3ALmj0rlTUSYzqN0Zd8pQEQDtW");
        testUser.setRealName("张三");
        testUser.setStatus(1);
    }

    @Test
    @DisplayName("登录成功 - 返回有效双 Token")
    void login_success() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("123456");

        LoginVO vo = authService.login(dto);

        assertNotNull(vo);
        assertEquals(1L, vo.getUserId());
        assertEquals("zhangsan", vo.getUsername());
        assertNotNull(vo.getAccessToken());
        assertNotNull(vo.getRefreshToken());
        assertEquals("Bearer", vo.getTokenType());
        assertTrue(vo.getExpiresIn() > 0);
    }

    @Test
    @DisplayName("登录失败 - 用户不存在")
    void login_userNotFound() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("not_exist");
        dto.setPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ResultCode.USER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void login_wrongPassword() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("zhangsan");
        dto.setPassword("wrong_password");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(dto));
        assertEquals(ResultCode.PASSWORD_ERROR.getCode(), ex.getCode());
    }
}
