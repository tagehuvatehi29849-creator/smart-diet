package com.hzy.smartdiet.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.hzy.smartdiet.common.ErrorCode;
import com.hzy.smartdiet.constant.RedisKeyConstants;
import com.hzy.smartdiet.exception.BusinessException;
import com.hzy.smartdiet.mapper.UserProfileMapper;
import com.hzy.smartdiet.model.dto.UserProfileDTO;
import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.model.enums.ActivityLevelEnum;
import com.hzy.smartdiet.model.vo.UserProfileVO;
import com.hzy.smartdiet.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * 用户画像服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final long PROFILE_CACHE_TTL = 3600L; // 1 小时

    @Override
    public UserProfile getByUserId(String userId) {
        // 先查缓存
        String cacheKey = RedisKeyConstants.userProfileKey(userId);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof UserProfile profile) {
            return profile;
        }
        // 缓存未命中，查 DB
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile != null) {
            redisTemplate.opsForValue().set(cacheKey, profile, PROFILE_CACHE_TTL, TimeUnit.SECONDS);
        }
        return profile;
    }

    @Override
    public UserProfileVO getVoByUserId(String userId) {
        UserProfile profile = getByUserId(userId);
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }
        return UserProfileVO.from(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO createProfile(UserProfileDTO dto) {
        if (userProfileMapper.selectByUserId(dto.getUserId()) != null) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "该用户已存在画像，请使用更新接口");
        }
        UserProfile profile = dtoToEntity(dto);
        calcAndSetBmrTdee(profile);
        userProfileMapper.insert(profile);
        evictCache(dto.getUserId());
        return UserProfileVO.from(profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(UserProfileDTO dto) {
        UserProfile existing = userProfileMapper.selectByUserId(dto.getUserId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }
        // 将 DTO 中非空字段覆盖到 existing
        mergeDto(existing, dto);
        calcAndSetBmrTdee(existing);
        userProfileMapper.updateById(existing);
        evictCache(dto.getUserId());
        return UserProfileVO.from(existing);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAiSummary(String userId, String summary) {
        userProfileMapper.update(null,
                new LambdaUpdateWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .set(UserProfile::getAiProfileSummary, summary));
        evictCache(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProfile(String userId) {
        UserProfile profile = userProfileMapper.selectByUserId(userId);
        if (profile == null) {
            throw new BusinessException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }
        userProfileMapper.deleteById(profile.getId());
        evictCache(userId);
    }

    // ---- 私有方法 ----

    /**
     * Mifflin-St Jeor 公式计算 BMR，再乘以活动系数得到 TDEE
     * <ul>
     *   <li>男性：10×体重(kg) + 6.25×身高(cm) - 5×年龄 + 5</li>
     *   <li>女性：10×体重(kg) + 6.25×身高(cm) - 5×年龄 - 161</li>
     * </ul>
     */
    private void calcAndSetBmrTdee(UserProfile profile) {
        if (profile.getWeightKg() == null || profile.getHeightCm() == null || profile.getAge() == null) {
            return;
        }
        double weight = profile.getWeightKg().doubleValue();
        double height = profile.getHeightCm().doubleValue();
        int age = profile.getAge();
        int genderAdj = (profile.getGender() != null && profile.getGender() == 2) ? -161 : 5;

        double bmrVal = 10 * weight + 6.25 * height - 5.0 * age + genderAdj;
        profile.setBmr(BigDecimal.valueOf(bmrVal).setScale(2, RoundingMode.HALF_UP));

        int level = profile.getActivityLevel() != null ? profile.getActivityLevel() : 1;
        double factor = ActivityLevelEnum.fromCode(level).getFactor();
        profile.setTdee(BigDecimal.valueOf(bmrVal * factor).setScale(2, RoundingMode.HALF_UP));
    }

    private UserProfile dtoToEntity(UserProfileDTO dto) {
        UserProfile p = new UserProfile();
        p.setUserId(dto.getUserId());
        mergeDto(p, dto);
        return p;
    }

    private void mergeDto(UserProfile p, UserProfileDTO dto) {
        if (dto.getNickname() != null)           p.setNickname(dto.getNickname());
        if (dto.getAvatarUrl() != null)          p.setAvatarUrl(dto.getAvatarUrl());
        if (dto.getGender() != null)             p.setGender(dto.getGender());
        if (dto.getAge() != null)                p.setAge(dto.getAge());
        if (dto.getHeightCm() != null)           p.setHeightCm(dto.getHeightCm());
        if (dto.getWeightKg() != null)           p.setWeightKg(dto.getWeightKg());
        if (dto.getTargetWeightKg() != null)     p.setTargetWeightKg(dto.getTargetWeightKg());
        if (dto.getActivityLevel() != null)      p.setActivityLevel(dto.getActivityLevel());
        if (dto.getHealthGoals() != null)        p.setHealthGoals(dto.getHealthGoals());
        if (dto.getDietaryPreferences() != null) p.setDietaryPreferences(dto.getDietaryPreferences());
        if (dto.getFoodAllergies() != null)      p.setFoodAllergies(dto.getFoodAllergies());
        if (dto.getHealthConditions() != null)   p.setHealthConditions(dto.getHealthConditions());
    }

    private void evictCache(String userId) {
        redisTemplate.delete(RedisKeyConstants.userProfileKey(userId));
    }
}
