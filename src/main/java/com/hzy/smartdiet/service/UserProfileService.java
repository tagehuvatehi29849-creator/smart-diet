package com.hzy.smartdiet.service;

import com.hzy.smartdiet.model.dto.UserProfileDTO;
import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.model.vo.UserProfileVO;

/**
 * 用户画像服务接口
 */
public interface UserProfileService {

    /**
     * 根据 userId 查询用户画像（优先走缓存）
     */
    UserProfile getByUserId(String userId);

    /**
     * 根据 userId 查询用户画像 VO（含 BMI 计算等派生字段）
     */
    UserProfileVO getVoByUserId(String userId);

    /**
     * 新建用户画像（同时自动计算 BMR / TDEE）
     */
    UserProfileVO createProfile(UserProfileDTO dto);

    /**
     * 更新用户画像（自动重算 BMR / TDEE，并刷新 Redis 缓存）
     */
    UserProfileVO updateProfile(UserProfileDTO dto);

    /**
     * 更新 AI 归纳的长期记忆摘要
     */
    void updateAiSummary(String userId, String summary);

    /**
     * 删除用户画像（逻辑删除）
     */
    void deleteProfile(String userId);
}
