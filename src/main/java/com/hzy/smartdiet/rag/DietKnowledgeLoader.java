package com.hzy.smartdiet.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 饮食知识库加载器
 * <p>
 * 在应用启动时（若 {@code app.rag.dataloader.enabled=true}）将内置的
 * 营养学知识片段向量化并写入 PgVector，后续 RAG 检索时使用。
 * <br>
 * 生产环境可将知识来源替换为：
 * <ul>
 * <li>Markdown/PDF 文件（通过 MarkdownDocumentReader 等读取）</li>
 * <li>MyBatis-Plus 查询数据库生成的文档</li>
 * <li>Jsoup 爬取的营养学网页</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DietKnowledgeLoader implements ApplicationRunner {

    // 假设你已经定义好了 RagService 并在里面封装了 VectorStore 的写入逻辑
    private final RagService ragService;

    @Value("${app.rag.dataloader.enabled:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("[RAG] 知识库加载已禁用（app.rag.dataloader.enabled=false），跳过初始化");
            return;
        }
        log.info("[RAG] 开始初始化饮食营养知识库...");
        try {
            List<Document> rawDocs = buildKnowledgeDocs();
            // 使用 TokenTextSplitter 将长文档切分为更小的语义块
            TokenTextSplitter splitter = new TokenTextSplitter(512, 100, 5, 10000, true);
            List<Document> chunks = splitter.apply(rawDocs);
            ragService.storeDocuments(chunks);
            log.info("[RAG] 知识库初始化完成，共写入 {} 个文档块", chunks.size());
        } catch (Exception e) {
            log.error("[RAG] 知识库初始化失败，将在无知识库模式下运行", e);
        }
    }

    /**
     * 构建内置营养学知识文档
     * 实际项目中可改为读取外部文件或数据库
     */
    private List<Document> buildKnowledgeDocs() {
        List<Document> docs = new ArrayList<>();

        // 【关键修复点】：显式声明 Map 的泛型为 <String, Object>
        Map<String, Object> meta = Map.<String, Object>of("source", "builtin", "domain", "nutrition");

        // ---- 宏量营养素 ----
        docs.add(new Document("""
                【蛋白质基础知识】
                蛋白质是人体必需的大量营养素，每克提供 4 kcal 热量。
                成人每日推荐摄入量（RDA）为 0.8g/kg 体重，健身增肌人群可提高至 1.6-2.2g/kg。
                优质蛋白来源：鸡蛋、瘦肉、鱼虾、豆腐、牛奶。
                蛋白质摄入不足会导致肌肉流失、免疫力下降；过量摄入会增加肾脏负担。
                """, meta));

        docs.add(new Document("""
                【碳水化合物基础知识】
                碳水化合物是人体最主要的能量来源，每克提供 4 kcal。
                分为简单碳水（糖类，如白糖、果汁）和复合碳水（淀粉、膳食纤维）。
                推荐每日摄入量占总热量的 45-65%。
                糖尿病患者应重点控制简单碳水摄入，优先选择低GI食物（燕麦、糙米、藜麦）。
                """, meta));

        docs.add(new Document("""
                【脂肪基础知识】
                脂肪每克提供 9 kcal，是三大宏量营养素中能量密度最高的。
                分为饱和脂肪（动物油脂、椰子油）、不饱和脂肪（橄榄油、坚果、鱼油）和反式脂肪（人造黄油）。
                推荐每日摄入量占总热量的 20-35%，应限制饱和脂肪和反式脂肪。
                Omega-3 脂肪酸（深海鱼、核桃、亚麻籽）有益心脑血管健康。
                """, meta));

        // ---- 常见饮食问题 ----
        docs.add(new Document("""
                【减重饮食原则】
                科学减重的核心是制造热量缺口（摄入热量 < 消耗热量），建议每天缺口 300-500 kcal。
                避免极端节食，每周减重速度不超过 0.5-1 kg。
                高蛋白饮食有助于保持饱腹感和肌肉量。
                增加蔬菜（低热量、高纤维）摄入，减少精制碳水和高糖食物。
                推荐饮食模式：地中海饮食、DASH 饮食。
                """, meta));

        docs.add(new Document("""
                【增肌饮食原则】
                增肌需要热量盈余（摄入 > 消耗，建议每天多摄入 250-500 kcal）。
                蛋白质是肌肉合成的原料，推荐每公斤体重摄入 1.6-2.2g。
                训练后 30-60 分钟内补充蛋白质+碳水有助于肌肉恢复（如鸡胸肉+米饭）。
                保证充足的睡眠和休息，肌肉在休息时生长。
                """, meta));

        docs.add(new Document("""
                【糖尿病饮食管理】
                糖尿病患者应控制总碳水摄入量，尤其是精制糖和高GI食物。
                推荐低GI食物：燕麦、糙米、绿叶蔬菜、豆类。
                少量多餐，避免血糖大幅波动。
                建议每餐碳水摄入量约 45-60g（2型糖尿病）。
                增加膳食纤维摄入（>25g/天）有助于延缓血糖上升。
                """, meta));

        docs.add(new Document("""
                【高血压饮食管理】
                DASH 饮食是经科学验证的降压饮食方案。
                核心原则：减少钠盐摄入（<2300mg/天，最好<1500mg/天）。
                增加钾、镁、钙的摄入：香蕉、土豆、菠菜、低脂奶制品。
                减少饱和脂肪、红肉、加工食品；增加全谷物、蔬菜、水果、坚果。
                适量饮酒：男性≤2标准杯/天，女性≤1标准杯/天。
                """, meta));

        // ---- 常见食物营养速查 ----
        docs.add(new Document("""
                【常见主食热量参考（每100g）】
                大米（生）：约 356 kcal | 米饭（熟）：约 116 kcal
                面条（熟）：约 137 kcal | 馒头：约 221 kcal
                燕麦片：约 379 kcal | 糙米（生）：约 357 kcal
                红薯（熟）：约 99 kcal | 土豆（煮）：约 77 kcal
                玉米（熟）：约 112 kcal
                """, meta));

        docs.add(new Document("""
                【常见蛋白质食物热量参考（每100g）】
                鸡胸肉（煮）：约 165 kcal，蛋白质 31g
                鸡蛋（整个）：约 155 kcal，蛋白质 13g
                猪瘦肉：约 143 kcal，蛋白质 20g
                牛里脊：约 158 kcal，蛋白质 26g
                三文鱼：约 208 kcal，蛋白质 20g
                北豆腐：约 76 kcal，蛋白质 8g
                脱脂牛奶（100ml）：约 35 kcal，蛋白质 3.5g
                """, meta));

        docs.add(new Document("""
                【膳食纤维的重要性】
                膳食纤维分为可溶性（燕麦、苹果、豆类）和不可溶性（全麦、麸皮、蔬菜）。
                成人推荐每日摄入 25-38g。
                作用：促进肠道蠕动、控制血糖、降低胆固醇、增加饱腹感。
                富含纤维的食物：西蓝花、菠菜、胡萝卜、苹果、豆类、全谷物。
                增加纤维摄入时需同步增加饮水量，否则可能导致便秘。
                """, meta));

        docs.add(new Document("""
                【每日饮水建议】
                成人每日推荐饮水量：男性约 3.7L（含食物中水分），女性约 2.7L。
                纯饮水量建议：约 1.5-2L/天（8杯水）。
                运动后、高温天气、大量出汗时需额外补水。
                饮水最佳时机：晨起空腹、餐前半小时、运动前后。
                含糖饮料不能替代水，过量摄入会增加肥胖和糖尿病风险。
                """, meta));

        return docs;
    }
}