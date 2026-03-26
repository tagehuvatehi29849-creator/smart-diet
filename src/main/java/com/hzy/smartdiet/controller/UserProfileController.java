package com.hzy.smartdiet.controller;

import com.hzy.smartdiet.common.Result;
import com.hzy.smartdiet.model.dto.UserProfileDTO;
import com.hzy.smartdiet.model.vo.UserProfileVO;
import com.hzy.smartdiet.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户健康画像控制器
 */
@Validated
@RestController
@RequestMapping("/v1/profile")
@RequiredArgsConstructor
@Tag(name = "用户画像接口", description = "用户健康信息的增删改查")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/{userId}")
    @Operation(summary = "查询用户画像", description = "根据 userId 查询健康画像（含 BMI、BMR、TDEE 等派生字段）")
    public Result<UserProfileVO> getProfile(
            @PathVariable @Parameter(description = "用户唯一标识") String userId) {
        return Result.success(userProfileService.getVoByUserId(userId));
    }

    @PostMapping
    @Operation(summary = "创建用户画像", description = "首次注册时创建健康画像，同时自动计算 BMR/TDEE")
    public Result<UserProfileVO> createProfile(@RequestBody @Valid UserProfileDTO dto) {
        return Result.success("用户画像创建成功", userProfileService.createProfile(dto));
    }

    @PutMapping
    @Operation(summary = "更新用户画像", description = "更新健康信息，自动重新计算 BMR/TDEE 并刷新缓存")
    public Result<UserProfileVO> updateProfile(@RequestBody @Valid UserProfileDTO dto) {
        return Result.success("用户画像更新成功", userProfileService.updateProfile(dto));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "删除用户画像", description = "逻辑删除，不影响历史饮食记录")
    public Result<Void> deleteProfile(
            @PathVariable @Parameter(description = "用户唯一标识") String userId) {
        userProfileService.deleteProfile(userId);
        return Result.success("用户画像已删除", null);
    }
}
