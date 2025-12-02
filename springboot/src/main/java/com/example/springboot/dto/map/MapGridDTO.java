package com.example.springboot.dto.map;

import lombok.Data;

/**
 * 地图网格数据传输对象
 * 用于传输地图网格数据
 */
@Data
public class MapGridDTO {
    /**
     * 网格宽度（列数）
     */
    private int width;
    
    /**
     * 网格高度（行数）
     */
    private int height;
    
    /**
     * 网格矩阵
     * 0 = 通路（可通行）
     * 1 = 障碍物（墙壁、不可通行）
     */
    private int[][] gridMatrix;
    
    public MapGridDTO() {
    }
    
    public MapGridDTO(int width, int height, int[][] gridMatrix) {
        this.width = width;
        this.height = height;
        this.gridMatrix = gridMatrix;
    }
}


















