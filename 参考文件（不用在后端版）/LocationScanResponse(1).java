package com.example.springboot.dto.navigation;

import lombok.Data;

@Data
public class LocationScanResponse {
    /**
     * 当前定位的节点信息
     */
    private MapNodeResponse currentNode;

    /**
     * 定位成功消息
     */
    private String message;
}


