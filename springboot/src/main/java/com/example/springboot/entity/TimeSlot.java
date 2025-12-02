package com.example.springboot.entity; // 包名调整

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

/**
 * 固定时间段表
 */
@Entity
@Table(name = "time_slots")
@Data
public class TimeSlot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer slotId;

    @Column(name = "slot_name", length = 100)
    private String slotName; // 时段名称 (如“上午 08:00-08:30”)

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
}
