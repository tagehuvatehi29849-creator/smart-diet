package com.hzy.smartdiet.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应体
 *
 * @param <T> 列表元素类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页响应体")
public class PageResult<T> {

    @Schema(description = "当前页码（从 1 开始）")
    private long pageNum;

    @Schema(description = "每页条数")
    private long pageSize;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private long totalPages;

    @Schema(description = "数据列表")
    private List<T> records;

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> records) {
        long totalPages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
        return new PageResult<>(pageNum, pageSize, total, totalPages, records);
    }
}
