package com.example.springboot.dto.location;

import lombok.Data;

@Data
public class LocationResponse {
    private Integer locationId;
    private String locationName;
    private Integer departmentId;
    private String departmentName;
    private Integer floorLevel;
    private String building;
    private String roomNumber;
    private Integer capacity;
    private Integer mapNodeId;
}

