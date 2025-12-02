package com.example.springboot.service;

import com.example.springboot.dto.timeslot.TimeSlotCreateRequest;
import com.example.springboot.dto.timeslot.TimeSlotResponse;
import com.example.springboot.dto.timeslot.TimeSlotUpdateRequest;
import com.example.springboot.entity.TimeSlot;
import com.example.springboot.exception.BadRequestException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.TimeSlotRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimeSlotService {

    private final TimeSlotRepository timeSlotRepository;

    @Autowired
    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> findAllTimeSlots() {
        return timeSlotRepository.findAll().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimeSlotResponse findTimeSlotById(Integer id) {
        TimeSlot timeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot not found with id " + id));
        return convertToResponseDto(timeSlot);
    }

    @Transactional
    public TimeSlotResponse createTimeSlot(TimeSlotCreateRequest request) {
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time.");
        }
        if (timeSlotRepository.existsByStartTimeAndEndTime(request.getStartTime(), request.getEndTime())) {
            throw new BadRequestException("Time slot with same start and end time already exists.");
        }

        TimeSlot timeSlot = new TimeSlot();
        BeanUtils.copyProperties(request, timeSlot);
        return convertToResponseDto(timeSlotRepository.save(timeSlot));
    }

    @Transactional
    public TimeSlotResponse updateTimeSlot(Integer id, TimeSlotUpdateRequest request) {
        TimeSlot existingTimeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot not found with id " + id));

        LocalTime newStartTime = request.getStartTime() != null ? request.getStartTime() : existingTimeSlot.getStartTime();
        LocalTime newEndTime = request.getEndTime() != null ? request.getEndTime() : existingTimeSlot.getEndTime();

        if (newStartTime.isAfter(newEndTime) || newStartTime.equals(newEndTime)) {
            throw new BadRequestException("Start time must be before end time.");
        }

        if ((request.getStartTime() != null || request.getEndTime() != null) &&
                timeSlotRepository.findByStartTimeAndEndTime(newStartTime, newEndTime)
                        .filter(ts -> !ts.getSlotId().equals(id)).isPresent()) {
            throw new BadRequestException("Another time slot with the same start and end time already exists.");
        }

        if (request.getSlotName() != null) existingTimeSlot.setSlotName(request.getSlotName());
        if (request.getStartTime() != null) existingTimeSlot.setStartTime(request.getStartTime());
        if (request.getEndTime() != null) existingTimeSlot.setEndTime(request.getEndTime());

        return convertToResponseDto(timeSlotRepository.save(existingTimeSlot));
    }

    @Transactional
    public void deleteTimeSlot(Integer id) {
        if (!timeSlotRepository.existsById(id)) {
            throw new ResourceNotFoundException("TimeSlot not found with id " + id);
        }
        // TODO: Consider business logic for deleting time slots that are referenced by schedules.
        timeSlotRepository.deleteById(id);
    }

    public TimeSlotResponse convertToResponseDto(TimeSlot timeSlot) {
        TimeSlotResponse response = new TimeSlotResponse();
        BeanUtils.copyProperties(timeSlot, response);
        return response;
    }
}
