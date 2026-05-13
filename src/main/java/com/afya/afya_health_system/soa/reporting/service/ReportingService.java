package com.afya.afya_health_system.soa.reporting.service;

import com.afya.afya_health_system.soa.admission.model.AdmissionStatus;
import com.afya.afya_health_system.soa.admission.repository.AdmissionRepository;
import com.afya.afya_health_system.soa.admission.repository.MovementRepository;
import com.afya.afya_health_system.soa.hospitalservice.repository.HospitalServiceRepository;
import com.afya.afya_health_system.soa.patient.model.Patient;
import com.afya.afya_health_system.soa.patient.repository.PatientRepository;
import com.afya.afya_health_system.soa.urgence.repository.UrgenceRepository;
import com.afya.afya_health_system.soa.urgence.repository.UrgenceTimelineEventRepository;
import com.afya.afya_health_system.soa.reporting.dto.ExportResponse;
import com.afya.afya_health_system.soa.reporting.dto.AuditEventResponse;
import com.afya.afya_health_system.soa.reporting.dto.MetricResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.Locale;

/**
 * Placeholder analytics and export hooks. Values are static until wired to real aggregates or jobs.
 * <p>Français : indicateurs et exports fictifs pour l'instant ; à brancher sur de vrais agrégats ou jobs.</p>
 */
@Service
public class ReportingService {
    private final AdmissionRepository admissionRepository;
    private final HospitalServiceRepository hospitalServiceRepository;
    private final MovementRepository movementRepository;
    private final UrgenceRepository urgenceRepository;
    private final UrgenceTimelineEventRepository urgenceTimelineEventRepository;
    private final PatientRepository patientRepository;

    public ReportingService(
            AdmissionRepository admissionRepository,
            HospitalServiceRepository hospitalServiceRepository,
            MovementRepository movementRepository,
            UrgenceRepository urgenceRepository,
            UrgenceTimelineEventRepository urgenceTimelineEventRepository,
            PatientRepository patientRepository
    ) {
        this.admissionRepository = admissionRepository;
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.movementRepository = movementRepository;
        this.urgenceRepository = urgenceRepository;
        this.urgenceTimelineEventRepository = urgenceTimelineEventRepository;
        this.patientRepository = patientRepository;
    }

    public MetricResponse occupancy() {
        List<Map<String, Object>> rows = occupancyByServiceRows();
        double totalOccupied = rows.stream().mapToDouble(r -> ((Number) r.get("occupiedBeds")).doubleValue()).sum();
        double totalCapacity = rows.stream().mapToDouble(r -> ((Number) r.get("bedCapacity")).doubleValue()).sum();
        double overallRate = totalCapacity <= 0 ? 0.0 : (totalOccupied * 100.0 / totalCapacity);
        return new MetricResponse(
                "occupancy_rate",
                Map.of(
                        "overallRatePercent", round2(overallRate),
                        "occupiedBeds", (int) totalOccupied,
                        "totalBeds", (int) totalCapacity
                ),
                Instant.now().toString()
        );
    }

    public MetricResponse occupancyByService() {
        return new MetricResponse("occupancy_by_service", occupancyByServiceRows(), Instant.now().toString());
    }

    public MetricResponse admissions(Integer days) {
        LocalDateTime cutoff = cutoff(days);
        long total = admissionRepository.findAll().stream()
                .filter(a -> cutoff == null || !a.getAdmissionDateTime().isBefore(cutoff))
                .count();
        return new MetricResponse("admissions_count", total, Instant.now().toString());
    }

    public MetricResponse urgences(Integer days) {
        LocalDateTime cutoff = cutoff(days);
        long total = urgenceRepository.findAll().stream()
                .filter(u -> cutoff == null || !u.getCreatedAt().isBefore(cutoff))
                .count();
        return new MetricResponse("urgences_count", total, Instant.now().toString());
    }

