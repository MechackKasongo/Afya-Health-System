import axios from 'axios';
import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  CloseRequest,
  OrientRequest,
  PatientResponse,
  TriageRequest,
  UrgenceResponse,
  UrgenceStatus,
  UrgenceTimelineEventResponse,
} from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

const statusLabels: Record<UrgenceStatus, string> = {
  EN_ATTENTE_TRIAGE: 'En attente triage',
  EN_COURS: 'En cours',
  ORIENTE: 'Orienté',
  CLOTURE: 'Clôturé',
};

const triageOptions = ['Rouge', 'Orange', 'Jaune', 'Vert', 'Bleu'];
const orientationOptions = [
  'Admission médecine',
  'Admission chirurgie',
  'Observation 24h',
  'Sortie domicile',
  'Transfert externe',
];
const OTHER_OPTION = '__OTHER__';

export function UrgenceDetailPage() {
  const { id } = useParams<{ id: string }>();
  const urgenceId = Number(id);

  const [urgence, setUrgence] = useState<UrgenceResponse | null>(null);
  const [patientName, setPatientName] = useState<string>('');
  const [timeline, setTimeline] = useState<UrgenceTimelineEventResponse[]>([]);
  const [triageLevel, setTriageLevel] = useState('');
  const [triageCustom, setTriageCustom] = useState('');
  const [triageDetails, setTriageDetails] = useState('');
  const [orientation, setOrientation] = useState('');
  const [orientationCustom, setOrientationCustom] = useState('');
  const [orientDetails, setOrientDetails] = useState('');
  const [closeDetails, setCloseDetails] = useState('');
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(urgenceId)) {
      setError("ID d'urgence invalide.");
      setLoading(false);
      return;
    }
    void loadData();
  }, [urgenceId]);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [message]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [urgenceRes, timelineRes] = await Promise.all([
        api.get<UrgenceResponse>(`/api/v1/urgences/${urgenceId}`),
        api.get<UrgenceTimelineEventResponse[]>(`/api/v1/urgences/${urgenceId}/timeline`),
      ]);
      setUrgence(urgenceRes.data);
      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${urgenceRes.data.patientId}`);
      setPatientName(`${patientRes.data.firstName} ${patientRes.data.lastName}`.trim());
      setTimeline(timelineRes.data);
    } catch (err) {
      if (axios.isAxiosError(err) && err.response?.status === 404) {
        setError(
          getApiErrorMessage(
            err,
            "Ce dossier d'urgence est introuvable ou vous n'y avez pas accès selon votre affectation hospitalière.",
          ),
        );
      } else {
        setError(getApiErrorMessage(err, "Impossible de charger le dossier d'urgence."));
      }
    } finally {
      setLoading(false);
    }
  }

  async function onTriage(e: React.FormEvent) {
    e.preventDefault();
    const selectedTriage = triageLevel === OTHER_OPTION ? triageCustom.trim() : triageLevel.trim();
    if (!selectedTriage) {
      setError('Le niveau de triage est obligatoire.');
      return;
    }
    const payload: TriageRequest = { triageLevel: selectedTriage, details: triageDetails.trim() || undefined };
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.put<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/triage`, payload);
      setTriageDetails('');
      setMessage('Triage enregistré.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de faire le triage.'));
    } finally {
      setPending(false);
    }
  }

  async function onOrient(e: React.FormEvent) {
    e.preventDefault();
    const selectedOrientation = orientation === OTHER_OPTION ? orientationCustom.trim() : orientation.trim();
    if (!selectedOrientation) {
      setError("L'orientation est obligatoire.");
      return;
    }
    const payload: OrientRequest = { orientation: selectedOrientation, details: orientDetails.trim() || undefined };
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.put<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/orient`, payload);
      setOrientDetails('');
      setMessage('Orientation enregistrée.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'orienter ce dossier."));
    } finally {
      setPending(false);
    }
  }

  async function onClose() {
    if (!window.confirm(`Confirmer la clôture du dossier urgence #${urgenceId} ?`)) return;
    const payload: CloseRequest = { details: closeDetails.trim() || undefined };
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.put<UrgenceResponse>(`/api/v1/urgences/${urgenceId}/close`, payload);
      setCloseDetails('');
      setMessage('Dossier clôturé.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de clôturer ce dossier.'));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/urgences">Retour aux urgences</Link>
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

      {!loading && urgence && (
        <>
          <div
            style={{
              display: 'grid',
              gap: '1rem',
              gridTemplateColumns: 'minmax(320px, 1.25fr) repeat(3, minmax(240px, 1fr))',
              alignItems: 'start',
            }}
          >
            <div className="card">
              <h3 style={{ marginTop: 0 }}>État du dossier</h3>
              <p style={{ margin: '0.35rem 0' }}><strong>Patient:</strong> #{urgence.patientId}{patientName ? ` - ${patientName}` : ''}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Statut:</strong> {statusLabels[urgence.status]}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Priorité:</strong> {urgence.priority}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Motif:</strong> {urgence.motif || '-'}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Triage:</strong> {urgence.triageLevel || '-'}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Orientation:</strong> {urgence.orientation || '-'}</p>
            </div>

            <form onSubmit={onTriage} className="card">
              <h3 style={{ marginTop: 0 }}>Triage</h3>
              <div className="field">
                <label htmlFor="triageLevel">Niveau *</label>
                <select id="triageLevel" value={triageLevel} onChange={(e) => setTriageLevel(e.target.value)} required>
                  <option value="">Sélectionner</option>
                  {triageOptions.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                  <option value={OTHER_OPTION}>Autre...</option>
                </select>
              </div>
              {triageLevel === OTHER_OPTION && (
                <div className="field">
                  <label htmlFor="triageCustom">Niveau personnalisé *</label>
                  <input
                    id="triageCustom"
                    value={triageCustom}
                    onChange={(e) => setTriageCustom(e.target.value)}
                    placeholder="Saisir un niveau"
                    required
                  />
                </div>
              )}
              <div className="field">
                <label htmlFor="triageDetails">Details</label>
                <input id="triageDetails" value={triageDetails} onChange={(e) => setTriageDetails(e.target.value)} />
              </div>
              <button type="submit" className="btn btn-primary" disabled={pending || urgence.status === 'CLOTURE'}>
                {pending ? 'Traitement...' : 'Valider triage'}
              </button>
            </form>

            <form onSubmit={onOrient} className="card">
              <h3 style={{ marginTop: 0 }}>Orientation</h3>
              <div className="field">
                <label htmlFor="orientation">Orientation *</label>
                <select id="orientation" value={orientation} onChange={(e) => setOrientation(e.target.value)} required>
                  <option value="">Sélectionner</option>
                  {orientationOptions.map((opt) => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                  <option value={OTHER_OPTION}>Autre...</option>
                </select>
              </div>
              {orientation === OTHER_OPTION && (
                <div className="field">
                  <label htmlFor="orientationCustom">Orientation personnalisée *</label>
                  <input
                    id="orientationCustom"
                    value={orientationCustom}
                    onChange={(e) => setOrientationCustom(e.target.value)}
                    placeholder="Saisir une orientation"
                    required
                  />
                </div>
              )}
              <div className="field">
                <label htmlFor="orientDetails">Details</label>
                <input id="orientDetails" value={orientDetails} onChange={(e) => setOrientDetails(e.target.value)} />
              </div>
              <button type="submit" className="btn btn-primary" disabled={pending || urgence.status === 'CLOTURE'}>
                {pending ? 'Traitement...' : 'Valider orientation'}
              </button>
            </form>

            <div className="card">
              <h3 style={{ marginTop: 0 }}>Clôture</h3>
              <div className="field">
                <label htmlFor="closeDetails">Détails</label>
                <input id="closeDetails" value={closeDetails} onChange={(e) => setCloseDetails(e.target.value)} />
              </div>
              <button type="button" className="btn btn-danger" disabled={pending || urgence.status === 'CLOTURE'} onClick={() => void onClose()}>
                {pending ? 'Traitement...' : 'Clôturer le dossier'}
              </button>
            </div>
          </div>

          <div className="card table-wrap" style={{ marginTop: '1rem' }}>
            <h3 style={{ marginTop: 0 }}>Timeline</h3>
            <ScrollTableRegion>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Détails</th>
                  </tr>
                </thead>
                <tbody>
                  {timeline.length === 0 ? (
                    <tr><td colSpan={3} style={{ color: 'var(--muted)' }}>Aucun événement.</td></tr>
                  ) : (
                    timeline.map((ev) => (
                      <tr key={ev.id}>
                        <td>{new Date(ev.createdAt).toLocaleString('fr-FR')}</td>
                        <td>{ev.type}</td>
                        <td>{ev.details || '-'}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </ScrollTableRegion>
            <TableResultFooter totalElements={timeline.length} displayedCount={timeline.length} itemLabelPlural="événement(s)" />
          </div>
        </>
      )}
    </>
  );
}
