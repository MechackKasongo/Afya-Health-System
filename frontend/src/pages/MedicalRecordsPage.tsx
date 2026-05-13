import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PagePatientResponse } from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

export function MedicalRecordsPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState<PagePatientResponse | null>(null);
  const [queryInput, setQueryInput] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [sortBy, setSortBy] = useState<'patient' | 'dossierNumber' | 'birthDate'>('patient');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadPatients();
  }, [appliedQuery, sortBy, sortDir]);

  async function loadPatients() {
    setLoading(true);
    setError(null);
    try {
      const backendSortBy = sortBy === 'patient' ? 'lastName' : sortBy;
      const params = new URLSearchParams({ page: '0', size: String(LIST_FETCH_PAGE_SIZE), sortBy: backendSortBy, sortDir });
      if (appliedQuery.trim()) params.set('query', appliedQuery.trim());
      const { data } = await api.get<PagePatientResponse>(`/api/v1/patients?${params}`);
      setPage(data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les dossiers médicaux.'));
    } finally {
      setLoading(false);
    }
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedQuery(queryInput);
  }

  function openRecord(patientId: number) {
    setError(null);
    navigate(`/medical-records/${patientId}`);
  }

  return (
    <>
      {error && <div className="error-banner">{error}</div>}
      <div className="card" style={{ marginBottom: '1rem' }}>
        <form onSubmit={onSearchSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ flex: '1 1 320px', marginBottom: 0 }}>
            <label htmlFor="medical-records-search">Rechercher patient</label>
            <input
              id="medical-records-search"
              value={queryInput}
              onChange={(e) => setQueryInput(e.target.value)}
              placeholder="Nom, post-nom ou numéro dossier"
            />
          </div>
          <div className="field" style={{ flex: '0 0 220px', marginBottom: 0 }}>
            <label htmlFor="medical-records-sort-by">Trier par</label>
            <select
              id="medical-records-sort-by"
              value={sortBy}
              onChange={(e) => {
                setSortBy(e.target.value as 'patient' | 'dossierNumber' | 'birthDate');
              }}
            >
              <option value="patient">Patient</option>
              <option value="dossierNumber">Dossier patient</option>
              <option value="birthDate">Date</option>
            </select>
          </div>
          <div className="field" style={{ flex: '0 0 170px', marginBottom: 0 }}>
            <label htmlFor="medical-records-sort-dir">Ordre</label>
            <select
              id="medical-records-sort-dir"
              value={sortDir}
              onChange={(e) => {
                setSortDir(e.target.value as 'asc' | 'desc');
              }}
            >
              <option value="asc">↑ Ascendant</option>
              <option value="desc">↓ Descendant</option>
            </select>
          </div>
          <button type="submit" className="btn btn-primary">Rechercher</button>
        </form>
      </div>

      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}
      {!loading && page && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Dossier patient</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {page.content.length === 0 ? (
                  <tr>
                    <td colSpan={3} style={{ color: 'var(--muted)' }}>Aucun patient trouvé.</td>
                  </tr>
                ) : (
                  page.content.map((patient) => (
                    <tr key={patient.id}>
                      <td>#{patient.id} - {[patient.firstName, patient.lastName, patient.postName].filter(Boolean).join(' ')}</td>
                      <td>{patient.dossierNumber}</td>
                      <td>
                        <button
                          type="button"
                          onClick={() => openRecord(patient.id)}
                          style={{
                            border: 'none',
                            background: 'none',
                            padding: 0,
                            color: 'var(--accent)',
                            cursor: 'pointer',
                            font: 'inherit',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '0.35rem',
                          }}
                        >
                          <span aria-hidden="true">↗</span>
                          Ouvrir dossier
                        </button>
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
            itemLabelPlural="patient(s)"
          />
        </div>
      )}
    </>
  );
}
