package com.example.springboot.service;

import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.entity.enums.AppointmentType;
import com.example.springboot.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 排班可用性检查服务
 * 处理号源可用性计算，考虑加号预留机制
 */
@Service
public class ScheduleAvailabilityService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleAvailabilityService.class);

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public ScheduleAvailabilityService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * 检查排班是否有可用号源（考虑待支付的加号预留）
     * 
     * @param schedule 排班
     * @return 是否有可用号源
     */
    public boolean isSlotAvailable(Schedule schedule) {
        Integer actualAvailable = getActualAvailableSlots(schedule);
        return actualAvailable > 0;
    }

    /**
     * 获取实际可用号源数量
     * 实际可用 = totalSlots - bookedSlots - 待支付的加号数量
     * 
     * @param schedule 排班
     * @return 实际可用号源数量
     */
    public Integer getActualAvailableSlots(Schedule schedule) {
        Integer totalSlots = schedule.getTotalSlots();
        Integer bookedSlots = schedule.getBookedSlots();
        
        // 统计待支付的加号数量（这些号源被"软预留"，不应对普通患者开放）
        Integer pendingAddOnCount = appointmentRepository.countByScheduleAndTypeAndStatus(
                schedule, 
                AppointmentType.ADD_ON, 
                AppointmentStatus.PENDING_PAYMENT);
        
        Integer actualAvailable = totalSlots - bookedSlots - pendingAddOnCount;
        
        logger.debug("排班可用号源计算 - scheduleId: {}, 总号源: {}, 已预约: {}, 待支付加号: {}, 实际可用: {}",
                schedule.getScheduleId(), totalSlots, bookedSlots, pendingAddOnCount, actualAvailable);
        
        return Math.max(0, actualAvailable); // 确保不返回负数
    }

    /**
     * 获取排班的详细可用性信息
     * 
     * @param schedule 排班
     * @return 可用性详情
     */
    public AvailabilityInfo getAvailabilityInfo(Schedule schedule) {
        Integer totalSlots = schedule.getTotalSlots();
        Integer bookedSlots = schedule.getBookedSlots();
        
        // 统计待支付的加号数量
        Integer pendingAddOnCount = appointmentRepository.countByScheduleAndTypeAndStatus(
                schedule, 
                AppointmentType.ADD_ON, 
                AppointmentStatus.PENDING_PAYMENT);
        
        // 统计已支付的加号数量
        Integer paidAddOnCount = appointmentRepository.countByScheduleAndTypeAndStatus(
                schedule,
                AppointmentType.ADD_ON,
                AppointmentStatus.scheduled);
        
        Integer actualAvailable = totalSlots - bookedSlots - pendingAddOnCount;
        
        return new AvailabilityInfo(
                totalSlots,
                bookedSlots,
                pendingAddOnCount,
                paidAddOnCount,
                Math.max(0, actualAvailable)
        );
    }

    /**
     * 可用性信息类
     */
    public static class AvailabilityInfo {
        private final Integer totalSlots;          // 总号源数
        private final Integer bookedSlots;         // 已预约数
        private final Integer pendingAddOnCount;   // 待支付加号数
        private final Integer paidAddOnCount;      // 已支付加号数
        private final Integer actualAvailable;     // 实际可用数

        public AvailabilityInfo(Integer totalSlots, Integer bookedSlots, 
                               Integer pendingAddOnCount, Integer paidAddOnCount,
                               Integer actualAvailable) {
            this.totalSlots = totalSlots;
            this.bookedSlots = bookedSlots;
            this.pendingAddOnCount = pendingAddOnCount;
            this.paidAddOnCount = paidAddOnCount;
            this.actualAvailable = actualAvailable;
        }

        public Integer getTotalSlots() {
            return totalSlots;
        }

        public Integer getBookedSlots() {
            return bookedSlots;
        }

        public Integer getPendingAddOnCount() {
            return pendingAddOnCount;
        }

        public Integer getPaidAddOnCount() {
            return paidAddOnCount;
        }

        public Integer getActualAvailable() {
            return actualAvailable;
        }

        @Override
        public String toString() {
            return String.format("AvailabilityInfo{总号源=%d, 已预约=%d, 待支付加号=%d, 已支付加号=%d, 实际可用=%d}",
                    totalSlots, bookedSlots, pendingAddOnCount, paidAddOnCount, actualAvailable);
        }
    }
}