    public MetricResponse averageLengthOfStay(Integer days) {
        LocalDateTime cutoff = cutoff(days);
        var closedAdmissions = admissionRepository.findAll().stream()
                .filter(a -> a.getDischargeDateTime() != null)
                .filter(a -> cutoff == null || !a.getDischargeDateTime().isBefore(cutoff))
                .toList();

        if (closedAdmissions.isEmpty()) {
            return new MetricResponse(
                    "average_length_of_stay",
                    Map.of("days", 0.0, "closedAdmissions", 0),
                    Instant.now().toString()
            );
        }

        double avgDays = closedAdmissions.stream()
                .mapToLong(a -> Duration.between(a.getAdmissionDateTime(), a.getDischargeDateTime()).toHours())
                .average()
                .orElse(0.0) / 24.0;

        return new MetricResponse(
                "average_length_of_stay",
                Map.of(
                        "days", round2(avgDays),
                        "closedAdmissions", closedAdmissions.size()
                ),
                Instant.now().toString()
        );
    }

    /**
     * Aggregate the latest append-only audit events from:
     * - Urgences timeline (urgence_timeline_events)
     * - Admissions movements (admission_movements)
     *
     * Fr: agréger le fil d'audit (append-only) des passages urgences et des mouvements d'admission.
     */
    public List<AuditEventResponse> auditEvents(Integer days) {
        LocalDateTime cutoff = cutoff(days);
        var urgencyEvents = urgenceTimelineEventRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(e -> cutoff == null || !e.getCreatedAt().isBefore(cutoff))
                .limit(100)
                .toList();
        var admissionMovements = movementRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(m -> cutoff == null || !m.getCreatedAt().isBefore(cutoff))
                .limit(100)
                .toList();

        List<AuditEventResponse> mapped = new ArrayList<>(urgencyEvents.size() + admissionMovements.size());

        for (var e : urgencyEvents) {
            mapped.add(new AuditEventResponse(
                    e.getId(),
                    "URGENCES",
                    "URGENCE",
                    e.getUrgenceId(),
                    e.getType(),
                    e.getDetails(),
                    e.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString()
            ));
        }

        for (var m : admissionMovements) {
            String msg = m.getNote() == null ? "" : m.getNote();
            mapped.add(new AuditEventResponse(
                    m.getId(),
                    "ADMISSIONS",
                    "ADMISSION",
                    m.getAdmissionId(),
                    m.getType(),
                    (msg.isBlank() ? null : msg),
                    m.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString()
            ));
        }

        mapped.sort(Comparator.comparing((AuditEventResponse a) -> a.createdAt()).reversed());
        return mapped.size() > 25 ? mapped.subList(0, 25) : mapped;
    }
    public ExportResponse activityReport(Integer days) {
        String suffix = (days == null || days <= 0) ? "" : ("?days=" + days);
        return new ExportResponse("activity", "READY", "/api/v1/exports/activity-report/download" + suffix);
    }

    public ExportResponse occupancyReport(Integer days) {
        String suffix = (days == null || days <= 0) ? "" : ("?days=" + days);
        return new ExportResponse("occupancy", "READY", "/api/v1/exports/occupancy-report/download" + suffix);
    }

    public String activityReportCsv(Integer days) {
        LocalDateTime cutoff = cutoff(days);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder();
        csv.append("Section,Date,Type,Entite,EntiteId,PatientId,Dossier,PatientNomComplet,Service,Details\n");

        Map<Long, Patient> patientsById = patientRepository.findAll().stream()
                .collect(Collectors.toMap(Patient::getId, p -> p));

        admissionRepository.findAll(Sort.by(Sort.Direction.DESC, "admissionDateTime")).stream()
                .filter(a -> cutoff == null || !a.getAdmissionDateTime().isBefore(cutoff))
                .forEach(a -> {
                    Patient p = patientsById.get(a.getPatientId());
                    csv.append(csvRow(
                            "ADMISSIONS",
                            a.getAdmissionDateTime().format(fmt),
                            a.getStatus().name(),
                            "ADMISSION",
                            String.valueOf(a.getId()),
                            String.valueOf(a.getPatientId()),
                            p == null ? "" : nullSafe(p.getDossierNumber()),
                            fullName(p),
                            nullSafe(a.getServiceName()),
                            "Chambre=" + nullSafe(a.getRoom()) + "; Lit=" + nullSafe(a.getBed())
                    ));
                });

        urgenceRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(u -> cutoff == null || !u.getCreatedAt().isBefore(cutoff))
                .forEach(u -> {
                    Patient p = patientsById.get(u.getPatientId());
                    csv.append(csvRow(
                            "URGENCES",
                            u.getCreatedAt().format(fmt),
                            u.getStatus().name(),
                            "URGENCE",
                            String.valueOf(u.getId()),
                            String.valueOf(u.getPatientId()),
                            p == null ? "" : nullSafe(p.getDossierNumber()),
                            fullName(p),
                            "",
                            "Priorite=" + nullSafe(u.getPriority()) + "; Motif=" + nullSafe(u.getMotif())
                    ));
                });

        return csv.toString();
    }

