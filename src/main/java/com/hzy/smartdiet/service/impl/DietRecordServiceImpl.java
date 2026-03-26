package com.hzy.smartdiet.service.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hzy.smartdiet.common.ErrorCode;
import com.hzy.smartdiet.common.PageResult;
import com.hzy.smartdiet.common.UserContext;
import com.hzy.smartdiet.exception.BusinessException;
import com.hzy.smartdiet.mapper.DietRecordMapper;
import com.hzy.smartdiet.model.dto.DietRecordReqDTO;
import com.hzy.smartdiet.model.entity.DietRecord;
import com.hzy.smartdiet.model.vo.DietRecordVO;
import com.hzy.smartdiet.service.AgentService;
import com.hzy.smartdiet.service.DietRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 饮食记录服务实现
 */
@Slf4j
@Service
@DS("mysql") // 【关键】：强制走业务 MySQL 数据源
public class DietRecordServiceImpl implements DietRecordService {

    private final DietRecordMapper dietRecordMapper;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    // 【核心修复】：去掉类上的 @RequiredArgsConstructor，手动编写构造函数，
    // 并为 agentService 参数打上 @Lazy 注解，打破循环依赖。
    public DietRecordServiceImpl(DietRecordMapper dietRecordMapper,
                                 @Lazy AgentService agentService,
                                 ObjectMapper objectMapper) {
        this.dietRecordMapper = dietRecordMapper;
        this.agentService = agentService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DietRecordVO addRecord(DietRecordReqDTO dto) {
        DietRecord record = new DietRecord();

        // ================== 核心安全改造 ==================
        // 不再相信前端传的 userId，直接从 JWT 拦截器存好的上下文获取
        String currentUserId = UserContext.getUserId();
        if (currentUserId == null) {
            throw new BusinessException(401, "获取当前用户信息失败，请重新登录");
        }
        record.setUserId(currentUserId);
        // ===============================================

        record.setRecordDate(dto.getRecordDate() != null ? dto.getRecordDate() : LocalDate.now());
        record.setMealType(dto.getMealType());
        record.setMealDescription(dto.getMealDescription());
        record.setImageUrl(dto.getImageUrl());

        dietRecordMapper.insert(record);

        if (dto.isAnalyzeNow()) {
            record = performAiAnalysis(record);
            dietRecordMapper.updateById(record);
        }
        return DietRecordVO.from(record);
    }

    @Override
    public List<DietRecordVO> getDailyRecords(String userId, LocalDate date) {
        // HTTP 请求线程用 UserContext（安全）；AI 工具线程 UserContext 为空时降级用参数
        String currentUserId = UserContext.getUserId();
        String queryUserId   = (currentUserId != null && !currentUserId.isBlank()) ? currentUserId : userId;
        LocalDate queryDate  = date != null ? date : LocalDate.now();
        return dietRecordMapper.selectByUserIdAndDate(queryUserId, queryDate)
                .stream().map(DietRecordVO::from).collect(Collectors.toList());
    }

    @Override
    public PageResult<DietRecordVO> pageRecords(String userId, int pageNum, int pageSize) {
        // ================== 核心安全改造 ==================
        String currentUserId = UserContext.getUserId();
        Page<DietRecord> page = new Page<>(pageNum, pageSize);
        Page<DietRecord> result = dietRecordMapper.selectPage(page,
                new LambdaQueryWrapper<DietRecord>()
                        // 强制过滤当前用户的记录
                        .eq(DietRecord::getUserId, currentUserId)
                        .orderByDesc(DietRecord::getRecordDate)
                        .orderByDesc(DietRecord::getCreatedAt));
        List<DietRecordVO> voList = result.getRecords().stream()
                .map(DietRecordVO::from).collect(Collectors.toList());
        return PageResult.of(pageNum, pageSize, result.getTotal(), voList);
    }

    @Override
    public DietRecordVO getById(Long id) {
        DietRecord record = dietRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.DIET_RECORD_NOT_FOUND);
        }
        // ================== 核心安全改造 ==================
        // 越权校验：只能查自己的记录
        if (!record.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权查看他人的饮食记录");
        }
        return DietRecordVO.from(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DietRecordVO updateRecord(Long id, DietRecordReqDTO dto) {
        DietRecord record = dietRecordMapper.selectById(id);
        if (record == null) throw new BusinessException(ErrorCode.DIET_RECORD_NOT_FOUND);

        // ================== 核心安全改造 ==================
        // 越权校验：只能改自己的记录
        if (!record.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权修改他人的饮食记录");
        }

        if (dto.getMealDescription() != null) record.setMealDescription(dto.getMealDescription());
        if (dto.getImageUrl() != null)        record.setImageUrl(dto.getImageUrl());
        if (dto.getMealType() != null)        record.setMealType(dto.getMealType());

        dietRecordMapper.updateById(record);
        return DietRecordVO.from(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long id) {
        DietRecord record = dietRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.DIET_RECORD_NOT_FOUND);
        }
        // ================== 核心安全改造 ==================
        // 越权校验：只能删自己的记录
        if (!record.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权删除他人的饮食记录");
        }

        dietRecordMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DietRecordVO reanalyze(Long id) {
        DietRecord record = dietRecordMapper.selectById(id);
        if (record == null) throw new BusinessException(ErrorCode.DIET_RECORD_NOT_FOUND);

        // 越权校验
        if (!record.getUserId().equals(UserContext.getUserId())) {
            throw new BusinessException(403, "无权操作他人的饮食记录");
        }

        record = performAiAnalysis(record);
        dietRecordMapper.updateById(record);
        return DietRecordVO.from(record);
    }

    @Override
    public List<DietRecordVO> getByDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        // HTTP 请求线程用 UserContext（安全）；AI 工具线程 UserContext 为空时降级用参数
        String currentUserId = UserContext.getUserId();
        String queryUserId   = (currentUserId != null && !currentUserId.isBlank()) ? currentUserId : userId;
        return dietRecordMapper.selectByUserIdAndDateRange(queryUserId, startDate, endDate)
                .stream().map(DietRecordVO::from).collect(Collectors.toList());
    }

