package com.example.springboot.controller;

import com.example.springboot.dto.report.DoctorHoursResponse;
import com.example.springboot.dto.report.RegistrationHoursResponse;
import com.example.springboot.dto.schedule.ScheduleListRequest;
import com.example.springboot.dto.schedule.ScheduleResponse;
import com.example.springboot.service.ScheduleService;
import com.example.springboot.service.RegistrationReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private RegistrationReportService registrationReportService;

    @GetMapping("/doctor-hours")
    public ResponseEntity<List<DoctorHoursResponse>> getDoctorHours(
            @RequestParam Integer departmentId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Integer doctorId,
            @RequestParam(defaultValue = "doctor") String groupBy
    ) {
        ScheduleListRequest req = new ScheduleListRequest();
        req.setDepartmentId(departmentId);
        req.setStartDate(LocalDate.parse(startDate));
        req.setEndDate(LocalDate.parse(endDate));

        // 取足够大的分页，避免多次请求
        Pageable pageable = PageRequest.of(0, 2000);
        Page<ScheduleResponse> page = scheduleService.getSchedules(req, pageable);
        List<ScheduleResponse> list = page.getContent();

        if (doctorId != null) {
            list = list.stream().filter(s -> Objects.equals(s.getDoctorId(), doctorId)).collect(Collectors.toList());
        }

        // 分组 key: doctor 或 doctor+date
        Map<String, Aggregation> map = new LinkedHashMap<>();
        for (ScheduleResponse s : list) {
            String key = "doctor_date".equalsIgnoreCase(groupBy)
                    ? s.getDoctorId() + "|" + nullSafe(s.getDoctorName()) + "|" + String.valueOf(s.getScheduleDate())
                    : s.getDoctorId() + "|" + nullSafe(s.getDoctorName());

            Aggregation agg = map.computeIfAbsent(key, k -> new Aggregation());
            agg.doctorId = s.getDoctorId();
            agg.doctorName = s.getDoctorName();
            if ("doctor_date".equalsIgnoreCase(groupBy)) {
                agg.date = s.getScheduleDate();
            }
            agg.sessions++;

            // 以时段开始结束时间计算时长，兜底为4小时
            double hours = 4.0;
            if (s.getStartTime() != null && s.getEndTime() != null) {
                Duration d = Duration.between(s.getStartTime(), s.getEndTime());
                hours = Math.max(0, d.toMinutes() / 60.0);
            }
            agg.hours += hours;

            if (s.getLocation() != null) {
                agg.locations.add(s.getLocation());
            }
        }

        List<DoctorHoursResponse> result = map.values().stream().map(a -> {
            DoctorHoursResponse r = new DoctorHoursResponse();
            r.setDoctorId(a.doctorId);
            r.setDoctorName(a.doctorName);
            r.setDate(a.date);
            r.setSessions(a.sessions);
            // 保留两位小数
            r.setHours(new BigDecimal(String.format(java.util.Locale.ROOT, "%.2f", a.hours)));
            r.setLocations(String.join("、", a.locations));
            return r;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/registration-hours")
    public ResponseEntity<List<RegistrationHoursResponse>> getRegistrationHours(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer doctorId
    ) {
        List<RegistrationHoursResponse> data = registrationReportService.collectRegistrationHours(
                departmentId,
                doctorId,
                startDate,
                endDate
        );
        return ResponseEntity.ok(data);
    }

    @GetMapping("/registration-hours/export")
    public ResponseEntity<byte[]> exportRegistrationHours(
            @RequestParam(required = false) Integer departmentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer doctorId
    ) {
        byte[] content = registrationReportService.exportRegistrationHoursExcel(
                departmentId,
                doctorId,
                startDate,
                endDate
        );
        String filename = String.format("registration-hours-%s-%s.xlsx", startDate, endDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(content.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    private String nullSafe(String s) { return s == null ? "" : s; }

    private static class Aggregation {
        Integer doctorId;
        String doctorName;
        LocalDate date;
        int sessions = 0;
        double hours = 0.0;
        Set<String> locations = new LinkedHashSet<>();
    }
}


