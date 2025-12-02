package com.example.springboot.service;

import com.example.springboot.dto.report.RegistrationHoursResponse;
import com.example.springboot.entity.Appointment;
import com.example.springboot.entity.Department;
import com.example.springboot.entity.Doctor;
import com.example.springboot.entity.Location;
import com.example.springboot.entity.Schedule;
import com.example.springboot.entity.TimeSlot;
import com.example.springboot.entity.enums.AppointmentStatus;
import com.example.springboot.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationReportService {

    private static final Duration BREAK_THRESHOLD = Duration.ofMinutes(90); // 午休>=1.5小时视为分段
    private static final LocalTime NIGHT_THRESHOLD = LocalTime.of(18, 0);
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.CHINA);
    private static final BigDecimal DEFAULT_COEFFICIENT = BigDecimal.ONE;
    private static final ZoneId TARGET_ZONE = ZoneId.of("Asia/Shanghai");

    private final AppointmentRepository appointmentRepository;

    public List<RegistrationHoursResponse> collectRegistrationHours(Integer departmentId,
                                                                    Integer doctorId,
                                                                    LocalDate startDate,
                                                                    LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("必须提供起止日期");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        }

        log.info("开始计算挂号工时，departmentId={}, doctorId={}, startDate={}, endDate={}",
                departmentId, doctorId, startDate, endDate);

        List<Appointment> appointments = appointmentRepository.findAppointmentsForRegistrationHours(
                departmentId,
                doctorId,
                startDate,
                endDate,
                AppointmentStatus.completed
        );

        log.info("查询到满足条件的号源 {} 条", appointments.size());

        if (appointments.isEmpty()) {
            return List.of();
        }

        Map<DoctorDayKey, List<VisitPoint>> grouped = new LinkedHashMap<>();
        for (Appointment appointment : appointments) {
            Schedule schedule = appointment.getSchedule();
            if (schedule == null || schedule.getDoctor() == null) {
                continue;
            }
            LocalDate workDate = schedule.getScheduleDate();
            if (workDate == null) {
                continue;
            }

            LocalDateTime rawCallTime = resolveCallTime(appointment, schedule);
            LocalDateTime rawEndTime = resolveEndTime(appointment, schedule, rawCallTime);
            LocalDateTime callTime = convertToTargetZone(rawCallTime);
            LocalDateTime endTime = convertToTargetZone(rawEndTime);
            if (callTime == null || endTime == null) {
                continue;
            }

            Doctor doctor = schedule.getDoctor();
            Department department = doctor.getDepartment();

            DoctorDayKey key = new DoctorDayKey(
                    doctor.getDoctorId(),
                    doctor.getFullName(),
                    department != null ? department.getDepartmentId() : null,
                    department != null ? department.getName() : null,
                    department != null && department.getParentDepartment() != null ? department.getParentDepartment().getParentDepartmentId() : null,
                    department != null && department.getParentDepartment() != null ? department.getParentDepartment().getName() : null,
                    workDate
            );

            VisitPoint point = new VisitPoint(callTime, endTime, extractLocation(schedule));
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(point);
        }

        List<RegistrationHoursResponse> responses = new ArrayList<>();
        for (Map.Entry<DoctorDayKey, List<VisitPoint>> entry : grouped.entrySet()) {
            List<VisitPoint> points = entry.getValue();
            points.sort(Comparator.comparing(VisitPoint::callTime));
            List<Segment> segments = splitSegments(points);
            int index = 1;
            for (Segment segment : segments) {
                responses.add(buildResponse(entry.getKey(), segment, index++));
            }
            log.debug("医生 {} 日期 {} 共拆分 {} 个班段", entry.getKey().doctorName(), entry.getKey().workDate(), segments.size());
            for (int i = 0; i < segments.size(); i++) {
                Segment seg = segments.get(i);
                log.debug("  班段{}: start={}, end={}, rawHours={}, regHours={}, visits={}",
                        i + 1,
                        seg.getFirstCallTime(),
                        seg.getLastEndTime(),
                        seg.getRawHours(),
                        seg.getRegHours(),
                        seg.getVisitCount());
            }
        }

        responses.sort(Comparator
                .comparing(RegistrationHoursResponse::getWorkDate)
                .thenComparing(RegistrationHoursResponse::getDoctorName, Comparator.nullsLast(String::compareTo))
                .thenComparing(RegistrationHoursResponse::getSegmentIndex));

        log.info("挂号工时生成完成，总记录数 {}", responses.size());

        return responses;
    }

    public byte[] exportRegistrationHoursExcel(Integer departmentId,
                                               Integer doctorId,
                                               LocalDate startDate,
                                               LocalDate endDate) {
        List<RegistrationHoursResponse> data = collectRegistrationHours(departmentId, doctorId, startDate, endDate);
        log.info("开始导出挂号工时 Excel，记录数 {}", data.size());
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("挂号工时");
            String[] headers = {
                    "医生",
                    "所属科室",
                    "日期",
                    "班段",
                    "首诊时间",
                    "末诊时间",
                    "原始工时(h)",
                    "挂号工时(h)",
                    "门诊人次",
                    "夜班标记",
                    "诊室/地点",
                    "科室系数",
                    "绩效点数"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (RegistrationHoursResponse item : data) {
                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(nvl(item.getDoctorName()));
                row.createCell(col++).setCellValue(nvl(item.getDepartmentName()));
                row.createCell(col++).setCellValue(item.getWorkDate() != null ? item.getWorkDate().toString() : "");
                row.createCell(col++).setCellValue(nvl(item.getSegmentLabel()));
                row.createCell(col++).setCellValue(formatDateTime(item.getFirstCallTime()));
                row.createCell(col++).setCellValue(formatDateTime(item.getLastEndTime()));
                row.createCell(col++).setCellValue(toDouble(item.getRawHours()));
                row.createCell(col++).setCellValue(toDouble(item.getRegHours()));
                row.createCell(col++).setCellValue(item.getVisitCount() != null ? item.getVisitCount() : 0);
                row.createCell(col++).setCellValue(item.isNightFlag() ? "是" : "否");
                row.createCell(col++).setCellValue(nvl(item.getLocations()));
                row.createCell(col++).setCellValue(toDouble(item.getDepartmentCoefficient()));
                row.createCell(col).setCellValue(toDouble(item.getPerformancePoints()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("导出挂号工时报表失败", e);
        }
    }

    private RegistrationHoursResponse buildResponse(DoctorDayKey key, Segment segment, int segmentIndex) {
        RegistrationHoursResponse response = new RegistrationHoursResponse();
        response.setDoctorId(key.doctorId());
        response.setDoctorName(key.doctorName());
        response.setDepartmentId(key.departmentId());
        response.setDepartmentName(key.departmentName());
        response.setParentDepartmentId(key.parentDepartmentId());
        response.setParentDepartmentName(key.parentDepartmentName());
        response.setWorkDate(key.workDate());
        response.setSegmentIndex(segmentIndex);
        response.setSegmentLabel(determineSegmentLabel(segment.getFirstCallTime(), segment.getLastEndTime(), segmentIndex));
        response.setFirstCallTime(segment.getFirstCallTime());
        response.setLastEndTime(segment.getLastEndTime());
        response.setRawHours(segment.getRawHours());
        response.setRegHours(segment.getRegHours());
        response.setVisitCount(segment.getVisitCount());
        response.setNightFlag(segment.isNightFlag());
        response.setLocations(segment.getLocations());

        BigDecimal coefficient = DEFAULT_COEFFICIENT;
        response.setDepartmentCoefficient(coefficient);
        BigDecimal performance = coefficient.multiply(BigDecimal.valueOf(segment.getVisitCount()))
                .setScale(2, RoundingMode.HALF_UP);
        response.setPerformancePoints(performance);
        return response;
    }

    private String determineSegmentLabel(LocalDateTime first, LocalDateTime last, int index) {
        if (first == null) {
            return "班段 " + index;
        }
        int hour = first.getHour();
        if (hour < 12) {
            return "上午段";
        }
        if (hour < 18) {
            return "下午段";
        }
        return "夜间段";
    }

    private static List<Segment> splitSegments(List<VisitPoint> points) {
        List<Segment> segments = new ArrayList<>();
        Segment current = null;
        for (VisitPoint point : points) {
            if (current == null) {
                current = Segment.from(point);
                continue;
            }
            Duration gap = Duration.between(current.getLastEndTime(), point.callTime());
            if (gap.compareTo(BREAK_THRESHOLD) >= 0) {
                segments.add(current);
                current = Segment.from(point);
            } else {
                current.extend(point);
            }
        }
        if (current != null) {
            segments.add(current);
        }
        return segments;
    }

    private LocalDateTime resolveCallTime(Appointment appointment, Schedule schedule) {
        if (appointment.getCheckInTime() != null) {
            return appointment.getCheckInTime();
        }
        TimeSlot slot = schedule.getSlot();
        LocalTime start = slot != null ? slot.getStartTime() : null;
        if (start == null) {
            start = LocalTime.MIDNIGHT;
        }
        LocalDate date = schedule.getScheduleDate();
        return date != null ? LocalDateTime.of(date, start) : null;
    }

    private LocalDateTime resolveEndTime(Appointment appointment, Schedule schedule, LocalDateTime fallback) {
        // 优先使用 status 变为 completed 时的 updated_at 作为就诊结束时间
        if (appointment.getUpdatedAt() != null && appointment.getStatus() == AppointmentStatus.completed) {
            LocalDateTime updatedAt = appointment.getUpdatedAt();
            LocalDate workDate = schedule.getScheduleDate();
            
            // 合理性校验：updated_at 必须在 check_in_time 之后，不能早于首诊时间
            if (appointment.getCheckInTime() != null && updatedAt.isBefore(appointment.getCheckInTime())) {
                log.warn("预约 {} 的 updated_at ({}) 早于 check_in_time ({})，使用排班时段结束时间", 
                        appointment.getAppointmentId(), updatedAt, appointment.getCheckInTime());
            } else if (fallback != null && updatedAt.isBefore(fallback)) {
                log.warn("预约 {} 的 updated_at ({}) 早于首诊时间 ({})，使用首诊时间", 
                        appointment.getAppointmentId(), updatedAt, fallback);
                return fallback;
            } else {
                // 校验通过，使用 updated_at 作为就诊结束时间
                return updatedAt;
            }
        }
        
        // 如果 updated_at 不存在或不合理，使用排班时段的结束时间
        TimeSlot slot = schedule.getSlot();
        LocalTime end = slot != null ? slot.getEndTime() : null;
        LocalDate date = schedule.getScheduleDate();
        LocalDateTime candidate = (end != null && date != null) ? LocalDateTime.of(date, end) : null;
        if (candidate == null) {
            // 如果都没有，回退到签到时间或首诊时间
            return appointment.getCheckInTime() != null ? appointment.getCheckInTime() : fallback;
        }
        // 确保结束时间不早于首诊时间
        if (fallback != null && candidate.isBefore(fallback)) {
            return fallback;
        }
        return candidate;
    }

    private String extractLocation(Schedule schedule) {
        Location location = schedule.getLocation();
        if (location != null && location.getLocationName() != null) {
            return location.getLocationName();
        }
        return null;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME_FMT);
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }

    private LocalDateTime convertToTargetZone(LocalDateTime source) {
        if (source == null) {
            return null;
        }
        return source;
    }

    private record DoctorDayKey(Integer doctorId,
                                String doctorName,
                                Integer departmentId,
                                String departmentName,
                                Integer parentDepartmentId,
                                String parentDepartmentName,
                                LocalDate workDate) {
    }

    private record VisitPoint(LocalDateTime callTime, LocalDateTime endTime, String location) {
    }

    private static class Segment {
        private LocalDateTime firstCallTime;
        private LocalDateTime lastEndTime;
        private int visitCount;
        private final Set<String> locationSet = new LinkedHashSet<>();
        private boolean nightFlag;

        private static Segment from(VisitPoint point) {
            Segment segment = new Segment();
            segment.firstCallTime = point.callTime();
            segment.lastEndTime = point.endTime();
            if (segment.lastEndTime == null) {
                segment.lastEndTime = segment.firstCallTime;
            }
            if (segment.lastEndTime != null && segment.firstCallTime != null && segment.lastEndTime.isBefore(segment.firstCallTime)) {
                segment.lastEndTime = segment.firstCallTime;
            }
            segment.visitCount = 1;
            segment.nightFlag = isNight(point);
            if (point.location() != null) {
                segment.locationSet.add(point.location());
            }
            return segment;
        }

        private void extend(VisitPoint point) {
            this.visitCount += 1;
            if (point.endTime() != null && (this.lastEndTime == null || point.endTime().isAfter(this.lastEndTime))) {
                this.lastEndTime = point.endTime();
            }
            this.nightFlag = this.nightFlag || isNight(point);
            if (point.location() != null) {
                this.locationSet.add(point.location());
            }
        }

        private static boolean isNight(VisitPoint point) {
            LocalTime call = point.callTime() != null ? point.callTime().toLocalTime() : null;
            LocalTime end = point.endTime() != null ? point.endTime().toLocalTime() : null;
            return (call != null && !call.isBefore(NIGHT_THRESHOLD)) || (end != null && !end.isBefore(NIGHT_THRESHOLD));
        }

        public LocalDateTime getFirstCallTime() {
            return firstCallTime;
        }

        public LocalDateTime getLastEndTime() {
            return lastEndTime;
        }

        public int getVisitCount() {
            return visitCount;
        }

        public boolean isNightFlag() {
            return nightFlag;
        }

        public String getLocations() {
            return locationSet.isEmpty() ? "" : String.join("、", locationSet);
        }

        public BigDecimal getRawHours() {
            if (firstCallTime == null || lastEndTime == null) {
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            long minutes = Math.max(0, Duration.between(firstCallTime, lastEndTime).toMinutes());
            return BigDecimal.valueOf(minutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        }

        public BigDecimal getRegHours() {
            return getRawHours()
                    .add(BigDecimal.valueOf(0.5))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}

