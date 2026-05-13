import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import type {
  AdmissionResponse,
  ConsultationCreateRequest,
  ConsultationResponse,
  PageAdmissionResponse,
  PageConsultationResponse,
  PagePatientResponse,
  PatientResponse,
} from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

export function ConsultationsPage() {
  const { user } = useAuth();
  const connectedDoctorName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';
  const [page, setPage] = useState<PageConsultationResponse | null>(null);
  const [patientIdFilter, setPatientIdFilter] = useState('');
  const [admissionIdFilter, setAdmissionIdFilter] = useState('');
  const [appliedPatientIdFilter, setAppliedPatientIdFilter] = useState('');
  const [appliedAdmissionIdFilter, setAppliedAdmissionIdFilter] = useState('');
  const [sortBy, setSortBy] = useState<'id' | 'consultationDateTime' | 'patientId' | 'admissionId' | 'doctorName'>('id');
  const [sortDir, setSortDir] = useState<'desc' | 'asc'>('desc');
  const [patientQuery, setPatientQuery] = useState('');
  const [patientOptions, setPatientOptions] = useState<PatientResponse[]>([]);
  const [patientLoading, setPatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [admissionOptions, setAdmissionOptions] = useState<AdmissionResponse[]>([]);
  const [admissionLoading, setAdmissionLoading] = useState(false);
  const [admissionId, setAdmissionId] = useState('');
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [patientNamesById, setPatientNamesById] = useState<Record<number, string>>({});

  useEffect(() => {
    void loadConsultations();
  }, [sortBy, sortDir, appliedPatientIdFilter, appliedAdmissionIdFilter]);

  useEffect(() => {
    const query = patientQuery.trim();
    if (query.length < 2) {
      setPatientOptions([]);
      return;
    }
    setPatientLoading(true);
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams({ page: '0', size: '8', sortBy: 'id', sortDir: 'desc', query });
      void api
        .get<PagePatientResponse>(`/api/v1/patients?${params}`)
        .then((res) => setPatientOptions(res.data.content))
        .catch(() => setPatientOptions([]))
        .finally(() => setPatientLoading(false));
    }, 250);
    return () => {
      window.clearTimeout(timer);
      setPatientLoading(false);
    };
  }, [patientQuery]);

  useEffect(() => {
    if (!selectedPatient) {
      setAdmissionOptions([]);
      setAdmissionId('');
      return;
    }
    setAdmissionLoading(true);
    const params = new URLSearchParams({ patientId: String(selectedPatient.id), sortBy: 'admissionDateTime', sortDir: 'desc', page: '0', size: '100' });
    void api
      .get<PageAdmissionResponse>(`/api/v1/admissions?${params}`)
      .then((res) => setAdmissionOptions(res.data.content))
      .catch(() => setAdmissionOptions([]))
      .finally(() => setAdmissionLoading(false));
  }, [selectedPatient]);

  async function loadConsultations() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ sortBy, sortDir, page: '0', size: String(LIST_FETCH_PAGE_SIZE) });
      if (appliedPatientIdFilter.trim()) params.set('patientId', appliedPatientIdFilter.trim());
      if (appliedAdmissionIdFilter.trim()) params.set('admissionId', appliedAdmissionIdFilter.trim());
      const { data } = await api.get<PageConsultationResponse>(`/api/v1/consultations?${params}`);
      setPage(data);
      await enrichPatientNames(data.content);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les consultations.'));
    } finally {
      setLoading(false);
    }
  }

  async function onFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedPatientIdFilter(patientIdFilter);
    setAppliedAdmissionIdFilter(admissionIdFilter);
  }

  async function enrichPatientNames(consultations: ConsultationResponse[]) {
    const missingPatientIds = Array.from(new Set(consultations.map((c) => c.patientId))).filter(
      (id) => !patientNamesById[id],
    );
    if (missingPatientIds.length === 0) return;
    const results = await Promise.all(
      missingPatientIds.map((id) =>
        api
          .get<PatientResponse>(`/api/v1/patients/${id}`)
          .then((res) => ({ id, name: `${res.data.firstName} ${res.data.lastName}`.trim() || `Patient #${id}` }))
          .catch(() => ({ id, name: `Patient #${id}` })),
      ),
    );
    setPatientNamesById((prev) => {
      const next = { ...prev };
      results.forEach((r) => {
        next[r.id] = r.name;
      });
      return next;
    });
  }

  async function onCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    const parsedAdmissionId = Number.parseInt(admissionId, 10);
    if (!selectedPatient) return setError('Sélectionnez un patient.');
    if (!Number.isFinite(parsedAdmissionId) || parsedAdmissionId <= 0) return setError('ID admission invalide.');

    const payload: ConsultationCreateRequest = {
      patientId: selectedPatient.id,
      admissionId: parsedAdmissionId,
      doctorName: connectedDoctorName,
      reason: reason.trim() || undefined,
    };

    setSubmitting(true);
    setError(null);
    try {
      await api.post<ConsultationResponse>('/api/v1/consultations', payload);
      setPatientQuery('');
      setPatientOptions([]);
      setSelectedPatient(null);
      setAdmissionOptions([]);
      setAdmissionId('');
      setReason('');
      await loadConsultations();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer la consultation.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginTop: 0 }}>Nouvelle consultation</h3>
        <form onSubmit={onCreateSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ flex: '2 1 280px', marginBottom: 0 }}>
            <label htmlFor="consult-patient-search">Patient *</label>
            <input
              id="consult-patient-search"
              value={patientQuery}
              onChange={(e) => {
                setPatientQuery(e.target.value);
                setSelectedPatient(null);
                setError(null);
              }}
              placeholder="Rechercher par nom ou dossier (min 2 caractères)"
              required
            />
            {selectedPatient ? (
              <small style={{ color: 'var(--muted)' }}>
                Sélection: #{selectedPatient.id} - {selectedPatient.dossierNumber} - {selectedPatient.firstName} {selectedPatient.lastName}
              </small>
            ) : patientLoading ? (
              <small style={{ color: 'var(--muted)' }}>Recherche en cours...</small>
            ) : null}
            {!selectedPatient && patientOptions.length > 0 && (
              <div style={{ marginTop: '0.4rem', border: '1px solid var(--border)', borderRadius: '0.5rem', maxHeight: 220, overflowY: 'auto' }}>
                {patientOptions.map((patient) => (
                  <button
                    key={patient.id}
                    type="button"
                    className="btn btn-ghost"
                    style={{ width: '100%', justifyContent: 'flex-start', borderRadius: 0 }}
                    onClick={() => {
                      setSelectedPatient(patient);
                      setPatientQuery(`${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`);
                      setPatientOptions([]);
                      setError(null);
                    }}
                  >
                    #{patient.id} - {patient.dossierNumber} - {patient.firstName} {patient.lastName}
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
            <label htmlFor="consult-admission-id">Admission *</label>
            <select
              id="consult-admission-id"
              value={admissionId}
              onChange={(e) => setAdmissionId(e.target.value)}
              disabled={!selectedPatient || admissionLoading}
              required
            >
              <option value="">{admissionLoading ? 'Chargement...' : 'Sélectionner une admission'}</option>
              {admissionOptions.map((adm) => (
                <option key={adm.id} value={adm.id}>
                  #{adm.id} - {adm.serviceName} ({adm.status})
                </option>
              ))}
            </select>
          </div>
          <div className="field" style={{ flex: '2 1 220px', marginBottom: 0 }}>
            <label htmlFor="consult-doctor">Médecin</label>
            <input id="consult-doctor" value={connectedDoctorName} readOnly />
          </div>
          <div className="field" style={{ flex: '2 1 260px', marginBottom: 0 }}>
            <label htmlFor="consult-reason">Motif</label>
            <input id="consult-reason" value={reason} onChange={(e) => setReason(e.target.value)} />
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Création...' : 'Créer'}
          </button>
        </form>
      </div>

      <div className="card" style={{ marginBottom: '1rem' }}>
        <form onSubmit={onFilterSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ flex: '1 1 200px', marginBottom: 0 }}>
            <label htmlFor="filter-patient">Filtre patient</label>
            <input id="filter-patient" value={patientIdFilter} onChange={(e) => setPatientIdFilter(e.target.value)} inputMode="numeric" />
          </div>
          <div className="field" style={{ flex: '1 1 200px', marginBottom: 0 }}>
            <label htmlFor="filter-admission">Filtre admission</label>
            <input id="filter-admission" value={admissionIdFilter} onChange={(e) => setAdmissionIdFilter(e.target.value)} inputMode="numeric" />
          </div>
          <div className="field" style={{ flex: '0 0 220px', marginBottom: 0 }}>
            <label htmlFor="consult-sort-by">Trier par</label>
            <select id="consult-sort-by" value={sortBy} onChange={(e) => { setSortBy(e.target.value as typeof sortBy); }}>
              <option value="id">ID</option>
              <option value="consultationDateTime">Date consultation</option>
              <option value="patientId">Patient</option>
              <option value="admissionId">Admission</option>
              <option value="doctorName">Médecin</option>
            </select>
          </div>
          <div className="field" style={{ flex: '0 0 160px', marginBottom: 0 }}>
            <label htmlFor="consult-sort-dir">Ordre ↕</label>
            <select id="consult-sort-dir" value={sortDir} onChange={(e) => { setSortDir(e.target.value as typeof sortDir); }}>
              <option value="desc">↓ Descendant</option>
              <option value="asc">↑ Ascendant</option>
            </select>
          </div>
          <button type="submit" className="btn btn-primary">Filtrer</button>
          <button type="button" className="btn btn-ghost" onClick={() => { setPatientIdFilter(''); setAdmissionIdFilter(''); setAppliedPatientIdFilter(''); setAppliedAdmissionIdFilter(''); }}>
            Réinitialiser
          </button>
        </form>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && page && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Patient</th>
                  <th>Admission</th>
                  <th>Médecin</th>
                  <th>Motif</th>
                  <th>Date</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {page.content.length === 0 ? (
                  <tr><td colSpan={7} style={{ color: 'var(--muted)' }}>Aucune consultation.</td></tr>
                ) : (
                  page.content.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>
                        #{item.patientId}
                        {patientNamesById[item.patientId] ? ` - ${patientNamesById[item.patientId]}` : ''}
                      </td>
                      <td>#{item.admissionId}</td>
                      <td>{item.doctorName}</td>
                      <td>{item.reason || '-'}</td>
                      <td>{new Date(item.consultationDateTime).toLocaleString('fr-FR')}</td>
                      <td><Link to={`/consultations/${item.id}`}>Dossier</Link></td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={page.totalElements}
            displayedCount={page.content.length}
            itemLabelPlural="consultation(s)"
          />
        </div>
      )}
    </>
  );
}
