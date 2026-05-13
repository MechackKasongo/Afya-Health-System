package com.afya.afya_health_system.soa.admission.service;

import com.afya.afya_health_system.soa.admission.dto.BedSuggestionResponse;
import com.afya.afya_health_system.soa.admission.model.AdmissionStatus;
import com.afya.afya_health_system.soa.admission.repository.AdmissionRepository;
import com.afya.afya_health_system.soa.common.exception.BadRequestException;
import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import com.afya.afya_health_system.soa.hospitalservice.repository.HospitalServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BedAllocationService {

    private static final List<AdmissionStatus> OCCUPYING_STATUSES =
            List.of(AdmissionStatus.EN_COURS, AdmissionStatus.TRANSFERE);

    private final HospitalServiceRepository hospitalServiceRepository;
    private final AdmissionRepository admissionRepository;

    public BedAllocationService(
            HospitalServiceRepository hospitalServiceRepository,
            AdmissionRepository admissionRepository
    ) {
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.admissionRepository = admissionRepository;
    }

    /**
     * Propose le prochain couple chambre/lit cohérent avec la capacité configurée et les séjours
     * {@code EN_COURS} / {@code TRANSFERE} (même logique que le reporting d'occupation).
     */
    @Transactional(readOnly = true)
    public BedSuggestionResponse suggest(String serviceNameRaw) {
        if (serviceNameRaw == null || serviceNameRaw.isBlank()) {
            throw new BadRequestException("Le nom du service est requis.");
        }
        String serviceName = serviceNameRaw.trim();
        HospitalService svc = hospitalServiceRepository.findByNameIgnoreCase(serviceName)
                .filter(HospitalService::isActive)
                .orElse(null);
        if (svc == null) {
            return new BedSuggestionResponse(false, null, null, 0, 0,
                    "Service hospitalier introuvable ou inactif.");
        }
        long occupied = admissionRepository.countByServiceNameIgnoreCaseAndStatusIn(serviceName, OCCUPYING_STATUSES);
        int capacity = svc.getBedCapacity() == null ? 0 : svc.getBedCapacity();
        if (capacity <= 0) {
            return new BedSuggestionResponse(false, null, null, occupied, 0,
                    "Capacité de lits non configurée (0) pour ce service. Saisissez chambre et lit manuellement.");
        }
        if (occupied >= capacity) {
            return new BedSuggestionResponse(false, null, null, occupied, capacity,
                    "Tous les lits de ce service sont occupés. Libérez un lit, augmentez la capacité dans « Services hôpitaux », ou attribuez chambre et lit manuellement.");
        }
        int slot = (int) occupied + 1;
        int roomIndex = ((slot - 1) / 2) + 1;
        char bedLetter = ((slot - 1) % 2 == 0) ? 'A' : 'B';
        String room = String.valueOf(100 + roomIndex);
        String bed = String.valueOf(bedLetter);
        return new BedSuggestionResponse(true, room, bed, occupied, capacity, null);
    }
}
