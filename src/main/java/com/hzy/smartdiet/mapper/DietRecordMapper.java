package com.hzy.smartdiet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzy.smartdiet.model.entity.DietRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录 Mapper
 */
@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    /**
     * 查询指定用户某天的所有餐食记录
     */
    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND record_date = #{date} AND deleted = 0 ORDER BY meal_type ASC")
    List<DietRecord> selectByUserIdAndDate(@Param("userId") String userId, @Param("date") LocalDate date);

    /**
     * 查询指定用户某个日期区间内的记录（用于生成周报/月报）
     */
    @Select("SELECT * FROM diet_record WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0 ORDER BY record_date ASC, meal_type ASC")
    List<DietRecord> selectByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 统计某用户在指定日期区间内的日均热量（用于周报）
     */
    @Select("SELECT COALESCE(AVG(daily_cal), 0) FROM (" +
            "  SELECT record_date, SUM(total_calories) AS daily_cal" +
            "  FROM diet_record" +
            "  WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate} AND deleted = 0" +
            "  GROUP BY record_date" +
            ") t")
    Double avgDailyCalories(
            @Param("userId") String userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
