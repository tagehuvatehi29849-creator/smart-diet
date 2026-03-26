package com.hzy.smartdiet.rag;

import com.hzy.smartdiet.constant.PromptConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG 检索服务
 * <p>
 * 将用户的查询文本向量化，从 PgVectorStore 中检索最相关的营养知识片段，
 * 拼接成结构化的上下文字符串，注入到 AI 的 Prompt 中。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;

    /** 默认检索 Top-K 条结果 */
    private static final int DEFAULT_TOP_K = 5;
    /** 相似度阈值（余弦距离，0~1，越高越相似）*/
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * 根据用户输入检索相关营养知识，返回格式化的上下文字符串
     *
     * @param userQuery 用户输入（自然语言）
     * @return 格式化的参考上下文，供注入 Prompt；若无相关结果则返回空字符串
     */
    public String retrieveContext(String userQuery) {
        if (userQuery == null || userQuery.isBlank()) return "";

        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(userQuery)
                            .topK(DEFAULT_TOP_K)
                            .similarityThreshold(SIMILARITY_THRESHOLD)
                            .build()
            );

            if (docs == null || docs.isEmpty()) {
                log.debug("[RAG] 查询 '{}' 未找到相关知识片段", userQuery);
                return "";
            }

            String context = docs.stream()
                    .map(doc -> "- " + doc.getFormattedContent())
                    .collect(Collectors.joining("\n"));

            log.debug("[RAG] 查询 '{}' 检索到 {} 条知识片段", userQuery, docs.size());

            return new org.springframework.ai.chat.prompt.PromptTemplate(PromptConstants.RAG_CONTEXT_TEMPLATE)
                    .create(Map.of("context", context))
                    .getContents();

        } catch (Exception e) {
            log.error("[RAG] 向量检索失败，退化为无 RAG 模式", e);
            return "";
        }
    }

    /**
     * 将文档列表存入向量库（供知识库加载时调用）
     */
    public void storeDocuments(List<Document> documents) {
        if (documents == null || documents.isEmpty()) return;
        vectorStore.add(documents);
        log.info("[RAG] 成功写入 {} 条文档到向量库", documents.size());
    }

    /**
     * 删除指定 ID 的文档（按需维护知识库）
     */
    public void deleteDocuments(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        vectorStore.delete(ids);
        log.info("[RAG] 删除向量库文档 {} 条", ids.size());
    }
}
