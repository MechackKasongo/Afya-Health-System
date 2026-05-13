import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  AdmissionCreateRequest,
  AdmissionResponse,
  AdmissionStatus,
  BedSuggestionResponse,
  HospitalServiceResponse,
  PageAdmissionResponse,
  PageHospitalServiceResponse,
  PagePatientResponse,
  PatientResponse,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

const statusLabels: Record<AdmissionStatus, string> = {
  EN_COURS: 'En cours',
  TRANSFERE: 'Transfere',
  SORTI: 'Sorti',
  DECEDE: 'Decede',
};

export function AdmissionsPage() {
  const { user } = useAuth();
  const canManageAdmissions = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_RECEPTION');
  const canCreateAdmission =
    hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_RECEPTION') || hasRole(user, 'ROLE_MEDECIN');
  /** Aligné avec les routes dossier médical (admin / médecin / infirmier). */
  const canAccessMedicalRecord =
    hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_MEDECIN') || hasRole(user, 'ROLE_INFIRMIER');
  const [page, setPage] = useState<PageAdmissionResponse | null>(null);
  const [patientNamesById, setPatientNamesById] = useState<Record<number, string>>({});
  const [reloadKey, setReloadKey] = useState(0);
  const [patientIdInput, setPatientIdInput] = useState('');
  const [statusInput, setStatusInput] = useState('');
  const [appliedPatientId, setAppliedPatientId] = useState('');
  const [appliedStatus, setAppliedStatus] = useState('');
  const [sortBy, setSortBy] = useState<'id' | 'admissionDateTime' | 'patientId' | 'serviceName' | 'status'>('id');
  const [sortDir, setSortDir] = useState<'desc' | 'asc'>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);
  const [createError, setCreateError] = useState<string | null>(null);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [createPatientQuery, setCreatePatientQuery] = useState('');
  const [createPatientOptions, setCreatePatientOptions] = useState<PatientResponse[]>([]);
  const [createPatientLoading, setCreatePatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [createServiceName, setCreateServiceName] = useState('');
  const [createRoom, setCreateRoom] = useState('');
  const [createBed, setCreateBed] = useState('');
  const [createReason, setCreateReason] = useState('');
  const [createBedSuggestionMessage, setCreateBedSuggestionMessage] = useState<string | null>(null);

  const fetchBedSuggestion = useCallback(async (serviceName: string) => {
    setCreateBedSuggestionMessage(null);
    if (!serviceName.trim()) {
      setCreateRoom('');
      setCreateBed('');
      return;
    }
    try {
      const { data } = await api.get<BedSuggestionResponse>('/api/v1/admissions/suggestions/bed', {
        params: { serviceName },
      });
      if (data.available && data.room != null && data.bed != null) {
        setCreateRoom(data.room);
        setCreateBed(data.bed);
        setCreateBedSuggestionMessage(null);
      } else {
        setCreateRoom('');
        setCreateBed('');
        setCreateBedSuggestionMessage(data.message ?? 'Aucun lit automatique disponible pour ce service.');
      }
    } catch {
      setCreateRoom('');
      setCreateBed('');
      setCreateBedSuggestionMessage('Impossible de proposer un lit automatiquement.');
    }
  }, []);

  const hasFilters = appliedPatientId.trim() !== '' || appliedStatus.trim() !== '';

  const filterDescription = useMemo(() => {
    const parts: string[] = [];
    if (appliedPatientId.trim()) parts.push(`Patient #${appliedPatientId.trim()}`);
    if (appliedStatus.trim()) parts.push(`Statut ${statusLabels[appliedStatus as AdmissionStatus] ?? appliedStatus}`);
    return parts.length > 0 ? parts.join(' · ') : 'Aucun filtre applique';
  }, [appliedPatientId, appliedStatus]);

  useEffect(() => {
    void loadAdmissions();
  }, [sortBy, sortDir, reloadKey, appliedPatientId, appliedStatus]);

  useEffect(() => {
    if (!showCreateDrawer) return;
    void api
      .get<PageHospitalServiceResponse>('/api/v1/hospital-services?activeOnly=true&page=0&size=200')
      .then((res) => setServices(res.data.content))
      .catch(() => setServices([]));
  }, [showCreateDrawer]);

  useEffect(() => {
    if (!showCreateDrawer) return;
    const query = createPatientQuery.trim();
    if (query.length < 2) {
      setCreatePatientOptions([]);
      return;
    }
    setCreatePatientLoading(true);
    const timer = window.setTimeout(() => {
      const params = new URLSearchParams({ page: '0', size: '8', sortBy: 'id', sortDir: 'desc', query });
      void api
        .get<PagePatientResponse>(`/api/v1/patients?${params}`)
        .then((res) => setCreatePatientOptions(res.data.content))
        .catch(() => setCreatePatientOptions([]))
        .finally(() => setCreatePatientLoading(false));
    }, 250);
    return () => {
      window.clearTimeout(timer);
      setCreatePatientLoading(false);
    };
  }, [createPatientQuery, showCreateDrawer]);

  async function loadAdmissions() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        sortBy,
        sortDir,
        page: '0',
        size: String(LIST_FETCH_PAGE_SIZE),
      });
      if (appliedPatientId.trim()) params.set('patientId', appliedPatientId.trim());
      if (appliedStatus.trim()) params.set('status', appliedStatus.trim());
      const { data } = await api.get<PageAdmissionResponse>(`/api/v1/admissions?${params}`);
      setPage(data);
      await enrichPatientNames(data.content);
    } catch {
      setError('Impossible de charger les admissions.');
    } finally {
      setLoading(false);
    }
  }

  async function onFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedPatientId(patientIdInput);
    setAppliedStatus(statusInput);
  }

  async function enrichPatientNames(rows: AdmissionResponse[]) {
    const missingPatientIds = Array.from(new Set(rows.map((r) => r.patientId))).filter((id) => !patientNamesById[id]);
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

  function onResetFilters() {
    setPatientIdInput('');
    setStatusInput('');
    setAppliedPatientId('');
    setAppliedStatus('');
  }

  function resetCreateForm() {
    setCreatePatientQuery('');
    setCreatePatientOptions([]);
    setCreatePatientLoading(false);
    setSelectedPatient(null);
    setCreateServiceName('');
    setCreateRoom('');
    setCreateBed('');
    setCreateReason('');
    setCreateError(null);
    setCreateBedSuggestionMessage(null);
  }

  async function onCreateAdmissionSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedPatient) {
      setCreateError('Selectionnez un patient.');
      return;
    }
    if (!createServiceName.trim()) {
      setCreateError('Le service est obligatoire.');
      return;
    }

    const payload: AdmissionCreateRequest = {
      patientId: selectedPatient.id,
      serviceName: createServiceName.trim(),
      room: createRoom.trim() || undefined,
      bed: createBed.trim() || undefined,
      reason: createReason.trim() || undefined,
    };

    setSubmittingCreate(true);
    setCreateError(null);
    try {
      await api.post<AdmissionResponse>('/api/v1/admissions', payload);
      setShowCreateDrawer(false);
      resetCreateForm();
      setReloadKey((k) => k + 1);
    } catch (err) {
      setCreateError(getApiErrorMessage(err, "Impossible de creer l'admission."));
    } finally {
      setSubmittingCreate(false);
    }
  }

  return (
    <>
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between' }}>
          <form onSubmit={onFilterSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end', flex: '1 1 680px' }}>
            <div className="field" style={{ flex: '1 1 200px', marginBottom: 0 }}>
              <label htmlFor="admission-patient-id">ID patient</label>
              <input
                id="admission-patient-id"
                value={patientIdInput}
                onChange={(e) => setPatientIdInput(e.target.value)}
                placeholder="Ex. 12"
              />
            </div>
            <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
              <label htmlFor="admission-status">Statut</label>
              <select id="admission-status" value={statusInput} onChange={(e) => setStatusInput(e.target.value)}>
                <option value="">Tous les statuts</option>
                <option value="EN_COURS">En cours</option>
                <option value="TRANSFERE">Transfere</option>
                <option value="SORTI">Sorti</option>
                <option value="DECEDE">Decede</option>
              </select>
            </div>
            <div className="field" style={{ flex: '0 0 220px', marginBottom: 0 }}>
              <label htmlFor="admission-sort-by">Trier par</label>
              <select
                id="admission-sort-by"
                value={sortBy}
                onChange={(e) => {
                  setSortBy(e.target.value as typeof sortBy);
                }}
              >
                <option value="id">ID</option>
                <option value="admissionDateTime">Date admission</option>
                <option value="patientId">Patient</option>
                <option value="serviceName">Service</option>
                <option value="status">Statut</option>
              </select>
            </div>
            <div className="field" style={{ flex: '0 0 160px', marginBottom: 0 }}>
              <label htmlFor="admission-sort-dir">Ordre ↕</label>
              <select
                id="admission-sort-dir"
                value={sortDir}
                onChange={(e) => {
                  setSortDir(e.target.value as typeof sortDir);
                }}
              >
                <option value="desc">↓ Descendant</option>
                <option value="asc">↑ Ascendant</option>
              </select>
            </div>
            <button type="submit" className="btn btn-primary">
              Filtrer
            </button>
          </form>

          {canCreateAdmission && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => {
                setShowCreateDrawer(true);
                setCreateError(null);
              }}
            >
              Nouvelle admission
            </button>
          )}
        </div>
      </div>

      <p style={{ color: 'var(--muted)', marginTop: 0, marginBottom: '0.75rem' }}>{filterDescription}</p>
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
                  <th>Service</th>
                  <th>Chambre / lit</th>
                  <th>Motif</th>
                  <th>Admission</th>
                  <th>Sortie</th>
                  <th>Statut</th>
                  <th>Fiche</th>
                  <th>Dossier</th>
                </tr>
              </thead>
              <tbody>
                {page.content.length === 0 ? (
                  <tr>
                    <td colSpan={10} style={{ color: 'var(--muted)' }}>
                      Aucune admission trouvee.
                    </td>
                  </tr>
                ) : (
                  page.content.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>
                        #{item.patientId}
                        {patientNamesById[item.patientId] ? ` - ${patientNamesById[item.patientId]}` : ''}
                      </td>
                      <td>{item.serviceName}</td>
                      <td>{[item.room, item.bed].filter(Boolean).join(' / ') || '-'}</td>
                      <td>{item.reason}</td>
                      <td>{new Date(item.admissionDateTime).toLocaleString('fr-FR')}</td>
                      <td>{item.dischargeDateTime ? new Date(item.dischargeDateTime).toLocaleString('fr-FR') : '-'}</td>
                      <td>{statusLabels[item.status]}</td>
                      <td>
                        <Link to={`/admissions/${item.id}`}>Fiche</Link>
                      </td>
                      <td>
                        {canAccessMedicalRecord ? (
                          <Link to={`/medical-records/${item.patientId}`}>Dossier</Link>
                        ) : (
                          '—'
                        )}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter
            totalElements={page.totalElements}
            displayedCount={page.content.length}
            itemLabelPlural="admission(s)"
          />
        </div>
      )}

      {showCreateDrawer && (
        <>
          <div
            role="presentation"
            onClick={() => setShowCreateDrawer(false)}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'color-mix(in srgb, var(--accent) 14%, transparent)',
              zIndex: 39,
            }}
          />
          <aside
            style={{
              position: 'fixed',
              top: 0,
              right: 0,
              height: '100vh',
              width: 'min(50vw, 760px)',
              minWidth: '360px',
              background: 'var(--surface)',
              borderLeft: '1px solid var(--border)',
              zIndex: 40,
              overflowY: 'auto',
              padding: '1rem',
              boxShadow: '0 10px 40px rgba(2, 6, 23, 0.25)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <strong>Creer une admission</strong>
              <button type="button" className="btn btn-ghost" onClick={() => setShowCreateDrawer(false)}>
                Fermer
              </button>
            </div>
            {createError && <div className="error-banner">{createError}</div>}
            <form onSubmit={onCreateAdmissionSubmit} className="card" style={{ display: 'grid', gap: '0.75rem' }}>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="drawer-admission-patient-search">Patient *</label>
                <input
                  id="drawer-admission-patient-search"
                  value={createPatientQuery}
                  onChange={(e) => {
                    setCreatePatientQuery(e.target.value);
                    setSelectedPatient(null);
                    setCreateError(null);
                  }}
                  placeholder="Rechercher par nom ou dossier (min 2 caracteres)"
                  required
                />
                {selectedPatient ? (
                  <small style={{ color: 'var(--muted)' }}>
                    Selection: #{selectedPatient.id} - {selectedPatient.dossierNumber} - {selectedPatient.firstName} {selectedPatient.lastName}
                  </small>
                ) : createPatientLoading ? (
                  <small style={{ color: 'var(--muted)' }}>Recherche en cours...</small>
                ) : null}
                {!selectedPatient && createPatientOptions.length > 0 && (
                  <div style={{ marginTop: '0.4rem', border: '1px solid var(--border)', borderRadius: '0.5rem', maxHeight: 220, overflowY: 'auto' }}>
                    {createPatientOptions.map((patient) => (
                      <button
                        key={patient.id}
                        type="button"
                        className="btn btn-ghost"
                        style={{ width: '100%', justifyContent: 'flex-start', borderRadius: 0 }}
                        onClick={() => {
                          setSelectedPatient(patient);
                          setCreatePatientQuery(
                            `${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`
                          );
                          setCreatePatientOptions([]);
                          setCreateError(null);
                        }}
                      >
                        #{patient.id} - {patient.dossierNumber} - {patient.firstName} {patient.lastName}
                      </button>
                    ))}
                  </div>
                )}
              </div>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="drawer-admission-service-name">Service *</label>
                <select
                  id="drawer-admission-service-name"
                  value={createServiceName}
                  onChange={(e) => {
                    const v = e.target.value;
                    setCreateServiceName(v);
                    void fetchBedSuggestion(v);
                  }}
                  required
                >
                  <option value="">Selectionner un service</option>
                  {services.map((s) => (
                    <option key={s.id} value={s.name}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </div>
              {createBedSuggestionMessage && (
                <div
                  role="status"
                  style={{
                    padding: '0.5rem 0.65rem',
                    borderRadius: '0.45rem',
                    border: '1px solid rgba(214, 158, 46, 0.55)',
                    background: 'rgba(214, 158, 46, 0.12)',
                    color: 'var(--text)',
                    fontSize: '0.88rem',
                    lineHeight: 1.35,
                  }}
                >
                  {createBedSuggestionMessage}
                </div>
              )}
              <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-admission-room">Chambre</label>
                  <input id="drawer-admission-room" value={createRoom} onChange={(e) => setCreateRoom(e.target.value)} placeholder="Ex. 205" />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-admission-bed">Lit</label>
                  <input id="drawer-admission-bed" value={createBed} onChange={(e) => setCreateBed(e.target.value)} placeholder="Ex. A" />
                </div>
              </div>
              <small style={{ color: 'var(--muted)', marginTop: '-0.35rem' }}>
                Chambre et lit sont proposés automatiquement selon les lits libres ; vous pouvez les modifier.
              </small>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="drawer-admission-reason">Motif</label>
                <textarea
                  id="drawer-admission-reason"
                  rows={4}
                  value={createReason}
                  onChange={(e) => setCreateReason(e.target.value)}
                  placeholder="Motif d'hospitalisation"
                />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" className="btn btn-primary" disabled={submittingCreate}>
                  {submittingCreate ? 'Creation...' : "Creer l'admission"}
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => {
                    setShowCreateDrawer(false);
                    resetCreateForm();
                  }}
                >
                  Annuler
                </button>
              </div>
            </form>
          </aside>
        </>
      )}
    </>
  );
}
