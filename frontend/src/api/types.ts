export interface MeResponse {
  id: number;
  username: string;
  fullName: string;
  roles: string[];
  hospitalServiceIds: number[];
  /** Libellés des services affectés (triés), pour l’en-tête. */
  hospitalServiceNames: string[];
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  /** Profil aligné sur GET /auth/me (affectations hospitalières incluses). */
  me: MeResponse;
}

export interface PatientResponse {
  id: number;
  firstName: string;
  lastName: string;
  dossierNumber: string;
  birthDate: string;
  sex: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  postName: string | null;
  employer: string | null;
  employeeId: string | null;
  profession: string | null;
  spouseName: string | null;
  spouseProfession: string | null;
  /** Set when death was recorded (e.g. via declare-death on an admission). */
  deceasedAt: string | null;
}

export interface PatientCreateRequest {
  firstName: string;
  lastName: string;
  dossierNumber?: string;
  birthDate: string;
  sex: string;
  phone?: string;
  email?: string;
  address?: string;
  postName?: string;
  employer?: string;
  employeeId?: string;
  profession?: string;
  spouseName?: string;
  spouseProfession?: string;
}

/** PUT /api/v1/patients/{id} ; le n° de dossier reste non modifiable via ce corps. */
export interface PatientUpdateRequest {
  firstName: string;
  lastName: string;
  birthDate: string;
  sex: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  postName: string;
  employer: string;
  employeeId: string;
  profession: string;
  spouseName: string;
  spouseProfession: string;
}

