import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';
import type {
  AdmissionResponse,
  BedSuggestionResponse,
  DeathDeclarationRequest,
  DischargeRequest,
  HospitalServiceResponse,
  PageHospitalServiceResponse,
  PatientResponse,
  TransferRequest,
} from '../api/types';

export function AdmissionDetailPage() {
  const { user } = useAuth();
  const canManageAdmissions = hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_RECEPTION');
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);

  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [patientName, setPatientName] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [savingAction, setSavingAction] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [transferService, setTransferService] = useState('');
  const [transferRoom, setTransferRoom] = useState('');
  const [transferBed, setTransferBed] = useState('');
  const [transferNote, setTransferNote] = useState('');
  const [transferBedSuggestionMessage, setTransferBedSuggestionMessage] = useState<string | null>(null);
  const [dischargeNote, setDischargeNote] = useState('');
  const [deathNote, setDeathNote] = useState('');
  const [services, setServices] = useState<HospitalServiceResponse[]>([]);
  const [patientDeceasedAt, setPatientDeceasedAt] = useState<string | null>(null);

  const fetchTransferBedSuggestion = useCallback(async (serviceName: string) => {
    setTransferBedSuggestionMessage(null);
    if (!serviceName.trim()) {
      setTransferRoom('');
      setTransferBed('');
      return;
    }
    try {
      const { data } = await api.get<BedSuggestionResponse>('/api/v1/admissions/suggestions/bed', {
        params: { serviceName },
      });
      if (data.available && data.room != null && data.bed != null) {
        setTransferRoom(data.room);
        setTransferBed(data.bed);
        setTransferBedSuggestionMessage(null);
      } else {
        setTransferRoom('');
        setTransferBed('');
        setTransferBedSuggestionMessage(data.message ?? 'Aucun lit automatique disponible pour ce service.');
      }
    } catch {
      setTransferRoom('');
      setTransferBed('');
      setTransferBedSuggestionMessage('Impossible de proposer un lit automatiquement.');
    }
  }, []);

  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoading(false);
      return;
    }
    void loadData();
  }, [admissionId]);

  useEffect(() => {
    if (!actionMessage) return;
    const timer = window.setTimeout(() => setActionMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [actionMessage]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const { data: admissionData } = await api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`);
      setAdmission(admissionData);
      const patientRes = await api.get<PatientResponse>(`/api/v1/patients/${admissionData.patientId}`);
      setPatientName(`${patientRes.data.firstName} ${patientRes.data.lastName}`.trim());
      setPatientDeceasedAt(patientRes.data.deceasedAt ?? null);

      const servicesRes = await api.get<PageHospitalServiceResponse>('/api/v1/hospital-services?activeOnly=true&page=0&size=200');
      setServices(servicesRes.data.content);
    } catch {
      setError("Impossible de charger la fiche d'admission.");
    } finally {
      setLoading(false);
    }
  }

  async function onTransferSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!transferService.trim()) {
      setError('Le service de destination est obligatoire.');
      return;
    }
    if (!window.confirm(`Confirmer le transfert de l'admission #${admissionId} ?`)) return;

    const payload: TransferRequest = {
      toService: transferService.trim(),
      room: transferRoom.trim() || undefined,
      bed: transferBed.trim() || undefined,
      note: transferNote.trim() || undefined,
    };

    setSavingAction(true);
    setError(null);
    setActionMessage(null);
    try {
      await api.put<AdmissionResponse>(`/api/v1/admissions/${admissionId}/transfer`, payload);
      setTransferService('');
      setTransferRoom('');
      setTransferBed('');
      setTransferNote('');
      setTransferBedSuggestionMessage(null);
      setActionMessage('Transfert enregistre avec succes.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'executer le transfert."));
    } finally {
      setSavingAction(false);
    }
  }

  async function onDischarge() {
    if (!window.confirm(`Confirmer la sortie du patient pour l'admission #${admissionId} ?`)) return;
    const payload: DischargeRequest = { note: dischargeNote.trim() || undefined };
    setSavingAction(true);
    setError(null);
    setActionMessage(null);
    try {
      await api.put<AdmissionResponse>(`/api/v1/admissions/${admissionId}/discharge`, payload);
      setDischargeNote('');
      setActionMessage('Sortie enregistree avec succes.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer la sortie."));
    } finally {
      setSavingAction(false);
    }
  }

  async function onDeclareDeath() {
    if (!window.confirm(`Confirmer la declaration de deces pour l'admission #${admissionId} ?`)) return;
    const payload: DeathDeclarationRequest = { note: deathNote.trim() || undefined };
    setSavingAction(true);
    setError(null);
    setActionMessage(null);
    try {
      await api.put<AdmissionResponse>(`/api/v1/admissions/${admissionId}/declare-death`, payload);
      setDeathNote('');
      setActionMessage('Declaration de deces enregistree.');
      await loadData();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer la declaration de deces."));
    } finally {
      setSavingAction(false);
    }
  }

  /** Même logique que `ensureAdmissionOpen` côté API : transfert / sortie / décès tant que le séjour n'est pas SORTI ou DÉCÉDÉ (inclut TRANSFERE). */
  const stayOpenForAdministrativeActions =
    admission != null &&
    (admission.status === 'EN_COURS' || admission.status === 'TRANSFERE') &&
    patientDeceasedAt == null;

  return (
    <>
      <h1 className="page-title">Admission #{id}</h1>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/admissions">Retour a la liste des admissions</Link>
      </p>

      {actionMessage && (
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
          {actionMessage}
        </div>
      )}
      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && admission && (
        <>
          <PatientDeceasedBanner
            deceasedAt={patientDeceasedAt}
            detail="Transferts, sorties et déclaration de décès ne sont plus disponibles pour ce patient."
          />

          <div className="card" style={{ marginBottom: '1rem' }}>
            <h3 style={{ marginTop: 0 }}>Informations sejour</h3>
            <div className="grid-cards">
              <div className="tile-link" style={{ cursor: 'default' }}>
                <h3>Patient</h3>
                <p>
                  #{admission.patientId}
                  {patientName ? ` - ${patientName}` : ''}
                </p>
              </div>
              <div className="tile-link" style={{ cursor: 'default' }}>
                <h3>Service</h3>
                <p>{admission.serviceName}</p>
              </div>
              <div className="tile-link" style={{ cursor: 'default' }}>
                <h3>Chambre / lit</h3>
                <p>{[admission.room, admission.bed].filter(Boolean).join(' / ') || '-'}</p>
              </div>
              <div className="tile-link" style={{ cursor: 'default' }}>
                <h3>Statut</h3>
                <p>{admission.status}</p>
              </div>
            </div>
          </div>

          <div className="card" style={{ marginBottom: '1rem' }}>
            <h3 style={{ marginTop: 0 }}>Saisie clinique du séjour</h3>
            <p style={{ color: 'var(--muted)', marginTop: 0, marginBottom: '1rem', fontSize: '0.9rem' }}>
              Constantes, prescriptions et formulaire clinique pour cette admission.
            </p>
            <div className="grid-cards">
              <Link className="tile-link" to={`/admissions/${id}/vital-signs`}>
                <h3>Constantes</h3>
                <p>Relevés des signes vitaux et constantes</p>
              </Link>
              <Link className="tile-link" to={`/admissions/${id}/prescriptions`}>
                <h3>Prescriptions</h3>
                <p>Lignes de prescription et administrations</p>
              </Link>
              <Link className="tile-link" to={`/admissions/${id}/clinical-form`}>
                <h3>Formulaire clinique</h3>
                <p>Anamnèse, examen, conclusion</p>
              </Link>
            </div>
          </div>

          {canManageAdmissions && (
            <div className="card" style={{ marginTop: '1rem' }}>
              <h3 style={{ marginTop: 0 }}>Actions de sejour</h3>
              <p style={{ color: 'var(--muted)', marginTop: 0 }}>
                Utiliser ces actions pour transferer, sortir ou declarer un deces.
              </p>

              <div style={{ display: 'grid', gap: '1rem', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))' }}>
                <div className="card" style={{ margin: 0 }}>
                  <h4 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Transferer</h4>
                  <form
                    onSubmit={onTransferSubmit}
                    style={{
                      display: 'grid',
                      gap: '0.75rem',
                      gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
                    }}
                  >
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-service">Service destination *</label>
                      <select
                        id="transfer-service"
                        value={transferService}
                        onChange={(e) => {
                          const v = e.target.value;
                          setTransferService(v);
                          void fetchTransferBedSuggestion(v);
                        }}
                        required
                      >
                        <option value="">Sélectionner un service</option>
                        {services.map((s) => (
                          <option key={s.id} value={s.name}>
                            {s.name}
                          </option>
                        ))}
                      </select>
                    </div>
                    {transferBedSuggestionMessage && (
                      <div
                        role="status"
                        style={{
                          gridColumn: '1 / -1',
                          padding: '0.5rem 0.65rem',
                          borderRadius: '0.45rem',
                          border: '1px solid rgba(214, 158, 46, 0.55)',
                          background: 'rgba(214, 158, 46, 0.12)',
                          color: 'var(--text)',
                          fontSize: '0.88rem',
                          lineHeight: 1.35,
                        }}
                      >
                        {transferBedSuggestionMessage}
                      </div>
                    )}
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-room">Chambre</label>
                      <input
                        id="transfer-room"
                        value={transferRoom}
                        onChange={(e) => setTransferRoom(e.target.value)}
                        placeholder="Ex. 202"
                      />
                    </div>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-bed">Lit</label>
                      <input
                        id="transfer-bed"
                        value={transferBed}
                        onChange={(e) => setTransferBed(e.target.value)}
                        placeholder="Ex. B"
                      />
                    </div>
                    <small
                      style={{
                        gridColumn: '1 / -1',
                        color: 'var(--muted)',
                        marginTop: '-0.35rem',
                      }}
                    >
                      Chambre et lit sont proposés automatiquement selon les lits libres du service choisi ; vous pouvez les
                      modifier.
                    </small>
                    <div className="field" style={{ marginBottom: 0 }}>
                      <label htmlFor="transfer-note">Note transfert</label>
                      <input
                        id="transfer-note"
                        value={transferNote}
                        onChange={(e) => setTransferNote(e.target.value)}
                        placeholder="Motif du transfert"
                      />
                    </div>
                    <div style={{ gridColumn: '1 / -1' }}>
                      <button
                        type="submit"
                        className="btn btn-primary"
                        disabled={savingAction || !stayOpenForAdministrativeActions}
                      >
                        {savingAction ? 'Traitement...' : 'Transferer'}
                      </button>
                    </div>
                  </form>
                </div>

                <div className="card" style={{ margin: 0 }}>
                  <h4 style={{ marginTop: 0, marginBottom: '0.75rem' }}>Sortie</h4>
                  <div className="field" style={{ marginBottom: '0.75rem' }}>
                    <label htmlFor="discharge-note">Note de sortie</label>
                    <input
                      id="discharge-note"
                      value={dischargeNote}
                      onChange={(e) => setDischargeNote(e.target.value)}
                      placeholder="Optionnel"
                    />
                  </div>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => void onDischarge()}
                    disabled={savingAction || !stayOpenForAdministrativeActions}
                  >
                    {savingAction ? 'Traitement...' : 'Enregistrer sortie'}
                  </button>
                  <hr style={{ margin: '1rem 0', border: 0, borderTop: '1px solid var(--border)' }} />
                  <div className="field" style={{ marginBottom: '0.75rem' }}>
                    <label htmlFor="death-note">Note declaration deces</label>
                    <input
                      id="death-note"
                      value={deathNote}
                      onChange={(e) => setDeathNote(e.target.value)}
                      placeholder="Optionnel"
                    />
                  </div>
                  <button
                    type="button"
                    className="btn btn-danger"
                    onClick={() => void onDeclareDeath()}
                    disabled={savingAction || !stayOpenForAdministrativeActions}
                  >
                    {savingAction ? 'Traitement...' : 'Declarer deces'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </>
  );
}
