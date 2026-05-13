package com.afya.afya_health_system.soa.admission.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class LengthOfStayCalculatorService {
    public long calculateCalendarDays(LocalDateTime admissionDateTime, LocalDateTime dischargeDateTime) {
        LocalDate admissionDate = admissionDateTime.toLocalDate();
        LocalDate endDate = dischargeDateTime == null ? LocalDate.now() : dischargeDateTime.toLocalDate();
        return ChronoUnit.DAYS.between(admissionDate, endDate) + 1;
    }
}
