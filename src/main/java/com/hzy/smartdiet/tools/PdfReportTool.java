package com.hzy.smartdiet.tools;

import com.hzy.smartdiet.model.entity.UserProfile;
import com.hzy.smartdiet.model.vo.DietRecordVO;
import com.hzy.smartdiet.service.DietRecordService;
import com.hzy.smartdiet.service.UserProfileService;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PDF 饮食报告生成工具（AI Tool）
 * <p>
 * AI 可在用户要求"生成报告"、"导出周报"等场景下自动调用此工具，
 * 为指定用户生成指定日期区间的饮食分析 PDF 报告，并返回文件下载路径。
 * 使用 iText 9 + font-asian 中文字体实现中文 PDF 输出。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfReportTool {

    private final DietRecordService dietRecordService;
    private final UserProfileService userProfileService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DeviceRgb THEME_COLOR = new DeviceRgb(76, 175, 80); // 主题绿色

    /**
     * 生成饮食报告 PDF（按日期区间）
     *
     * @param userId    用户唯一标识
     * @param startDate 开始日期（格式：yyyy-MM-dd）
     * @param endDate   结束日期（格式：yyyy-MM-dd）
     * @return 生成的 PDF 文件路径（或错误提示）
     */
    @Tool(description = "为用户生成指定日期区间的饮食分析 PDF 报告。当用户说'帮我生成报告'、'导出周报'、'我想看我的饮食总结'等时调用。日期格式 yyyy-MM-dd，如 2026-03-18。")
    public String generateDietReport(String userId, String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FMT);
            LocalDate end   = LocalDate.parse(endDate,   DATE_FMT);

            if (start.isAfter(end)) {
                return "❌ 开始日期不能晚于结束日期";
            }

            List<DietRecordVO> records = dietRecordService.getByDateRange(userId, start, end);

            // 预先验证 PDF 可以正常生成（少量生成开销，主要是参数检查）
            generateReportBytes(userId, start, end);

            log.info("[PDF] 报告预验证通过: userId={}, {} ~ {}, {}条记录",
                    userId, startDate, endDate, records.size());

            // 返回文本 + 特殊下载标记（前端解析此标记并渲染下载按钮）
            return "✅ 饮食报告已生成！\n" +
                    "📊 统计周期：" + start.format(DISPLAY_FMT) + " 至 " + end.format(DISPLAY_FMT) + "\n" +
                    "📝 包含记录：" + records.size() + " 条\n\n" +
                    "[DOWNLOAD:" + startDate + ":" + endDate + "]\n" +
                    "（请原样展示上一行，勿改为 %%…%%、勿用 ~ 代替 :、勿做成 Markdown 链接）";

        } catch (Exception e) {
            log.error("[PDF] 报告生成失败, userId={}", userId, e);
            return "❌ PDF 报告生成失败：" + e.getMessage();
        }
    }

    /**
     * 生成 PDF 报告字节流（供下载接口直接调用，不写临时文件）。
     */
    public byte[] generateReportBytes(String userId, LocalDate start, LocalDate end) throws Exception {
        List<DietRecordVO> records = dietRecordService.getByDateRange(userId, start, end);
        UserProfile        profile = userProfileService.getByUserId(userId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        buildPdf(baos, profile, records, start, end);
        return baos.toByteArray();
    }

    // ---- 私有 PDF 构建方法 ----

    private void buildPdf(java.io.OutputStream outputStream, UserProfile profile,
                          List<DietRecordVO> records, LocalDate start, LocalDate end) throws Exception {

        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc, PageSize.A4);
        document.setMargins(40, 40, 40, 40);

        // 加载中文字体（iText font-asian 提供 STSong-Light）
        PdfFont chineseFont;
        try {
            chineseFont = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                    PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED);
        } catch (Exception e) {
            log.warn("[PDF] 中文字体加载失败，降级使用 Helvetica", e);
            chineseFont = PdfFontFactory.createFont();
        }

        // ---- 封面标题 ----
        addTitle(document, chineseFont, "智能饮食管理助手", 22, THEME_COLOR);
        addTitle(document, chineseFont, "个人饮食分析报告", 18, ColorConstants.BLACK);
        addParagraph(document, chineseFont,
                "统计周期：" + start.format(DISPLAY_FMT) + " 至 " + end.format(DISPLAY_FMT), 10, true);
        document.add(createCustomLine(1f, THEME_COLOR));
        document.add(new Paragraph("\n"));

        // ---- 用户基本信息 ----
        if (profile != null) {
            addSectionTitle(document, chineseFont, "【个人健康档案】");
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                    .useAllAvailableWidth();
            addInfoCell(infoTable, chineseFont, "姓名", profile.getNickname());
            addInfoCell(infoTable, chineseFont, "年龄", profile.getAge() + " 岁");
            addInfoCell(infoTable, chineseFont, "身高", profile.getHeightCm() + " cm");
            addInfoCell(infoTable, chineseFont, "体重", profile.getWeightKg() + " kg");
            if (profile.getBmr() != null) {
                addInfoCell(infoTable, chineseFont, "基础代谢", profile.getBmr().intValue() + " kcal/天");
            }
            if (profile.getTdee() != null) {
                addInfoCell(infoTable, chineseFont, "每日消耗", profile.getTdee().intValue() + " kcal/天");
            }
            document.add(infoTable);
            document.add(new Paragraph("\n"));
        }

        // ---- 统计汇总 ----
        if (!records.isEmpty()) {
            addSectionTitle(document, chineseFont, "【营养摄入汇总】");
            NutritionSummary summary = calcSummary(records, start, end);
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                    .useAllAvailableWidth();
            addSummaryCell(summaryTable, chineseFont, "打卡天数", summary.recordDays + " 天");
            addSummaryCell(summaryTable, chineseFont, "日均热量", Math.round(summary.avgCalories) + " kcal");
            addSummaryCell(summaryTable, chineseFont, "日均蛋白质", Math.round(summary.avgProtein) + " g");
            addSummaryCell(summaryTable, chineseFont, "日均碳水", Math.round(summary.avgCarbs) + " g");
            addSummaryCell(summaryTable, chineseFont, "日均脂肪", Math.round(summary.avgFat) + " g");
            addSummaryCell(summaryTable, chineseFont, "平均健康评分",
                    summary.avgScore > 0 ? Math.round(summary.avgScore) + " 分" : "暂无");
            document.add(summaryTable);
            document.add(new Paragraph("\n"));

            // ---- 每日记录明细 ----
            addSectionTitle(document, chineseFont, "【每日饮食明细】");
            Map<LocalDate, List<DietRecordVO>> byDate = records.stream()
                    .collect(Collectors.groupingBy(DietRecordVO::getRecordDate));

            for (Map.Entry<LocalDate, List<DietRecordVO>> entry :
                    byDate.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                addDaySection(document, chineseFont, entry.getKey(), entry.getValue());
            }
        } else {
            addParagraph(document, chineseFont, "该时间段内暂无饮食记录。", 11, false);
        }

        // ---- 页脚 ----
        document.add(new Paragraph("\n"));
        // 【已修复】这里直接复用你的 createCustomLine 方法
        document.add(createCustomLine(0.5f, ColorConstants.LIGHT_GRAY));
        addParagraph(document, chineseFont,
                "本报告由智能饮食管理助手自动生成，营养数据为估算值，仅供参考。", 8, true);

        document.close();
    }

    private void addTitle(Document doc, PdfFont font, String text, float size, com.itextpdf.kernel.colors.Color color) {
        Text t = new Text(stripNonBmp(text)).simulateBold();
        doc.add(new Paragraph(t)
                .setFont(font).setFontSize(size).setFontColor(color)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));
    }

    private void addSectionTitle(Document doc, PdfFont font, String text) {
        Text t = new Text(stripNonBmp(text)).simulateBold();
        doc.add(new Paragraph(t)
                .setFont(font).setFontSize(13).setFontColor(THEME_COLOR)
                .setMarginTop(8).setMarginBottom(4));
    }

    private void addParagraph(Document doc, PdfFont font, String text, float size, boolean center) {
        Paragraph p = new Paragraph(stripNonBmp(text)).setFont(font).setFontSize(size).setFontColor(ColorConstants.GRAY);
        if (center) p.setTextAlignment(TextAlignment.CENTER);
        doc.add(p);
    }

    private void addInfoCell(Table table, PdfFont font, String label, String value) {
        table.addCell(new Cell().add(new Paragraph(stripNonBmp(label)).setFont(font).setFontSize(9)
                .setFontColor(ColorConstants.GRAY)));
        table.addCell(new Cell().add(new Paragraph(value != null ? stripNonBmp(value) : "-").setFont(font).setFontSize(9)));
    }

    private void addSummaryCell(Table table, PdfFont font, String label, String value) {
        table.addCell(new Cell().add(
                new Paragraph(stripNonBmp(label) + "\n" + stripNonBmp(value))
                        .setFont(font).setFontSize(10).setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.BLACK)));
    }

    private void addDaySection(Document doc, PdfFont font, LocalDate date, List<DietRecordVO> dayRecords) {
        Text t = new Text(">> " + date.format(DISPLAY_FMT)).simulateBold();
        doc.add(new Paragraph(t)
                .setFont(font).setFontSize(11).setMarginTop(6));

        for (DietRecordVO r : dayRecords) {
            String rawDesc = r.getMealDescription() != null ? r.getMealDescription() : "无描述";
            String mealInfo = r.getMealTypeLabel() + "：" + stripNonBmp(rawDesc);
            String nutritionInfo = "";
            if (r.getTotalCalories() != null) {
                nutritionInfo = String.format("  [热量 %.0f kcal", r.getTotalCalories().doubleValue());
                if (r.getProteinG() != null)       nutritionInfo += String.format(" | 蛋白质 %.1fg", r.getProteinG().doubleValue());
                if (r.getCarbohydratesG() != null)  nutritionInfo += String.format(" | 碳水 %.1fg", r.getCarbohydratesG().doubleValue());
                if (r.getFatG() != null)             nutritionInfo += String.format(" | 脂肪 %.1fg", r.getFatG().doubleValue());
                nutritionInfo += "]";
            }
            doc.add(new Paragraph("  • " + mealInfo + nutritionInfo)
                    .setFont(font).setFontSize(9).setMarginLeft(10).setMarginBottom(2));

            // ---- 嵌入餐食图片 ----
            if (r.getImageUrl() != null && !r.getImageUrl().isBlank()) {
                try {
                    byte[] imgBytes = loadImageBytes(r.getImageUrl());
                    if (imgBytes != null && imgBytes.length > 0) {
                        ImageData imageData = ImageDataFactory.create(imgBytes);
                        Image img = new Image(imageData)
                                .setMaxWidth(220)
                                .setMarginLeft(20)
                                .setMarginTop(4)
                                .setMarginBottom(6);
                        doc.add(img);
                    }
                } catch (Exception e) {
                    log.warn("[PDF] 图片嵌入失败，跳过: {}", e.getMessage());
                }
            }

            if (r.getAiSuggestion() != null && !r.getAiSuggestion().isBlank()) {
                doc.add(new Paragraph("    * " + stripNonBmp(r.getAiSuggestion()))
                        .setFont(font).setFontSize(8).setFontColor(ColorConstants.GRAY)
                        .setMarginLeft(20).setMarginBottom(4));
            }
        }
    }

    /**
     * 从 Base64 Data URL 或外部 HTTP URL 加载图片字节，并统一转换为 PNG 格式。
     * <p>
     * iText 9 原生不支持 WebP；通过 TwelveMonkeys ImageIO 插件解码后转 PNG，
     * 确保所有格式（JPEG / PNG / WebP / GIF / BMP）都能正常嵌入。
     * </p>
     */
    private byte[] loadImageBytes(String imageUrl) {
        try {
            // 1. 获取原始字节
            byte[] raw;
            if (imageUrl.startsWith("data:")) {
                int comma = imageUrl.indexOf(',');
                if (comma < 0) return null;
                raw = java.util.Base64.getDecoder().decode(imageUrl.substring(comma + 1));
            } else {
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) new java.net.URL(imageUrl).openConnection();
                conn.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(12000);
                try (java.io.InputStream is = conn.getInputStream()) {
                    raw = is.readAllBytes();
                }
            }
            if (raw == null || raw.length == 0) return null;

            // 2. 先尝试 iText 直接识别（JPEG / PNG 等原生支持格式直接复用，性能最优）
            try {
                ImageDataFactory.create(raw);
                return raw; // iText 能识别，直接返回原始字节
            } catch (Exception ignored) {
                // iText 不认识（如 WebP），走 ImageIO 解码再转 PNG
            }

            // 3. 用 ImageIO（已通过 TwelveMonkeys 支持 WebP）解码 → 转为 PNG
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(raw));
            if (bi == null) {
                log.warn("[PDF] ImageIO 无法解码图片，跳过嵌入");
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "PNG", baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.warn("[PDF] 图片加载/转换失败（url前缀={}）: {}",
                    imageUrl.substring(0, Math.min(30, imageUrl.length())), e.getMessage());
            return null;
        }
    }

    private NutritionSummary calcSummary(List<DietRecordVO> records, LocalDate start, LocalDate end) {
        long totalDays = start.until(end).getDays() + 1;
        long recordDays = records.stream().map(DietRecordVO::getRecordDate).distinct().count();

        double totalCal = records.stream()
                .filter(r -> r.getTotalCalories() != null)
                .mapToDouble(r -> r.getTotalCalories().doubleValue()).sum();
        double totalProtein = records.stream()
                .filter(r -> r.getProteinG() != null)
                .mapToDouble(r -> r.getProteinG().doubleValue()).sum();
        double totalCarbs = records.stream()
                .filter(r -> r.getCarbohydratesG() != null)
                .mapToDouble(r -> r.getCarbohydratesG().doubleValue()).sum();
        double totalFat = records.stream()
                .filter(r -> r.getFatG() != null)
                .mapToDouble(r -> r.getFatG().doubleValue()).sum();
        double avgScore = records.stream()
                .filter(r -> r.getHealthScore() != null)
                .mapToDouble(DietRecordVO::getHealthScore).average().orElse(0);

        double divisor = recordDays > 0 ? recordDays : 1;
        return new NutritionSummary(recordDays, totalCal / divisor,
                totalProtein / divisor, totalCarbs / divisor, totalFat / divisor, avgScore);
    }

    private record NutritionSummary(long recordDays, double avgCalories,
                                    double avgProtein, double avgCarbs,
                                    double avgFat, double avgScore) {}

    private LineSeparator createCustomLine(float width, Color color) {
        SolidLine lineDrawer = new SolidLine(width);
        if (color != null) {
            lineDrawer.setColor(color);
        }
        return new LineSeparator(lineDrawer);
    }

    /**
     * 过滤非 BMP 字符（codepoint > U+FFFF，如 emoji）。
     * STSong-Light / UniGB-UCS2-H 字体只支持 BMP 范围，写入非 BMP 字符会抛
     * ITextException: This encoder only accepts BMP codepoints。
     */
    private static String stripNonBmp(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp <= 0xFFFF) {
                sb.appendCodePoint(cp);
            }
            // 非 BMP 字符直接跳过（emoji 等）
            i += Character.charCount(cp);
        }
        return sb.toString();
    }
}