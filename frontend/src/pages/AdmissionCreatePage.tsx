import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  AdmissionCreateRequest,
  AdmissionResponse,
  BedSuggestionResponse,
  HospitalServiceResponse,
  PageHospitalServiceResponse,
} from '../api/types';

export function AdmissionCreatePage() {
  const navigate = useNavigate();
  const [patientId, setPatientId] = useState('');
  const [serviceName, setServiceName] = useState('');
  const [room, setRoom] = useState('');
  const [bed, setBed] = useState('');
  const [reason, setReason] = useState('');
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [bedSuggestionMessage, setBedSuggestionMessage] = useState<string | null>(null);

  useEffect(() => {
    void api
      .get<PageHospitalServiceResponse>('/api/v1/hospital-services?activeOnly=true&page=0&size=200')
      .then((res) => setServices(res.data.content))
      .catch(() => setServices([]));
  }, []);

  useEffect(() => {
    if (!serviceName.trim()) {
      setRoom('');
      setBed('');
      setBedSuggestionMessage(null);
      return;
    }
    let cancelled = false;
    setBedSuggestionMessage(null);
    void api
      .get<BedSuggestionResponse>('/api/v1/admissions/suggestions/bed', { params: { serviceName } })
      .then((res) => {
        if (cancelled) return;
        const data = res.data;
        if (data.available && data.room != null && data.bed != null) {
          setRoom(data.room);
          setBed(data.bed);
          setBedSuggestionMessage(null);
        } else {
          setRoom('');
          setBed('');
          setBedSuggestionMessage(data.message ?? 'Aucun lit automatique disponible pour ce service.');
        }
      })
      .catch(() => {
        if (cancelled) return;
        setRoom('');
        setBed('');
        setBedSuggestionMessage('Impossible de proposer un lit automatiquement.');
      });
    return () => {
      cancelled = true;
    };
  }, [serviceName]);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    const parsedPatientId = Number.parseInt(patientId, 10);
    if (!Number.isFinite(parsedPatientId) || parsedPatientId <= 0) {
      setError('ID patient invalide.');
      return;
    }
    if (!serviceName.trim()) {
      setError('Le service est obligatoire.');
      return;
    }

    const payload: AdmissionCreateRequest = {
      patientId: parsedPatientId,
      serviceName: serviceName.trim(),
      room: room.trim() || undefined,
      bed: bed.trim() || undefined,
      reason: reason.trim() || undefined,
    };

    setSubmitting(true);
    setError(null);
    try {
      const { data } = await api.post<AdmissionResponse>('/api/v1/admissions', payload);
      navigate(`/admissions/${data.id}`);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de creer l'admission."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Nouvelle admission</h1>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/admissions">Retour aux admissions</Link>
      </p>
      {error && <div className="error-banner">{error}</div>}

      <form onSubmit={onSubmit} className="card" style={{ display: 'grid', gap: '0.75rem', maxWidth: 760 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="patientId">ID patient *</label>
          <input
            id="patientId"
            value={patientId}
            onChange={(e) => setPatientId(e.target.value)}
            inputMode="numeric"
            placeholder="Ex. 12"
            required
          />
        </div>

        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="serviceName">Service *</label>
          <select id="serviceName" value={serviceName} onChange={(e) => setServiceName(e.target.value)} required>
            <option value="">Sélectionner un service</option>
            {services.map((s) => (
              <option key={s.id} value={s.name}>
                {s.name}
              </option>
            ))}
          </select>
        </div>

        {bedSuggestionMessage && (
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
            {bedSuggestionMessage}
          </div>
        )}

        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="room">Chambre</label>
            <input id="room" value={room} onChange={(e) => setRoom(e.target.value)} placeholder="Ex. 205" />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="bed">Lit</label>
            <input id="bed" value={bed} onChange={(e) => setBed(e.target.value)} placeholder="Ex. A" />
          </div>
        </div>
        <small style={{ color: 'var(--muted)' }}>
          Chambre et lit sont proposés automatiquement selon les lits libres ; vous pouvez les modifier.
        </small>

        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="reason">Motif</label>
          <textarea
            id="reason"
            rows={4}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Motif d'hospitalisation"
          />
        </div>

        <div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creation...' : "Creer l'admission"}
          </button>
        </div>
      </form>
    </>
  );
}
