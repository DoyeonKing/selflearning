package com.example.springboot.dto.slotapplication;

import com.example.springboot.entity.enums.SlotApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新加号申请请求DTO（用于审批）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotApplicationUpdateRequest {

    @NotNull(message = "状态不能为空")
    private SlotApplicationStatus status;

    private Integer approverId;

    private String approverComments;
}
