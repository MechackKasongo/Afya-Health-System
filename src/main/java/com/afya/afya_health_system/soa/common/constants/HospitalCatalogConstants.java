package com.afya.afya_health_system.soa.common.constants;

/**
 * Canonical names for hospital services that participate in security rules or integrations.
 * <p><strong>Urgences — important pour les administrateurs :</strong> le libellé exact du service dans le catalogue
 * (écran ou API des services hospitaliers) doit être la chaîne {@link #URGENCES_SERVICE_NAME}, avec cette orthographe et cette casse,
 * pour que les affectations utilisateurs correspondent au contrôle d’accès au module urgences (comparaison insensible à la casse ;
 * un libellé différent comme « Service urgences » ou « Urgence » ne correspond pas à cette entrée catalogue).</p>
 * <p>Français : noms canoniques des unités hospitalières pour les règles de sécurité ; alignement obligatoire avec le catalogue.</p>
 *
 * @see com.afya.afya_health_system.soa.hospitalservice.service.HospitalServiceManagementService#ensureDefaultCatalog()
 * @see com.afya.afya_health_system.soa.admission.service.UserHospitalScopeService
 */
public final class HospitalCatalogConstants {

    /**
     * Nom du service « pôle urgences » dans le catalogue {@code hospital_services}. À utiliser pour les affectations afin
     * d’ouvrir le périmètre urgences aux médecins / infirmiers à périmètre restreint.
     */
    public static final String URGENCES_SERVICE_NAME = "Urgences";

    private HospitalCatalogConstants() {
    }
}
