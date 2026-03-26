package com.hzy.smartdiet.controller;

import com.hzy.smartdiet.common.PageResult;
import com.hzy.smartdiet.common.Result;
import com.hzy.smartdiet.model.dto.DietRecordReqDTO;
import com.hzy.smartdiet.model.vo.DietRecordVO;
import com.hzy.smartdiet.service.DietRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 饮食记录控制器
 */
@Validated
@RestController
@RequestMapping("/v1/diet")
@RequiredArgsConstructor
@Tag(name = "饮食记录接口", description = "每日饮食打卡记录的增删改查与 AI 分析")
public class DietRecordController {

    private final DietRecordService dietRecordService;

    @PostMapping("/record")
    @Operation(summary = "新增饮食记录", description = "记录一餐饮食，默认触发 AI 自动分析营养成分")
    public Result<DietRecordVO> addRecord(@RequestBody @Valid DietRecordReqDTO dto) {
        return Result.success("记录添加成功", dietRecordService.addRecord(dto));
    }

    @GetMapping("/daily")
    @Operation(summary = "查询某天全部记录", description = "查询指定用户某天的早中晚加餐所有记录")
    public Result<List<DietRecordVO>> getDailyRecords(
            @RequestParam @Parameter(description = "用户ID") String userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "查询日期（默认今天）") LocalDate date) {
        return Result.success(dietRecordService.getDailyRecords(userId, date));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询历史记录", description = "按创建时间倒序分页查询")
    public Result<PageResult<DietRecordVO>> pageRecords(
            @RequestParam String userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(dietRecordService.pageRecords(userId, pageNum, pageSize));
    }

    @GetMapping("/range")
    @Operation(summary = "查询日期区间记录", description = "查询指定日期区间内的所有记录（用于生成报告）")
    public Result<List<DietRecordVO>> getByRange(
            @RequestParam String userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(dietRecordService.getByDateRange(userId, startDate, endDate));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询单条记录详情")
    public Result<DietRecordVO> getById(
            @PathVariable @Parameter(description = "记录ID") Long id) {
        return Result.success(dietRecordService.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新饮食记录", description = "仅允许更新餐食描述和图片")
    public Result<DietRecordVO> updateRecord(
            @PathVariable Long id,
            @RequestBody @Valid DietRecordReqDTO dto) {
        return Result.success("记录更新成功", dietRecordService.updateRecord(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除饮食记录")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        dietRecordService.deleteRecord(id);
        return Result.success("记录已删除", null);
    }

    @PostMapping("/{id}/reanalyze")
    @Operation(summary = "重新 AI 分析", description = "手动触发对指定记录重新进行 AI 营养分析")
    public Result<DietRecordVO> reanalyze(@PathVariable Long id) {
        return Result.success("重新分析完成", dietRecordService.reanalyze(id));
    }
}
