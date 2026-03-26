package com.hzy.smartdiet.service;

import com.hzy.smartdiet.common.PageResult;
import com.hzy.smartdiet.model.dto.DietRecordReqDTO;
import com.hzy.smartdiet.model.vo.DietRecordVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录服务接口
 */
public interface DietRecordService {

    /**
     * 新增饮食记录（若 analyzeNow=true，同步触发 AI 分析）
     */
    DietRecordVO addRecord(DietRecordReqDTO dto);

    /**
     * 查询指定用户某天的所有餐食记录
     */
    List<DietRecordVO> getDailyRecords(String userId, LocalDate date);

    /**
     * 分页查询用户的历史饮食记录
     */
    PageResult<DietRecordVO> pageRecords(String userId, int pageNum, int pageSize);

    /**
     * 根据 ID 查询单条记录详情
     */
    DietRecordVO getById(Long id);

    /**
     * 更新饮食记录（仅更新描述和图片，营养数据不可手动修改）
     */
    DietRecordVO updateRecord(Long id, DietRecordReqDTO dto);

    /**
     * 删除饮食记录（逻辑删除）
     */
    void deleteRecord(Long id);

    /**
     * 手动触发 AI 重新分析指定记录
     */
    DietRecordVO reanalyze(Long id);

    /**
     * 查询指定日期区间的记录（用于生成报告）
     */
    List<DietRecordVO> getByDateRange(String userId, LocalDate startDate, LocalDate endDate);
}
