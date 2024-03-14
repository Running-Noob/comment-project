package com.f.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScrollResult {
    private List<?> list;
    private Long minTime;   // 查询到的min，作为下一次查询的max
    private Integer offset; // 读取偏移量
}
