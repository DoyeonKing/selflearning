package com.example.springboot.dto.navigation;

import lombok.Data;

@Data
public class LocationScanRequest {
    /**
     * 扫描到的定位二维码内容
     * 格式：LOCATION_NODE_{node_id}
     */
    private String qrCode;
}


