import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  ConsultationEventResponse,
  ConsultationResponse,
  EventCreateRequest,
  PatientResponse,
} from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

export function ConsultationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const consultationId = Number(id);

  const [consultation, setConsultation] = useState<ConsultationResponse | null>(null);
  const [patientName, setPatientName] = useState<string>('');
  const [timeline, setTimeline] = useState<ConsultationEventResponse[]>([]);
  const [observation, setObservation] = useState('');
  const [diagnostic, setDiagnostic] = useState('');
  const [examOrder, setExamOrder] = useState('');
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(consultationId)) {
      setError('ID consultation invalide.');
      setLoading(false);
      return;
    }
    void loadData();
  }, [consultationId]);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [message]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const { data } = await api.get<ConsultationResponse>(`/api/v1/consultations/${consultationId}`);
      setConsultation(data);
      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${data.patientId}`);
      setPatientName(`${patientRes.data.firstName} ${patientRes.data.lastName}`.trim());
      const events = await api.get<ConsultationEventResponse[]>(`/api/v1/patients/${data.patientId}/clinical-timeline`);
      setTimeline(events.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger le dossier consultation.'));
    } finally {
      setLoading(false);
    }
  }

  async function postEvent(path: string, content: string, clear: () => void, successMessage: string) {
    if (!content.trim()) return setError('Le contenu est obligatoire.');
    const payload: EventCreateRequest = { content: content.trim() };
    setPending(true);
    setError(null);
    setMessage(null);
    try {
      await api.post(path, payload);
      clear();
      setMessage(successMessage);
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter cet evenement."));
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/consultations">Retour aux consultations</Link>
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

      {!loading && consultation && (
        <>
          <div
            style={{
              display: 'grid',
              gap: '1rem',
              gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
              alignItems: 'start',
            }}
          >
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Informations consultation</h3>
              <p style={{ margin: '0.35rem 0' }}><strong>Patient:</strong> #{consultation.patientId}{patientName ? ` - ${patientName}` : ''}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Admission:</strong> #{consultation.admissionId}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Medecin:</strong> {consultation.doctorName}</p>
              <p style={{ margin: '0.35rem 0' }}><strong>Motif:</strong> {consultation.reason || '-'}</p>
            </div>
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Observation</h3>
              <div className="field">
                <label htmlFor="observation">Contenu</label>
                <textarea id="observation" rows={4} value={observation} onChange={(e) => setObservation(e.target.value)} />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending}
                onClick={() => void postEvent(`/api/v1/consultations/${consultationId}/observations`, observation, () => setObservation(''), 'Observation ajoutee.')}
              >
                {pending ? 'Traitement...' : 'Ajouter observation'}
              </button>
            </div>
            <div className="card">
              <h3 style={{ marginTop: 0 }}>Diagnostic</h3>
              <div className="field">
                <label htmlFor="diagnostic">Contenu</label>
                <textarea id="diagnostic" rows={4} value={diagnostic} onChange={(e) => setDiagnostic(e.target.value)} />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending}
                onClick={() => void postEvent(`/api/v1/consultations/${consultationId}/diagnostics`, diagnostic, () => setDiagnostic(''), 'Diagnostic ajoute.')}
              >
                {pending ? 'Traitement...' : 'Ajouter diagnostic'}
              </button>
            </div>

            <div className="card">
              <h3 style={{ marginTop: 0 }}>Demande examen</h3>
              <div className="field">
                <label htmlFor="examOrder">Contenu</label>
                <textarea id="examOrder" rows={4} value={examOrder} onChange={(e) => setExamOrder(e.target.value)} />
              </div>
              <button
                type="button"
                className="btn btn-primary"
                disabled={pending}
                onClick={() => void postEvent(`/api/v1/consultations/${consultationId}/orders/exams`, examOrder, () => setExamOrder(''), 'Prescription examen ajoutee.')}
              >
                {pending ? 'Traitement...' : 'Ajouter demande'}
              </button>
            </div>
          </div>

          <div className="card table-wrap" style={{ marginTop: '1rem' }}>
            <h3 style={{ marginTop: 0 }}>Chronologie clinique patient</h3>
            <ScrollTableRegion>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Contenu</th>
                    <th>Consultation</th>
                  </tr>
                </thead>
                <tbody>
                  {timeline.length === 0 ? (
                    <tr><td colSpan={4} style={{ color: 'var(--muted)' }}>Aucun evenement.</td></tr>
                  ) : (
                    timeline.map((ev) => (
                      <tr key={ev.id}>
                        <td>{new Date(ev.createdAt).toLocaleString('fr-FR')}</td>
                        <td>{ev.type}</td>
                        <td>{ev.content}</td>
                        <td>#{ev.consultationId}</td>
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
