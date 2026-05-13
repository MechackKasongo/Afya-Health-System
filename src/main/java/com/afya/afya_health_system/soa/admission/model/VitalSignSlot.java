package com.afya.afya_health_system.soa.admission.model;

/**
 * Time slot for charting (French inpatient charts often use morning/evening columns).
 * <p>Français : créneau de relevé (colonnes matin/soir sur les fiches de surveillance).</p>
 */
public enum VitalSignSlot {
    MATIN,
    SOIR,
    /** Whole-day reading without morning/evening split / relevé journalier sans distinction M/S */
    JOURNEE
}
