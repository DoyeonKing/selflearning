package com.example.springboot.converter;

import com.example.springboot.entity.enums.LeaveRequestStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LeaveRequestStatusConverter implements AttributeConverter<LeaveRequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(LeaveRequestStatus status) {
        if (status == null) {
            return null;
        }
        // 转换为小写存储到数据库
        return status.name().toLowerCase();
    }

    @Override
    public LeaveRequestStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.trim().isEmpty()) {
            return null;
        }
        
        // 从数据库读取时转换为枚举
        try {
            return LeaveRequestStatus.valueOf(dbValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果无法匹配，返回默认值
            return LeaveRequestStatus.PENDING;
        }
    }
}
