import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { MedicalRecordEntryResponse, MedicalRecordResponse, PatientResponse, TextUpdateRequest } from '../api/types';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

export function MedicalRecordDetailPage() {
  const { patientId } = useParams<{ patientId: string }>();
  const parsedPatientId = Number(patientId);

  const [record, setRecord] = useState<MedicalRecordResponse | null>(null);
  const [patientName, setPatientName] = useState<string>('');
  const [history, setHistory] = useState<MedicalRecordEntryResponse[]>([]);
  const [historySortDir, setHistorySortDir] = useState<'desc' | 'asc'>('desc');
  const [allergies, setAllergies] = useState('');
  const [antecedents, setAntecedents] = useState('');
  const [problemText, setProblemText] = useState('');
  const [documentText, setDocumentText] = useState('');
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(parsedPatientId)) {
      setError('ID patient invalide.');
      setLoading(false);
      return;
    }
    void loadData();
  }, [parsedPatientId]);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [message]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [recordRes, historyRes] = await Promise.all([
        api.get<MedicalRecordResponse>(`/api/v1/medical-records/${parsedPatientId}`),
        api.get<MedicalRecordEntryResponse[]>(`/api/v1/medical-records/${parsedPatientId}/history`),
      ]);
      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${parsedPatientId}`);
      setRecord(recordRes.data);
      setHistory(historyRes.data);
      setPatientName(`${patientRes.data.firstName} ${patientRes.data.lastName}`.trim());
      setAllergies(recordRes.data.allergies ?? '');
      setAntecedents(recordRes.data.antecedents ?? '');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger le dossier médical.'));
    } finally {
      setLoading(false);
    }
  }

  const historySorted = useMemo(() => {
    return [...history].sort((a, b) => {
      const aTime = new Date(a.createdAt).getTime();
      const bTime = new Date(b.createdAt).getTime();
      return historySortDir === 'asc' ? aTime - bTime : bTime - aTime;
    });
  }, [history, historySortDir]);

  async function updateText(path: string, content: string, successMessage: string) {
    const payload: TextUpdateRequest = { content: content.trim() };
    if (!payload.content) {
      setError('Le contenu ne peut pas être vide.');
      return;
    }

    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.put(path, payload);
      setMessage(successMessage);
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Mise à jour impossible.'));
    } finally {
      setPending(false);
    }
  }

  async function addEntry(path: string, content: string, clear: () => void, successMessage: string) {
    const payload: TextUpdateRequest = { content: content.trim() };
    if (!payload.content) {
      setError('Le contenu ne peut pas être vide.');
      return;
    }

    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post(path, payload);
      clear();
      setMessage(successMessage);
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Ajout d'entrée impossible."));
    } finally {
      setPending(false);
    }
  }

  const recordLocked = Boolean(record?.patientDeceasedAt);

  return (
    <>
      <h1 className="page-title">Dossier médical patient #{patientId}{patientName ? ` - ${patientName}` : ''}</h1>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/medical-records">Retour aux dossiers médicaux</Link>
      </p>

      {message && (
        <div
          style={{
            marginBottom: '0.6rem',
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.35rem',
            padding: '0.3rem 0.55rem',
            border: '1px solid rgba(61, 154, 237, 0.45)',
            borderRadius: '0.45rem',
            background: 'rgba(61, 154, 237, 0.08)',
            color: 'var(--text)',
            fontSize: '0.85rem',
            lineHeight: 1.2,
          }}
        >
          {message}
        </div>
      )}
      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && record && (
        <>
          <PatientDeceasedBanner
            deceasedAt={record.patientDeceasedAt}
            detail="Les allergies, antécédents et entrées ne peuvent pas être modifiées."
          />

          <div
            style={{
              display: 'grid',
              gap: '1rem',
              gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))',
              alignItems: 'start',
            }}
          >
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Allergies</h3>
              <div className="field">
                <label htmlFor="record-allergies">Contenu</label>
                <textarea
                  id="record-allergies"
                  rows={5}
                  value={allergies}
                  onChange={(e) => setAllergies(e.target.value)}
                  disabled={recordLocked}
                  readOnly={recordLocked}
                />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending || recordLocked}
                onClick={() => void updateText(`/api/v1/medical-records/${parsedPatientId}/allergies`, allergies, 'Allergies mises à jour.')}
              >
                {pending ? 'Traitement...' : 'Mettre à jour allergies'}
              </button>
            </div>

            <div className="card">
              <h3 style={{ marginTop: 0 }}>Antécédents</h3>
              <div className="field">
                <label htmlFor="record-antecedents">Contenu</label>
                <textarea
                  id="record-antecedents"
                  rows={5}
                  value={antecedents}
                  onChange={(e) => setAntecedents(e.target.value)}
                  disabled={recordLocked}
                  readOnly={recordLocked}
                />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending || recordLocked}
                onClick={() => void updateText(`/api/v1/medical-records/${parsedPatientId}/antecedents`, antecedents, 'Antécédents mis à jour.')}
              >
                {pending ? 'Traitement...' : 'Mettre à jour antécédents'}
              </button>
            </div>
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Ajouter un problème</h3>
              <div className="field">
                <label htmlFor="record-problem">Texte</label>
                <textarea
                  id="record-problem"
                  rows={4}
                  value={problemText}
                  onChange={(e) => setProblemText(e.target.value)}
                  disabled={recordLocked}
                  readOnly={recordLocked}
                />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending || recordLocked}
                onClick={() =>
                  void addEntry(
                    `/api/v1/medical-records/${parsedPatientId}/problems`,
                    problemText,
                    () => setProblemText(''),
                    'Problème ajouté.',
                  )
                }
              >
                {pending ? 'Traitement...' : 'Ajouter problème'}
              </button>
            </div>

            <div className="card">
              <h3 style={{ marginTop: 0 }}>Ajouter un document/note</h3>
              <div className="field">
                <label htmlFor="record-document">Texte</label>
                <textarea
                  id="record-document"
                  rows={4}
                  value={documentText}
                  onChange={(e) => setDocumentText(e.target.value)}
                  disabled={recordLocked}
                  readOnly={recordLocked}
                />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending || recordLocked}
                onClick={() =>
                  void addEntry(
                    `/api/v1/medical-records/${parsedPatientId}/documents`,
                    documentText,
                    () => setDocumentText(''),
                    'Document ajouté.',
                  )
                }
              >
                {pending ? 'Traitement...' : 'Ajouter document'}
              </button>
            </div>
          </div>

          <div className="card table-wrap" style={{ marginTop: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <h3 style={{ marginTop: 0, marginBottom: 0 }}>Historique</h3>
              <div className="field" style={{ marginBottom: 0, minWidth: 180 }}>
                <label htmlFor="history-sort-dir">Tri date</label>
                <select
                  id="history-sort-dir"
                  value={historySortDir}
                  onChange={(e) => {
                    setHistorySortDir(e.target.value as 'desc' | 'asc');
                  }}
                >
                  <option value="desc">↓ Descendant</option>
                  <option value="asc">↑ Ascendant</option>
                </select>
              </div>
            </div>
            <ScrollTableRegion>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Contenu</th>
                  </tr>
                </thead>
                <tbody>
                  {historySorted.length === 0 ? (
                    <tr>
                      <td colSpan={3} style={{ color: 'var(--muted)' }}>
                        Aucune entrée.
                      </td>
                    </tr>
                  ) : (
                    historySorted.map((entry) => (
                      <tr key={entry.id}>
                        <td>{new Date(entry.createdAt).toLocaleString('fr-FR')}</td>
                        <td>{entry.type}</td>
                        <td>{entry.content}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </ScrollTableRegion>
            <TableResultFooter
              totalElements={historySorted.length}
              displayedCount={historySorted.length}
              itemLabelPlural="entrée(s)"
            />
          </div>
        </>
      )}
    </>
  );
}
