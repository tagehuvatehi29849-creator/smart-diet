package com.hzy.smartdiet.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 登录 / 注册成功响应 VO
 */
@Data
@Builder
@Schema(description = "登录响应体")
public class LoginVO {

    @Schema(description = "访问令牌（前端每次请求须放入 Header: Authorization: Bearer {token}）")
    private String token;

    @Schema(description = "令牌过期时间戳（毫秒）")
    private long expireAt;

    @Schema(description = "业务 user_id（与 user_profile.user_id 对应）")
    private String userId;

    @Schema(description = "用户名（登录凭证）")
    private String username;

    @Schema(description = "昵称（展示用）")
    private String nickname;

    @Schema(description = "是否已创建健康画像")
    private boolean hasProfile;
}
