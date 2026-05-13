package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.dto.VitalSignCreateRequest;
import com.afya.afya_health_system.soa.admission.dto.VitalSignResponse;
import com.afya.afya_health_system.soa.admission.model.VitalSignReading;
import com.afya.afya_health_system.soa.admission.repository.VitalSignReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Charted vital signs scoped to an admission (blood pressure, pulse, temperature, weight, diuresis, stools).
 * <p>Français : constantes vitales rattachées à une admission (TA, pouls, température, poids, diurèse, selles).</p>
 */
@Service
@Transactional
public class VitalSignService {

    private final VitalSignReadingRepository vitalSignReadingRepository;
    private final AdmissionEligibilityService admissionEligibilityService;

    public VitalSignService(
            VitalSignReadingRepository vitalSignReadingRepository,
            AdmissionEligibilityService admissionEligibilityService
    ) {
        this.vitalSignReadingRepository = vitalSignReadingRepository;
        this.admissionEligibilityService = admissionEligibilityService;
    }

    public VitalSignResponse create(Long admissionId, VitalSignCreateRequest request, String username) {
        admissionEligibilityService.requireOpenForClinicalWrite(admissionId, username);
        VitalSignReading row = new VitalSignReading();
        row.setAdmissionId(admissionId);
        row.setRecordedAt(request.recordedAt());
        row.setSlot(request.slot());
        row.setSystolicBp(request.systolicBp());
        row.setDiastolicBp(request.diastolicBp());
        row.setPulseBpm(request.pulseBpm());
        row.setTemperatureCelsius(request.temperatureCelsius());
        row.setWeightKg(request.weightKg());
        row.setDiuresisMl(request.diuresisMl());
        row.setStoolsNote(request.stoolsNote());
        return toResponse(vitalSignReadingRepository.save(row));
    }

    public List<VitalSignResponse> listByAdmission(Long admissionId, String username) {
        admissionEligibilityService.requireAdmissionAccessible(admissionId, username);
        return vitalSignReadingRepository.findByAdmissionIdOrderByRecordedAtDesc(admissionId).stream()
                .map(this::toResponse)
                .toList();
    }

    private VitalSignResponse toResponse(VitalSignReading r) {
        return new VitalSignResponse(
                r.getId(),
                r.getAdmissionId(),
                r.getRecordedAt(),
                r.getSlot(),
                r.getSystolicBp(),
                r.getDiastolicBp(),
                r.getPulseBpm(),
                r.getTemperatureCelsius(),
                r.getWeightKg(),
                r.getDiuresisMl(),
                r.getStoolsNote()
        );
    }
}
