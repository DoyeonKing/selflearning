// filePath: springboot/src/main/java/com/example/springboot/dto/patient/MedicalHistoryUpdateRequest.java
package com.example.springboot.dto.patient;

import com.example.springboot.entity.enums.BlacklistStatus;
import lombok.Data;

@Data
public class MedicalHistoryUpdateRequest {
    private String allergyHistory; // 过敏史
    private String pastMedicalHistory; // 既往病史
    private BlacklistStatus blacklistStatus; // 黑名单状态
}