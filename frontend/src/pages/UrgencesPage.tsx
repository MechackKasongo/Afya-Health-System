import axios from 'axios';
import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PagePatientResponse, PageUrgenceResponse, PatientResponse, UrgenceCreateRequest, UrgenceResponse, UrgenceStatus } from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

const statusLabels: Record<UrgenceStatus, string> = {
  EN_ATTENTE_TRIAGE: 'En attente triage',
  EN_COURS: 'En cours',
  ORIENTE: 'Orienté',
  CLOTURE: 'Clôturé',
};

const priorityOptions = ['P1', 'P2', 'P3', 'P4', 'P5'];

export function UrgencesPage() {
  const [page, setPage] = useState<PageUrgenceResponse | null>(null);
  const [patientNamesById, setPatientNamesById] = useState<Record<number, string>>({});
  const [reloadKey, setReloadKey] = useState(0);
  const [statusInput, setStatusInput] = useState('');
  const [priorityInput, setPriorityInput] = useState('');
  const [appliedStatus, setAppliedStatus] = useState('');
  const [appliedPriority, setAppliedPriority] = useState('');
  const [sortBy, setSortBy] = useState<'id' | 'createdAt' | 'patientId' | 'priority' | 'status'>('id');
  const [sortDir, setSortDir] = useState<'desc' | 'asc'>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [createPatientQuery, setCreatePatientQuery] = useState('');
  const [createPatientOptions, setCreatePatientOptions] = useState<PatientResponse[]>([]);
  const [createPatientLoading, setCreatePatientLoading] = useState(false);
  const [selectedPatient, setSelectedPatient] = useState<PatientResponse | null>(null);
  const [motif, setMotif] = useState('');
  const [priority, setPriority] = useState('P2');
  const [submitting, setSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const hasFilters = appliedStatus.trim() !== '' || appliedPriority.trim() !== '';

  const filterDescription = useMemo(() => {
    const parts: string[] = [];
    if (appliedStatus.trim()) parts.push(`Statut ${statusLabels[appliedStatus as UrgenceStatus] ?? appliedStatus}`);
    if (appliedPriority.trim()) parts.push(`Priorité ${appliedPriority}`);
    return parts.length > 0 ? parts.join(' · ') : 'Aucun filtre appliqué';
  }, [appliedStatus, appliedPriority]);

  useEffect(() => {
    void loadUrgences();
  }, [sortBy, sortDir, reloadKey, appliedStatus, appliedPriority]);

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

  async function loadUrgences() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ sortBy, sortDir, page: '0', size: String(LIST_FETCH_PAGE_SIZE) });
      if (appliedStatus.trim()) params.set('status', appliedStatus.trim());
      if (appliedPriority.trim()) params.set('priority', appliedPriority.trim());
      const { data } = await api.get<PageUrgenceResponse>(`/api/v1/urgences?${params}`);
      setPage(data);
      await enrichPatientNames(data.content);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les urgences.'));
    } finally {
      setLoading(false);
    }
  }

  async function onFilterSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedStatus(statusInput);
    setAppliedPriority(priorityInput);
  }

  async function enrichPatientNames(rows: UrgenceResponse[]) {
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
    setStatusInput('');
    setPriorityInput('');
    setAppliedStatus('');
    setAppliedPriority('');
  }

  function resetCreateForm() {
    setCreatePatientQuery('');
    setCreatePatientOptions([]);
    setCreatePatientLoading(false);
    setSelectedPatient(null);
    setMotif('');
    setPriority('P2');
    setCreateError(null);
  }

  async function onCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!selectedPatient) {
      setCreateError('Selectionnez un patient.');
      return;
    }
    if (!priority.trim()) {
      setCreateError('La priorité est obligatoire.');
      return;
    }

    const payload: UrgenceCreateRequest = {
      patientId: selectedPatient.id,
      motif: motif.trim() || undefined,
      priority: priority.trim(),
    };

    setSubmitting(true);
    setCreateError(null);
    try {
      await api.post<UrgenceResponse>('/api/v1/urgences', payload);
      setShowCreateDrawer(false);
      resetCreateForm();
      setReloadKey((k) => k + 1);
    } catch (err) {
      const fallback =
        axios.isAxiosError(err) && err.response?.status === 409
          ? "Accès aux urgences non autorisé pour votre affectation."
          : "Impossible de créer l'urgence.";
      setCreateError(getApiErrorMessage(err, fallback));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Urgences</h1>

      {page?.scopeRestricted && (
        <div
          className="card"
          style={{
            marginBottom: '1rem',
            borderLeft: '4px solid var(--accent)',
            paddingLeft: '1rem',
          }}
        >
          <strong>Périmètre hospitalier</strong>
          <p style={{ margin: '0.35rem 0 0', color: 'var(--muted)', fontSize: '0.95rem', lineHeight: 1.45 }}>
            Votre affectation ne comprend pas le service « Urgences » : vous ne pouvez pas voir ni créer de passages aux
            urgences tant que cette unité ne vous est pas assignée.
          </p>
        </div>
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between' }}>
          <form onSubmit={onFilterSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end', flex: '1 1 680px' }}>
          <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
            <label htmlFor="urg-status">Statut</label>
            <select id="urg-status" value={statusInput} onChange={(e) => setStatusInput(e.target.value)}>
              <option value="">Tous</option>
              <option value="EN_ATTENTE_TRIAGE">En attente triage</option>
              <option value="EN_COURS">En cours</option>
              <option value="ORIENTE">Orienté</option>
              <option value="CLOTURE">Clôturé</option>
            </select>
          </div>
          <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
            <label htmlFor="urg-priority-filter">Priorité</label>
            <select id="urg-priority-filter" value={priorityInput} onChange={(e) => setPriorityInput(e.target.value)}>
              <option value="">Toutes</option>
              {priorityOptions.map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
          <div className="field" style={{ flex: '0 0 220px', marginBottom: 0 }}>
            <label htmlFor="urg-sort-by">Trier par</label>
            <select id="urg-sort-by" value={sortBy} onChange={(e) => { setSortBy(e.target.value as typeof sortBy); }}>
              <option value="id">ID</option>
              <option value="createdAt">Date création</option>
              <option value="patientId">Patient</option>
              <option value="priority">Priorité</option>
              <option value="status">Statut</option>
            </select>
          </div>
          <div className="field" style={{ flex: '0 0 160px', marginBottom: 0 }}>
            <label htmlFor="urg-sort-dir">Ordre ↕</label>
            <select id="urg-sort-dir" value={sortDir} onChange={(e) => { setSortDir(e.target.value as typeof sortDir); }}>
              <option value="desc">↓ Descendant</option>
              <option value="asc">↑ Ascendant</option>
            </select>
          </div>
          <button type="submit" className="btn btn-primary">Filtrer</button>
          <button type="button" className="btn btn-ghost" onClick={onResetFilters} disabled={!hasFilters}>
            Réinitialiser
          </button>
          </form>
          {!page?.scopeRestricted && (
            <button type="button" className="btn btn-primary" onClick={() => setShowCreateDrawer(true)}>
              Nouvelle urgence
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
                  <th>Motif</th>
                  <th>Priorité</th>
                  <th>Triage</th>
                  <th>Orientation</th>
                  <th>Statut</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {page.content.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ color: 'var(--muted)' }}>
                      {hasFilters
                        ? 'Aucune urgence ne correspond aux filtres sélectionnés.'
                        : 'Aucune urgence à afficher pour le moment.'}
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
                      <td>{item.motif || '-'}</td>
                      <td>{item.priority}</td>
                      <td>{item.triageLevel || '-'}</td>
                      <td>{item.orientation || '-'}</td>
                      <td>{statusLabels[item.status]}</td>
                      <td>
                        <Link to={`/urgences/${item.id}`}>Dossier</Link>
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
            itemLabelPlural="urgence(s)"
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
              <strong>Creer une urgence</strong>
              <button type="button" className="btn btn-ghost" onClick={() => setShowCreateDrawer(false)}>
                Fermer
              </button>
            </div>
            {createError && <div className="error-banner">{createError}</div>}
            <form onSubmit={onCreateSubmit} className="card" style={{ display: 'grid', gap: '0.75rem' }}>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="drawer-urg-patient-search">Patient *</label>
                <input
                  id="drawer-urg-patient-search"
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
                          setCreatePatientQuery(`${patient.firstName} ${patient.lastName} (${patient.dossierNumber})`);
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
                <label htmlFor="drawer-urg-priority">Priorité *</label>
                <select id="drawer-urg-priority" value={priority} onChange={(e) => setPriority(e.target.value)}>
                  {priorityOptions.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="drawer-urg-motif">Motif</label>
                <input
                  id="drawer-urg-motif"
                  value={motif}
                  onChange={(e) => setMotif(e.target.value)}
                  placeholder="Douleur thoracique, dyspnee..."
                />
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" className="btn btn-primary" disabled={submitting}>
                  {submitting ? 'Création...' : 'Créer'}
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
