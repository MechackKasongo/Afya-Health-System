import { api } from '../api/client';
import type { AdmissionResponse, PatientResponse } from '../api/types';

/**
 * Loads the patient's {@code deceasedAt} for a given admission (two HTTP calls).
 * <p>Français : horodatage de décès du patient lié à l'admission.</p>
 */
export async function fetchPatientDeceasedAtForAdmission(admissionId: number): Promise<string | null> {
  const { data: admission } = await api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`);
  const { data: patient } = await api.get<PatientResponse>(`/api/v1/patients/${admission.patientId}`);
  return patient.deceasedAt ?? null;
}
