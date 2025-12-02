package com.example.springboot.dto.navigation;

import lombok.Data;

import java.util.List;

@Data
public class NavigationRouteResponse {
    /**
     * 路径节点列表（按顺序）
     */
    private List<MapNodeResponse> pathNodes;

    /**
     * 总距离（米）
     */
    private Double totalDistance;

    /**
     * 预计步行时间（秒）
     */
    private Integer totalWalkTime;

    /**
     * 预计步行时间（分钟，格式化）
     */
    private String formattedWalkTime;

    /**
     * 导航指引列表
     */
    private List<NavigationInstruction> instructions;

    /**
     * 导航指引
     */
    @Data
    public static class NavigationInstruction {
        /**
         * 指引步骤序号
         */
        private Integer step;

        /**
         * 指引描述
         */
        private String instruction;

        /**
         * 当前节点
         */
        private MapNodeResponse currentNode;

        /**
         * 下一个节点
         */
        private MapNodeResponse nextNode;

        /**
         * 距离（米）
         */
        private Double distance;

        /**
         * 步行时间（秒）
         */
        private Integer walkTime;
    }
}


