package com.hzy.smartdiet.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzy.smartdiet.model.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户画像 Mapper
 */
@Mapper
@DS("mysql")
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    /** 根据业务 userId 查询（已过滤逻辑删除） */
    @Select("SELECT * FROM user_profile WHERE user_id = #{userId} AND deleted = 0 LIMIT 1")
    UserProfile selectByUserId(@Param("userId") String userId);

    /** 根据登录用户名查询（已过滤逻辑删除） */
    @Select("SELECT * FROM user_profile WHERE username = #{username} AND deleted = 0 LIMIT 1")
    UserProfile selectByUsername(@Param("username") String username);

    /** 检查用户名是否已存在 */
    @Select("SELECT COUNT(*) FROM user_profile WHERE username = #{username} AND deleted = 0")
    int countByUsername(@Param("username") String username);

    /** 更新最后登录时间 */
    @Update("UPDATE user_profile SET last_login_at = NOW() WHERE user_id = #{userId}")
    void updateLastLoginAt(@Param("userId") String userId);
}
