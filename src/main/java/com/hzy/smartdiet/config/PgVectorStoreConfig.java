package com.hzy.smartdiet.config;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * PgVector 向量存储手动配置
 * <p>
 * 从 Baomidou 动态数据源中精准提取 postgres 连接池，
 * 避免手动 @Bean DataSource 导致 Spring 全局事务和 MyBatis 路由被劫持。
 * </p>
 */
@Configuration
public class PgVectorStoreConfig {

    @Value("${spring.ai.vectorstore.pgvector.dimensions:1536}")
    private int dimensions;

    @Value("${spring.ai.vectorstore.pgvector.initialize-schema:true}")
    private boolean initializeSchema;

    /**
     * PgVectorStore：Spring AI 的向量存储实现
     * * @param dataSource 这里的 DataSource 实际上是 Spring 注入的 Baomidou 全局动态路由器
     * @param embeddingModel 阿里云 DashScope 的 Embedding 模型
     */
    @Bean
    public VectorStore pgVectorStore(DataSource dataSource, EmbeddingModel embeddingModel) {

        // 1. 将全局数据源强转为 Baomidou 的动态数据源路由器
        DynamicRoutingDataSource dynamicDataSource = (DynamicRoutingDataSource) dataSource;

        // 2. 精准提取我们在 application-local.yml 中配置的 'postgres' 数据源！
        // 这样就完全不会污染 MyBatis 的 master(mysql) 数据源了
        DataSource pgDataSource = dynamicDataSource.getDataSource("postgres");

        // 3. 构造专属的 JdbcTemplate，只给 AI 向量库在内部悄悄使用
        JdbcTemplate jdbcTemplate = new JdbcTemplate(pgDataSource);

        // 4. 构建并返回 PgVectorStore
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(dimensions)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(initializeSchema)
                .build();
    }
}