import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { RoleRoute } from './components/RoleRoute';
import { AdmissionsPage } from './pages/AdmissionsPage';
import { AdmissionCreatePage } from './pages/AdmissionCreatePage';
import { AdmissionDetailPage } from './pages/AdmissionDetailPage';
import { AdmissionVitalSignsPage } from './pages/AdmissionVitalSignsPage';
import { AdmissionPrescriptionsPage } from './pages/AdmissionPrescriptionsPage';
import { ConsultationDetailPage } from './pages/ConsultationDetailPage';
import { ConsultationsPage } from './pages/ConsultationsPage';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { AdmissionClinicalFormPage } from './pages/AdmissionClinicalFormPage';
import { MedicalRecordDetailPage } from './pages/MedicalRecordDetailPage';
import { MedicalRecordsPage } from './pages/MedicalRecordsPage';
import { MedicationAdministrationsPage } from './pages/MedicationAdministrationsPage';
import { PatientCreatePage } from './pages/PatientCreatePage';
import { PatientDetailPage } from './pages/PatientDetailPage';
import { PatientsPage } from './pages/PatientsPage';
import { ReportingPage } from './pages/ReportingPage';
import { UrgenceDetailPage } from './pages/UrgenceDetailPage';
import { UrgencesPage } from './pages/UrgencesPage';
import { UsersPage } from './pages/UsersPage';
import { HospitalServicesPage } from './pages/HospitalServicesPage';
import { SettingsPage } from './pages/SettingsPage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute />}>
            <Route element={<Layout />}>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_RECEPTION']} />}>
                <Route path="/patients" element={<PatientsPage />} />
                <Route path="/patients/new" element={<PatientCreatePage />} />
              </Route>
              <Route path="/patients/:id" element={<PatientDetailPage />} />
              <Route path="/admissions" element={<AdmissionsPage />} />
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_RECEPTION', 'ROLE_MEDECIN']} />}>
                <Route path="/admissions/new" element={<AdmissionCreatePage />} />
              </Route>
              <Route path="/admissions/:id" element={<AdmissionDetailPage />} />
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route path="/admissions/:id/vital-signs" element={<AdmissionVitalSignsPage />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN']} />}>
                <Route path="/admissions/:id/prescriptions" element={<AdmissionPrescriptionsPage />} />
                <Route path="/admissions/:id/clinical-form" element={<AdmissionClinicalFormPage />} />
                <Route
                  path="/admissions/:admissionId/prescriptions/:lineId/administrations"
                  element={<MedicationAdministrationsPage />}
                />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route path="/urgences" element={<UrgencesPage />} />
                <Route path="/urgences/:id" element={<UrgenceDetailPage />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route path="/consultations" element={<ConsultationsPage />} />
                <Route path="/consultations/:id" element={<ConsultationDetailPage />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_RECEPTION']} />}>
                <Route path="/hospital-services" element={<HospitalServicesPage />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN']} />}>
                <Route path="/reporting" element={<ReportingPage />} />
                <Route path="/users" element={<UsersPage />} />
              </Route>
              <Route element={<RoleRoute allowed={['ROLE_ADMIN', 'ROLE_MEDECIN', 'ROLE_INFIRMIER']} />}>
                <Route path="/medical-records" element={<MedicalRecordsPage />} />
                <Route path="/medical-records/:patientId" element={<MedicalRecordDetailPage />} />
              </Route>
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