    // ---- 私有方法 ----

    /**
     * 调用 AI 分析饮食记录，将解析结果回填到实体
     */
    private DietRecord performAiAnalysis(DietRecord record) {
        try {
            String analysisJson = agentService.analyzeDietRecord(
                    record.getUserId(),
                    record.getMealDescription(),
                    record.getMealType() != null ? record.getMealType() : 0,
                    record.getImageUrl()
            );
            record.setAiAnalysisResult(analysisJson);
            parseAndFillNutrition(record, analysisJson);
        } catch (Exception e) {
            log.error("[DietRecord] AI 分析失败, recordId={}", record.getId(), e);
            record.setAiSuggestion("AI 分析暂时不可用，请稍后重试。");
        }
        return record;
    }

    /**
     * 解析 AI 返回的 JSON，填充营养字段
     */
    @SuppressWarnings("unchecked")
    private void parseAndFillNutrition(DietRecord record, String json) {
        if (json == null || json.isBlank()) return;
        try {
            // 提取 JSON 块（AI 有时会输出 markdown 代码块）
            String cleanJson = extractJson(json);
            Map<String, Object> map = objectMapper.readValue(cleanJson, new TypeReference<>() {});

            // 统一将 foodItems 转为 List<String>：
            // AI 可能返回字符串数组或对象数组，两种情况都处理
            Object rawFoodItems = map.get("foodItems");
            if (rawFoodItems instanceof List<?> rawList) {
                List<String> items = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof Map<?, ?>) {
                        // 对象格式：{"name":"xx","amount":"xx","calories":xx} → 拼成可读字符串
                        @SuppressWarnings("unchecked")
                        Map<Object, Object> obj = (Map<Object, Object>) item;
                        String name    = String.valueOf(obj.getOrDefault("name", ""));
                        String amount  = String.valueOf(obj.getOrDefault("amount", ""));
                        Object cal     = obj.get("calories");
                        String display = name;
                        if (!amount.isBlank()) display += "（" + amount + "）";
                        if (cal != null)        display += " " + cal + "kcal";
                        items.add(display.strip());
                    } else if (item != null) {
                        // 字符串格式：直接使用
                        items.add(item.toString());
                    }
                }
                record.setFoodItems(items);
            }
            record.setTotalCalories(toBigDecimal(map.get("totalCalories")));
            record.setProteinG(toBigDecimal(map.get("protein")));
            record.setCarbohydratesG(toBigDecimal(map.get("carbohydrates")));
            record.setFatG(toBigDecimal(map.get("fat")));
            record.setFiberG(toBigDecimal(map.get("fiber")));
            record.setAiSuggestion((String) map.get("suggestion"));

            Object score = map.get("healthScore");
            if (score != null) record.setHealthScore(Integer.parseInt(score.toString()));

            // 热量超标预警（简单规则：单餐 > 1200 kcal 标为异常）
            if (record.getTotalCalories() != null
                    && record.getTotalCalories().compareTo(new BigDecimal("1200")) > 0) {
                record.setIsAbnormal(1);
                record.setAbnormalReason("本餐热量超过 1200 kcal，建议适当控制摄入量");
            }
        } catch (Exception e) {
            log.warn("[DietRecord] 解析 AI 营养数据失败，原始回复: {}", json, e);
            record.setAiSuggestion(json);
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}