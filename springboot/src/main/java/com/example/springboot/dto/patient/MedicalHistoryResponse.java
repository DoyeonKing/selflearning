// filePath: springboot/src/main/java/com/example/springboot/dto/patient/MedicalHistoryResponse.java
package com.example.springboot.dto.patient;

import com.example.springboot.entity.enums.BlacklistStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MedicalHistoryResponse {
    private Long id; // 患者ID
    private String idCard; // 身份证号
    private String name; // 姓名
    private String allergyHistory; // 过敏史
    private String pastMedicalHistory; // 既往病史
    
    // 关键修复：使用JsonProperty确保序列化时字段名为isBlacklisted
    @JsonProperty("isBlacklisted")
    private boolean blacklisted; // 是否拉黑
    
    // 兼容getter/setter方法
    public boolean getIsBlacklisted() {
        return blacklisted;
    }
    
    public void setIsBlacklisted(boolean blacklisted) {
        this.blacklisted = blacklisted;
    }
}