    public String occupancyReportCsv(Integer days) {
        LocalDateTime cutoff = cutoff(days);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder();
        csv.append("Rapport,GenereLe,Service,Occupes,Capacite,TauxOccupationPourcent\n");

        String generatedAt = LocalDateTime.now().format(fmt);
        for (Map<String, Object> row : occupancyByServiceRows()) {
            String serviceName = String.valueOf(row.get("serviceName"));
            long occupied = ((Number) row.get("occupiedBeds")).longValue();
            int capacity = ((Number) row.get("bedCapacity")).intValue();
            double rate = ((Number) row.get("occupancyRatePercent")).doubleValue();
            csv.append(csvRow(
                    "OCCUPATION",
                    generatedAt,
                    serviceName,
                    String.valueOf(occupied),
                    String.valueOf(capacity),
                    String.format(Locale.US, "%.2f", rate)
            ));
        }

        MetricResponse overall = occupancy();
        @SuppressWarnings("unchecked")
        Map<String, Object> val = (Map<String, Object>) overall.value();
        csv.append(csvRow(
                "OCCUPATION_GLOBALE",
                generatedAt,
                "TOTAL",
                String.valueOf(val.get("occupiedBeds")),
                String.valueOf(val.get("totalBeds")),
                String.valueOf(val.get("overallRatePercent"))
        ));

        if (cutoff != null) {
            csv.append(csvRow(
                    "NOTE",
                    generatedAt,
                    "Periode",
                    "Filtre jours",
                    String.valueOf(days),
                    "Le taux d'occupation est calculé sur l'état courant des admissions/services."
            ));
        }

        return csv.toString();
    }

    private List<Map<String, Object>> occupancyByServiceRows() {
        Map<String, Long> occupiedByService = admissionRepository
                .countOccupiedBedsByService(Set.of(AdmissionStatus.EN_COURS, AdmissionStatus.TRANSFERE))
                .stream()
                .collect(Collectors.toMap(
                        row -> row.getServiceName().trim().toLowerCase(),
                        AdmissionRepository.ServiceOccupancyRow::getOccupiedBeds
                ));

        List<Map<String, Object>> rows = new ArrayList<>();
        hospitalServiceRepository.findByActiveTrueOrderByNameAsc().forEach(service -> {
            long occupied = occupiedByService.getOrDefault(service.getName().trim().toLowerCase(), 0L);
            int capacity = service.getBedCapacity() == null ? 0 : service.getBedCapacity();
            double rate = capacity <= 0 ? 0.0 : (occupied * 100.0 / capacity);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("serviceName", service.getName());
            row.put("occupiedBeds", occupied);
            row.put("bedCapacity", capacity);
            row.put("occupancyRatePercent", round2(rate));
            rows.add(row);
        });
        return rows;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private LocalDateTime cutoff(Integer days) {
        if (days == null || days <= 0) return null;
        return LocalDateTime.now().minusDays(days);
    }

    private String csvRow(String... columns) {
        return java.util.Arrays.stream(columns)
                .map(this::escapeCsv)
                .collect(Collectors.joining(",")) + "\n";
    }

    private String escapeCsv(String raw) {
        String safe = raw == null ? "" : raw;
        String escaped = safe.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String nullSafe(String v) {
        return v == null ? "" : v;
    }

    private String fullName(Patient p) {
        if (p == null) return "";
        return (nullSafe(p.getFirstName()) + " " + nullSafe(p.getPostName()) + " " + nullSafe(p.getLastName())).trim();
    }
}
