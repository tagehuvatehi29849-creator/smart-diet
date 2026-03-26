package com.hzy.smartdiet.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzy.smartdiet.common.ErrorCode;
import com.hzy.smartdiet.constant.RedisKeyConstants;
import com.hzy.smartdiet.exception.BusinessException;
import com.hzy.smartdiet.mapper.UserProfileMapper;
import com.hzy.smartdiet.model.dto.LoginDTO;
import com.hzy.smartdiet.model.dto.RegisterDTO;
import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.model.vo.LoginVO;
import com.hzy.smartdiet.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 * <p>
 * 用户数据全部存储在 {@code user_profile} 表，无独立账号表。
 * 密码使用 Hutool BCrypt 加密（自带 salt，不可逆）。
 * 会话使用随机 UUID Token 写入 Redis，过期自动失效。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserProfileMapper  userProfileMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long TOKEN_TTL_DAYS          = 7L;
    private static final long TOKEN_TTL_REMEMBER_DAYS = 30L;

    // ==================== 注册 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(RegisterDTO dto) {
        // 1. 用户名唯一性校验
        if (userProfileMapper.countByUsername(dto.getUsername()) > 0) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 2. 生成唯一业务 userId（贯穿所有模块的关联键）
        String userId = UUID.randomUUID().toString().replace("-", "");

        // 3. 构建并插入用户记录（账号信息 + 健康画像一体）
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setUsername(dto.getUsername());
        profile.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));
        profile.setStatus(1);
        profile.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        profile.setGender(dto.getGender());
        profile.setAge(dto.getAge());
        profile.setHeightCm(dto.getHeightCm());
        profile.setWeightKg(dto.getWeightKg());
        profile.setActivityLevel(dto.getActivityLevel() != null ? dto.getActivityLevel() : 2);
        profile.setHealthGoals(dto.getHealthGoals());
        userProfileMapper.insert(profile);

        log.info("[Auth] 注册成功 username={}, userId={}", dto.getUsername(), userId);

        // 4. 注册即登录，颁发 Token
        return issueToken(userId, dto.getUsername(), profile.getNickname(), false);
    }

    // ==================== 登录 ====================

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查找账号
        UserProfile profile = userProfileMapper.selectByUsername(dto.getUsername());
        if (profile == null) {
            throw new BusinessException(ErrorCode.USERNAME_NOT_FOUND);
        }
        if (profile.getStatus() != null && profile.getStatus() == 0) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 2. BCrypt 密码校验
        if (!BCrypt.checkpw(dto.getPassword(), profile.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG);
        }

        // 3. 更新最后登录时间
        userProfileMapper.updateLastLoginAt(profile.getUserId());

        log.info("[Auth] 登录成功 username={}, userId={}", dto.getUsername(), profile.getUserId());

        return issueToken(profile.getUserId(), profile.getUsername(),
                profile.getNickname(), dto.isRememberMe());
    }

    // ==================== 退出 ====================

    @Override
    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        redisTemplate.delete(RedisKeyConstants.authTokenKey(token));
        log.info("[Auth] Token 已销毁");
    }

    // ==================== Token 验证 ====================

    @Override
    @SuppressWarnings("unchecked")
    public String getUserIdByToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            Object val = redisTemplate.opsForValue().get(RedisKeyConstants.authTokenKey(token));
            if (val == null) return null;
            Map<String, String> session = (Map<String, String>) val;
            return session.get("userId");
        } catch (Exception e) {
            log.warn("[Auth] Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 修改密码 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String userId, String oldPassword, String newPassword) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .eq(UserProfile::getDeleted, 0));
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!BCrypt.checkpw(oldPassword, profile.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_WRONG, "原密码不正确");
        }
        userProfileMapper.update(null,
                new LambdaUpdateWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .set(UserProfile::getPassword,
                                BCrypt.hashpw(newPassword, BCrypt.gensalt())));
        log.info("[Auth] 密码修改成功 userId={}", userId);
    }

    // ==================== 私有方法 ====================

    /**
     * 生成 Token 并写入 Redis，返回登录响应 VO
     */
    private LoginVO issueToken(String userId, String username, String nickname, boolean rememberMe) {
        String token   = UUID.randomUUID().toString().replace("-", "");
        long   ttlDays = rememberMe ? TOKEN_TTL_REMEMBER_DAYS : TOKEN_TTL_DAYS;

        Map<String, String> session = new HashMap<>();
        session.put("userId",   userId);
        session.put("username", username);
        session.put("nickname", nickname != null ? nickname : username);
        redisTemplate.opsForValue().set(
                RedisKeyConstants.authTokenKey(token),
                session,
                ttlDays, TimeUnit.DAYS
        );

        return LoginVO.builder()
                .token(token)
                .expireAt(System.currentTimeMillis() + ttlDays * 24 * 3600 * 1000L)
                .userId(userId)
                .username(username)
                .nickname(nickname != null ? nickname : username)
                .hasProfile(true)   // user_profile 已在注册时创建，始终为 true
                .build();
    }
}
