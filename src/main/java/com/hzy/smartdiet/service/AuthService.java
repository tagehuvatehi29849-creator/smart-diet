package com.hzy.smartdiet.service;

import com.hzy.smartdiet.model.dto.LoginDTO;
import com.hzy.smartdiet.model.dto.RegisterDTO;
import com.hzy.smartdiet.model.vo.LoginVO;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 注册：创建账号 + 关联用户画像，自动登录返回 Token
     */
    LoginVO register(RegisterDTO dto);

    /**
     * 登录：验证用户名密码，颁发 Token
     */
    LoginVO login(LoginDTO dto);

    /**
     * 退出登录：销毁 Redis 中的 Token
     */
    void logout(String token);

    /**
     * 根据 Token 获取对应的 userId（Token 无效返回 null）
     */
    String getUserIdByToken(String token);

    /**
     * 修改密码
     *
     * @param userId      当前登录用户的 userId
     * @param oldPassword 旧密码（明文）
     * @param newPassword 新密码（明文）
     */
    void changePassword(String userId, String oldPassword, String newPassword);
}
