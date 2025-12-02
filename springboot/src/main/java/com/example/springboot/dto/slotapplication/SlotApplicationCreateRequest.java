package com.example.springboot.dto.slotapplication;

import com.example.springboot.entity.enums.UrgencyLevel;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建加号申请请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotApplicationCreateRequest {

    @NotNull(message = "医生ID不能为空")
    private Integer doctorId;

    @NotNull(message = "排班ID不能为空")
    private Integer scheduleId;

    @NotNull(message = "加号数量不能为空")
    @Min(value = 1, message = "加号数量至少为1")
    @Max(value = 20, message = "加号数量最多为20")
    private Integer addedSlots;

    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @NotNull(message = "紧急程度不能为空")
    private UrgencyLevel urgencyLevel;

    @NotBlank(message = "申请理由不能为空")
    @Size(min = 10, max = 500, message = "申请理由长度必须在10-500个字符之间")
    private String reason;
}