export interface PagePatientResponse {
  content: PatientResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type AdmissionStatus = 'EN_COURS' | 'TRANSFERE' | 'SORTI' | 'DECEDE';

export interface AdmissionResponse {
  id: number;
  patientId: number;
  serviceName: string;
  room: string | null;
  bed: string | null;
  reason: string;
  admissionDateTime: string;
  dischargeDateTime: string | null;
  status: AdmissionStatus;
}

export interface AdmissionCreateRequest {
  patientId: number;
  serviceName: string;
  room?: string;
  bed?: string;
  reason?: string;
}

/** GET /api/v1/admissions/suggestions/bed?serviceName= */
export interface BedSuggestionResponse {
  available: boolean;
  room: string | null;
  bed: string | null;
  occupiedBeds: number;
  bedCapacity: number;
  message: string | null;
}

export type VitalSignSlot = 'MATIN' | 'SOIR' | 'JOURNEE';

export interface VitalSignResponse {
  id: number;
  admissionId: number;
  recordedAt: string;
  slot: VitalSignSlot | null;
  systolicBp: number | null;
  diastolicBp: number | null;
  pulseBpm: number | null;
  temperatureCelsius: number | null;
  weightKg: number | null;
  diuresisMl: number | null;
  stoolsNote: string | null;
}

export interface VitalSignCreateRequest {
  recordedAt: string;
  slot?: VitalSignSlot;
  systolicBp?: number;
  diastolicBp?: number;
  pulseBpm?: number;
  temperatureCelsius?: number;
  weightKg?: number;
  diuresisMl?: number;
  stoolsNote?: string;
}

export interface PrescriptionLineResponse {
  id: number;
  admissionId: number;
  medicationName: string;
  dosageText: string | null;
  frequencyText: string | null;
  instructionsText: string | null;
  prescriberName: string | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
  createdAt: string;
}

export interface PrescriptionLineCreateRequest {
  medicationName: string;
  dosageText?: string;
  frequencyText?: string;
  instructionsText?: string;
  prescriberName?: string;
  startDate: string;
  endDate?: string;
}

export interface PrescriptionLineUpdateRequest extends PrescriptionLineCreateRequest {
  active: boolean;
}

export interface MedicationAdministrationResponse {
  id: number;
  prescriptionLineId: number;
  administrationDate: string;
  slot: VitalSignSlot;
  administered: boolean;
}

export interface MedicationAdministrationCreateRequest {
  administrationDate: string;
  slot: VitalSignSlot;
  administered: boolean;
}

export interface AdmissionClinicalFormResponse {
  id: number;
  admissionId: number;
  antecedentsText: string | null;
  anamnesisText: string | null;
  physicalExamPulmonaryText: string | null;
  physicalExamCardiacText: string | null;
  physicalExamAbdominalText: string | null;
  physicalExamNeurologicalText: string | null;
  physicalExamMiscText: string | null;
  paraclinicalText: string | null;
  conclusionText: string | null;
}

export interface AdmissionClinicalFormUpsertRequest {
  antecedentsText?: string;
  anamnesisText?: string;
  physicalExamPulmonaryText?: string;
  physicalExamCardiacText?: string;
  physicalExamAbdominalText?: string;
  physicalExamNeurologicalText?: string;
  physicalExamMiscText?: string;
  paraclinicalText?: string;
  conclusionText?: string;
}

export interface TransferRequest {
  toService: string;
  room?: string;
  bed?: string;
  note?: string;
}

export interface DischargeRequest {
  note?: string;
}

export interface DeathDeclarationRequest {
  note?: string;
}

export type UrgenceStatus = 'EN_ATTENTE_TRIAGE' | 'EN_COURS' | 'ORIENTE' | 'CLOTURE';

export interface UrgenceResponse {
  id: number;
  patientId: number;
  motif: string | null;
  priority: string;
  triageLevel: string | null;
  orientation: string | null;
  status: UrgenceStatus;
  createdAt: string;
  closedAt: string | null;
}

export interface UrgenceCreateRequest {
  patientId: number;
  motif?: string;
  priority: string;
}

export interface TriageRequest {
  triageLevel: string;
  details?: string;
}

export interface OrientRequest {
  orientation: string;
  details?: string;
}

export interface CloseRequest {
  details?: string;
}

export interface UrgenceTimelineEventResponse {
  id: number;
  urgenceId: number;
  type: string;
  details: string | null;
  createdAt: string;
}

export interface ConsultationResponse {
  id: number;
  patientId: number;
  admissionId: number;
  doctorName: string;
  reason: string | null;
  consultationDateTime: string;
}

export interface ConsultationCreateRequest {
  patientId: number;
  admissionId: number;
  doctorName: string;
  reason?: string;
}

export interface ConsultationEventResponse {
  id: number;
  consultationId: number;
  patientId: number;
  type: string;
  content: string;
  createdAt: string;
}

export interface EventCreateRequest {
  content: string;
}

export interface MetricResponse {
  metric: string;
  value: unknown;
  generatedAt: string;
}

export interface ExportResponse {
  reportType: string;
  status: string;
  downloadUrl: string;
}

export interface MedicalRecordResponse {
  id: number;
  patientId: number;
  allergies: string | null;
  antecedents: string | null;
  createdAt: string;
  updatedAt: string;
  /** Mirror of patient.deceasedAt for read-only UI without an extra patient fetch. */
  patientDeceasedAt: string | null;
}

export interface MedicalRecordEntryResponse {
  id: number;
  patientId: number;
  type: string;
  content: string;
  createdAt: string;
}

export interface RecordCreateRequest {
  patientId: number;
}

export interface TextUpdateRequest {
  content: string;
}

export interface CredentialsLogPreviewResponse {
  content: string;
  empty: boolean;
  truncated: boolean;
  totalBytes: number;
  lineCount: number;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string | null;
  fullName: string;
  roles: string[];
  active: boolean;
  hospitalServiceIds: number[];
  /** Present only once in the JSON response right after account creation. */
  generatedPassword?: string | null;
}

export interface PageUserResponse {
  content: UserResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PasswordPreviewRequest {
  firstName: string;
  lastName: string;
  postName?: string;
  generatedPasswordLength?: number;
  variation?: number;
}

export interface PasswordPreviewResponse {
  password: string;
}

export interface UserCreateRequest {
  username?: string;
  fullName?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  postName?: string;
  password?: string;
  generatedPasswordLength?: number;
  /** Doit correspondre à la dernière proposition affichée (régénérations). */
  passwordVariation?: number;
  role: string;
  hospitalServiceIds?: number[];
}

export interface UserUpdateRequest {
  fullName: string;
  email?: string;
  role: string;
  password?: string;
  /** Omettre pour ne pas modifier l'affectation aux services. */
  hospitalServiceIds?: number[];
}

export interface RoleOptionResponse {
  id: number;
  name: string;
  label: string;
}

export interface HospitalServiceResponse {
  id: number;
  name: string;
  bedCapacity: number;
  active: boolean;
}

export interface HospitalServiceRequest {
  name: string;
  bedCapacity: number;
}

export interface PageAdmissionResponse {
  content: AdmissionResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PageConsultationResponse {
  content: ConsultationResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

/** Liste urgences : {@code scopeRestricted} si la liste est vide car l'affectation exclut le service « Urgences ». */
export interface PageUrgenceResponse {
  scopeRestricted: boolean;
  content: UrgenceResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PageHospitalServiceResponse {
  content: HospitalServiceResponse[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
