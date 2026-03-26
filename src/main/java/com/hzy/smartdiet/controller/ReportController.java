package com.hzy.smartdiet.controller;

import com.hzy.smartdiet.common.UserContext;
import com.hzy.smartdiet.tools.PdfReportTool;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 报告下载接口
 */
@Slf4j
@RestController
@RequestMapping("/v1/report")
@RequiredArgsConstructor
@Tag(name = "报告", description = "饮食报告下载")
public class ReportController {

    private final PdfReportTool pdfReportTool;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 下载指定日期区间的饮食 PDF 报告。
     * 日期参数使用 String 接收后手动解析，避免 Spring MVC @DateTimeFormat + LocalDate 的
     * 类型转换在某些版本直接返回 HTTP 400（绕过 GlobalExceptionHandler）。
     */
    @GetMapping("/download")
    @Operation(summary = "下载饮食 PDF 报告")
    public ResponseEntity<byte[]> download(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        String userId = UserContext.getUserId();
        log.info("[Report] 下载报告请求: userId={}, {} ~ {}", userId, startDate, endDate);

        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(startDate, DATE_FMT);
            end   = LocalDate.parse(endDate,   DATE_FMT);
        } catch (Exception e) {
            log.warn("[Report] 日期格式错误: startDate={}, endDate={}", startDate, endDate);
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] pdfBytes = pdfReportTool.generateReportBytes(userId, start, end);

            String fileName = String.format("diet-report_%s_%s.pdf", start, end);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(fileName, StandardCharsets.UTF_8)
                                    .build()
                                    .toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("[Report] PDF 生成失败: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
