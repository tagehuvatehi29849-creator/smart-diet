-- ============================================================
-- 智能饮食管理助手 - 数据库建表脚本
-- 数据库：smart_diet (MySQL 8.0+)
-- 字符集：utf8mb4 / utf8mb4_unicode_ci
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_diet
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_diet;

-- ============================================================
-- 表 1：user_profile（用户账号 + 健康画像合一）
-- username / password_hash 负责登录，其余字段记录健康信息。
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`             VARCHAR(64)  NOT NULL                COMMENT '用户唯一业务主键（UUID，各模块关联键）',

    -- 登录凭证
    `username`            VARCHAR(50)  NOT NULL                COMMENT '登录用户名（唯一，3~30位字母/数字/下划线）',
    `password`            VARCHAR(255) NOT NULL                COMMENT 'BCrypt 加密后的密码，永不明文存储',
    `status`              TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '账号状态：1=正常 0=禁用',
    `last_login_at`       DATETIME     DEFAULT NULL            COMMENT '上次登录时间',

    -- 基础健康信息
    `nickname`            VARCHAR(64)  DEFAULT NULL            COMMENT '用户昵称（展示用）',
    `avatar_url`          VARCHAR(256) DEFAULT NULL            COMMENT '头像 URL',
    `gender`              TINYINT(1)   DEFAULT 0               COMMENT '性别：0=未知 1=男 2=女',
    `age`                 INT          DEFAULT NULL            COMMENT '年龄（岁）',
    `height_cm`           DECIMAL(5,1) DEFAULT NULL            COMMENT '身高（cm）',
    `weight_kg`           DECIMAL(5,2) DEFAULT NULL            COMMENT '体重（kg）',
    `target_weight_kg`    DECIMAL(5,2) DEFAULT NULL            COMMENT '目标体重（kg）',
    `bmr`                 DECIMAL(8,2) DEFAULT NULL            COMMENT '基础代谢率 BMR（kcal/天），由系统计算',
    `tdee`                DECIMAL(8,2) DEFAULT NULL            COMMENT '每日总能量消耗 TDEE（kcal/天）',
    `activity_level`      TINYINT(1)   DEFAULT 1               COMMENT '活动水平：1=久坐 2=轻度 3=中度 4=高度 5=极高',

    -- 偏好与目标（JSON）
    `health_goals`        JSON         DEFAULT NULL            COMMENT '健康目标 JSON 数组，如 ["减重","控糖","增肌"]',
    `dietary_preferences` JSON         DEFAULT NULL            COMMENT '饮食偏好 JSON',
    `food_allergies`      JSON         DEFAULT NULL            COMMENT '食物过敏/忌口 JSON 数组',
    `health_conditions`   JSON         DEFAULT NULL            COMMENT '健康状况 JSON 数组，如 ["高血压","糖尿病"]',

    -- AI 长期记忆
    `ai_profile_summary`  TEXT         DEFAULT NULL            COMMENT 'AI 归纳的用户健康画像摘要（长期记忆）',

    `deleted`             TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=正常 1=已删除',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP     COMMENT '创建时间',
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id`  (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='用户账号与健康画像表（登录凭证 + AI 长期记忆）';


-- ============================================================
-- 表 2：diet_record（每日饮食打卡记录）
-- ============================================================
CREATE TABLE IF NOT EXISTS `diet_record` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`             VARCHAR(64)  NOT NULL                COMMENT '用户唯一标识（关联 user_profile.user_id）',
    `record_date`         DATE         NOT NULL                COMMENT '饮食记录日期',
    `meal_type`           TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '餐次类型：0=早餐 1=午餐 2=晚餐 3=加餐 4=夜宵',
    `meal_description`    TEXT         DEFAULT NULL            COMMENT '用户输入的餐食描述（原始文本）',
    `food_items`          JSON         DEFAULT NULL            COMMENT 'AI 解析后的食物列表 JSON',
    `image_url`           MEDIUMTEXT   DEFAULT NULL            COMMENT '餐食图片 URL 或 Base64 数据',
    `total_calories`      DECIMAL(8,2) DEFAULT NULL            COMMENT '总热量（kcal）',
    `protein_g`           DECIMAL(6,2) DEFAULT NULL            COMMENT '蛋白质（g）',
    `carbohydrates_g`     DECIMAL(6,2) DEFAULT NULL            COMMENT '碳水化合物（g）',
    `fat_g`               DECIMAL(6,2) DEFAULT NULL            COMMENT '脂肪（g）',
    `fiber_g`             DECIMAL(6,2) DEFAULT NULL            COMMENT '膳食纤维（g）',
    `sodium_mg`           DECIMAL(8,2) DEFAULT NULL            COMMENT '钠（mg）',
    `ai_analysis_result`  TEXT         DEFAULT NULL            COMMENT 'AI 完整分析结果 JSON',
    `ai_suggestion`       TEXT         DEFAULT NULL            COMMENT 'AI 健康建议（自然语言）',
    `health_score`        TINYINT      DEFAULT NULL            COMMENT '健康评分（0-100）',
    `is_abnormal`         TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '是否存在饮食异常：0=正常 1=有异常',
    `abnormal_reason`     VARCHAR(512) DEFAULT NULL            COMMENT '异常原因描述',
    `deleted`             TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=正常 1=已删除',
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `record_date`),
    KEY `idx_user_meal` (`user_id`, `meal_type`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='每日饮食打卡记录表';


-- ============================================================
-- 表 3：diet_weekly_summary（周报统计缓存，可选）
-- ============================================================
CREATE TABLE IF NOT EXISTS `diet_weekly_summary` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
    `user_id`          VARCHAR(64)  NOT NULL                COMMENT '用户唯一标识',
    `week_start_date`  DATE         NOT NULL                COMMENT '本周开始日期（周一）',
    `week_end_date`    DATE         NOT NULL                COMMENT '本周结束日期（周日）',
    `avg_calories`     DECIMAL(8,2) DEFAULT NULL            COMMENT '日均摄入热量（kcal）',
    `avg_protein_g`    DECIMAL(6,2) DEFAULT NULL            COMMENT '日均蛋白质（g）',
    `avg_carbs_g`      DECIMAL(6,2) DEFAULT NULL            COMMENT '日均碳水（g）',
    `avg_fat_g`        DECIMAL(6,2) DEFAULT NULL            COMMENT '日均脂肪（g）',
    `record_days`      TINYINT      DEFAULT 0               COMMENT '本周打卡天数',
    `avg_health_score` DECIMAL(4,1) DEFAULT NULL            COMMENT '平均健康评分',
    `ai_week_summary`  TEXT         DEFAULT NULL            COMMENT 'AI 生成的周报总结',
    `deleted`          TINYINT(1)   NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_week` (`user_id`, `week_start_date`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='每周饮食汇总统计表（供PDF报告使用）';


-- ============================================================
-- 示例初始化数据（可选）
-- 密码 "123456" 对应的 BCrypt 密文
-- ============================================================
INSERT INTO `user_profile` (
    `user_id`, `username`, `password`, `status`,
    `nickname`, `gender`, `age`, `height_cm`, `weight_kg`,
    `activity_level`, `health_goals`, `dietary_preferences`
) VALUES (
    'demo_user_001',
    'demo_admin',
    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8MWTCEyeHCvq',
    1,
    '演示用户', 1, 28, 175.0, 70.00,
    2,
    JSON_ARRAY('减重', '增强体质'),
    JSON_OBJECT('cuisine', JSON_ARRAY('粤菜', '日料'), 'diet_type', '普通')
) ON DUPLICATE KEY UPDATE `nickname` = VALUES(`nickname`);

-- 清理旧版遗留表（幂等，可安全重复执行）
DROP TABLE IF EXISTS `user_account`;