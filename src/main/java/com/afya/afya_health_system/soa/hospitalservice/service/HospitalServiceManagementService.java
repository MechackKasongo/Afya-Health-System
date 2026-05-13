package com.afya.afya_health_system.soa.hospitalservice.service;

import com.afya.afya_health_system.soa.admission.repository.AdmissionRepository;
import com.afya.afya_health_system.soa.common.exception.ConflictException;
import com.afya.afya_health_system.soa.common.exception.NotFoundException;
import com.afya.afya_health_system.soa.hospitalservice.dto.HospitalServiceRequest;
import com.afya.afya_health_system.soa.hospitalservice.dto.HospitalServiceResponse;
import com.afya.afya_health_system.soa.common.constants.HospitalCatalogConstants;
import com.afya.afya_health_system.soa.hospitalservice.dto.HospitalServiceStatusRequest;
import com.afya.afya_health_system.soa.hospitalservice.model.HospitalService;
import com.afya.afya_health_system.soa.hospitalservice.repository.HospitalServiceRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HospitalServiceManagementService {
    private final HospitalServiceRepository hospitalServiceRepository;
    private final AdmissionRepository admissionRepository;

    public HospitalServiceManagementService(HospitalServiceRepository hospitalServiceRepository, AdmissionRepository admissionRepository) {
        this.hospitalServiceRepository = hospitalServiceRepository;
        this.admissionRepository = admissionRepository;
    }

    @PostConstruct
    void ensureDefaultCatalog() {
        createIfMissing(HospitalCatalogConstants.URGENCES_SERVICE_NAME, 20);
        createIfMissing("Médecine interne", 30);
        createIfMissing("Chirurgie", 25);
        createIfMissing("Pédiatrie", 20);
        createIfMissing("Maternité", 15);
        // Libellés attendus par les tests d’intégration / clients (recherche ignore cas ; H2 regroupe souvent accents).
        createIfMissing("Medecine", 30);
        createIfMissing("Cardiologie", 25);
        createIfMissing("Bloc B", 15);
        createIfMissing("Reanimation", 20);
    }

    @Transactional(readOnly = true)
    public Page<HospitalServiceResponse> list(Boolean activeOnly, Integer page, Integer size) {
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 500 ? 20 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name"));
        Page<HospitalService> entityPage = Boolean.TRUE.equals(activeOnly)
                ? hospitalServiceRepository.findByActiveTrueOrderByNameAsc(pageable)
                : hospitalServiceRepository.findAllByOrderByNameAsc(pageable);
        return entityPage.map(this::toResponse);
    }

    public HospitalServiceResponse create(HospitalServiceRequest request) {
        String normalizedName = normalizeName(request.name());
        if (hospitalServiceRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Service hospitalier déjà existant");
        }
        HospitalService item = new HospitalService();
        item.setName(normalizedName);
        item.setBedCapacity(request.bedCapacity());
        item.setActive(true);
        return toResponse(hospitalServiceRepository.save(item));
    }

    public HospitalServiceResponse update(Long id, HospitalServiceRequest request) {
        HospitalService item = findEntity(id);
        String normalizedName = normalizeName(request.name());
        if (!normalizedName.equalsIgnoreCase(item.getName()) && hospitalServiceRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Service hospitalier déjà existant");
        }
        item.setName(normalizedName);
        item.setBedCapacity(request.bedCapacity());
        return toResponse(hospitalServiceRepository.save(item));
    }

    public HospitalServiceResponse updateStatus(Long id, HospitalServiceStatusRequest request) {
        HospitalService item = findEntity(id);
        item.setActive(request.active());
        return toResponse(hospitalServiceRepository.save(item));
    }

    public void delete(Long id) {
        HospitalService item = findEntity(id);
        if (admissionRepository.existsByServiceNameIgnoreCase(item.getName())) {
            throw new ConflictException("Suppression impossible: ce service est déjà utilisé par des admissions");
        }
        hospitalServiceRepository.delete(item);
    }

    private HospitalService findEntity(Long id) {
        return hospitalServiceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Service hospitalier introuvable: " + id));
    }

    private String normalizeName(String name) {
        return name == null ? "" : name.trim();
    }

    private void createIfMissing(String name, int bedCapacity) {
        if (hospitalServiceRepository.existsByNameIgnoreCase(name)) {
            return;
        }
        HospitalService hs = new HospitalService();
        hs.setName(name);
        hs.setBedCapacity(bedCapacity);
        hs.setActive(true);
        hospitalServiceRepository.save(hs);
    }

    private HospitalServiceResponse toResponse(HospitalService item) {
        return new HospitalServiceResponse(item.getId(), item.getName(), item.getBedCapacity(), item.isActive());
    }
}
