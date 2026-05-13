package com.afya.afya_health_system.soa.reporting.controller;

import com.afya.afya_health_system.soa.reporting.dto.ExportResponse;
import com.afya.afya_health_system.soa.reporting.dto.AuditEventResponse;
import com.afya.afya_health_system.soa.reporting.dto.MetricResponse;
import com.afya.afya_health_system.soa.reporting.service.ReportingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only KPI and export stubs (implementation still placeholder in service tier).
 * <p>Français : statistiques et exports (valeurs encore factices côté service).</p>
 */
@RestController
@RequestMapping("/api/v1")
public class ReportingController {

    private final ReportingService service;

    public ReportingController(ReportingService service) {
        this.service = service;
    }

    /**
     * Bed occupancy KPI placeholder.
     * <p>Français : taux d'occupation (non calculé pour l'instant).</p>
     */
    @GetMapping("/stats/occupancy")
    public MetricResponse occupancy() {
        return service.occupancy();
    }

    @GetMapping("/stats/occupancy-by-service")
    public MetricResponse occupancyByService() {
        return service.occupancyByService();
    }

    /**
     * Admissions volume placeholder.
     * <p>Français : volume d'admissions (factice).</p>
     */
    @GetMapping("/stats/admissions")
    public MetricResponse admissions(@RequestParam(required = false) Integer days) {
        return service.admissions(days);
    }

    /**
     * ER volume placeholder.
     * <p>Français : volume urgences (factice).</p>
     */
    @GetMapping("/stats/urgences")
    public MetricResponse urgences(@RequestParam(required = false) Integer days) {
        return service.urgences(days);
    }

    /**
     * ALOS placeholder.
     * <p>Français : durée moyenne de séjour (factice).</p>
     */
    @GetMapping("/stats/average-length-of-stay")
    public MetricResponse averageLengthOfStay(@RequestParam(required = false) Integer days) {
        return service.averageLengthOfStay(days);
    }

    /**
     * Audit feed MVP marker.
     * <p>Français : stub d'événements d'audit.</p>
     */
    @GetMapping("/audit/events")
    public List<AuditEventResponse> auditEvents(@RequestParam(required = false) Integer days) {
        return service.auditEvents(days);
    }

    /**
     * Queued CSV export stub.
     * <p>Français : export d'activité (file fictive).</p>
     */
    @PostMapping("/exports/activity-report")
    public ExportResponse activityReport(@RequestParam(required = false) Integer days) {
        return service.activityReport(days);
    }

    /**
     * Queued occupancy export stub.
     * <p>Français : export occupation (file fictive).</p>
     */
    @PostMapping("/exports/occupancy-report")
    public ExportResponse occupancyReport(@RequestParam(required = false) Integer days) {
        return service.occupancyReport(days);
    }

    @GetMapping(value = "/exports/activity-report/download", produces = "text/csv")
    public ResponseEntity<String> downloadActivityReport(@RequestParam(required = false) Integer days) {
        String csv = service.activityReportCsv(days);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"activity-report.csv\"")
                .body(csv);
    }

    @GetMapping(value = "/exports/occupancy-report/download", produces = "text/csv")
    public ResponseEntity<String> downloadOccupancyReport(@RequestParam(required = false) Integer days) {
        String csv = service.occupancyReportCsv(days);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"occupancy-report.csv\"")
                .body(csv);
    }
}
