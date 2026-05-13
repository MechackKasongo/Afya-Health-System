import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';
import { fetchPatientDeceasedAtForAdmission } from '../utils/admissionPatientDeceased';
import type {
  AdmissionClinicalFormResponse,
  AdmissionClinicalFormUpsertRequest,
  AdmissionResponse,
} from '../api/types';

type ClinicalFormState = {
  antecedentsText: string;
  anamnesisText: string;
  physicalExamPulmonaryText: string;
  physicalExamCardiacText: string;
  physicalExamAbdominalText: string;
  physicalExamNeurologicalText: string;
  physicalExamMiscText: string;
  paraclinicalText: string;
  conclusionText: string;
};

const emptyForm: ClinicalFormState = {
  antecedentsText: '',
  anamnesisText: '',
  physicalExamPulmonaryText: '',
  physicalExamCardiacText: '',
  physicalExamAbdominalText: '',
  physicalExamNeurologicalText: '',
  physicalExamMiscText: '',
  paraclinicalText: '',
  conclusionText: '',
};

function fromResponse(data: AdmissionClinicalFormResponse): ClinicalFormState {
  return {
    antecedentsText: data.antecedentsText ?? '',
    anamnesisText: data.anamnesisText ?? '',
    physicalExamPulmonaryText: data.physicalExamPulmonaryText ?? '',
    physicalExamCardiacText: data.physicalExamCardiacText ?? '',
    physicalExamAbdominalText: data.physicalExamAbdominalText ?? '',
    physicalExamNeurologicalText: data.physicalExamNeurologicalText ?? '',
    physicalExamMiscText: data.physicalExamMiscText ?? '',
    paraclinicalText: data.paraclinicalText ?? '',
    conclusionText: data.conclusionText ?? '',
  };
}

function toPayload(form: ClinicalFormState): AdmissionClinicalFormUpsertRequest {
  return {
    antecedentsText: form.antecedentsText.trim() || undefined,
    anamnesisText: form.anamnesisText.trim() || undefined,
    physicalExamPulmonaryText: form.physicalExamPulmonaryText.trim() || undefined,
    physicalExamCardiacText: form.physicalExamCardiacText.trim() || undefined,
    physicalExamAbdominalText: form.physicalExamAbdominalText.trim() || undefined,
    physicalExamNeurologicalText: form.physicalExamNeurologicalText.trim() || undefined,
    physicalExamMiscText: form.physicalExamMiscText.trim() || undefined,
    paraclinicalText: form.paraclinicalText.trim() || undefined,
    conclusionText: form.conclusionText.trim() || undefined,
  };
}

export function AdmissionClinicalFormPage() {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);
  const connectedCaregiverName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';

  const [form, setForm] = useState<ClinicalFormState>(emptyForm);
  const [admission, setAdmission] = useState<AdmissionResponse | null>(null);
  const [loadOk, setLoadOk] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [patientDeceasedAt, setPatientDeceasedAt] = useState<string | null>(null);

  const admissionClosed =
    admission?.status === 'SORTI' || admission?.status === 'DECEDE';
  const clinicalFormLocked = Boolean(patientDeceasedAt) || admissionClosed;

  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoadOk(false);
      setLoading(false);
      return;
    }
    void loadForm();
  }, [admissionId]);

  async function loadForm() {
    setLoading(true);
    setError(null);
    setMessage(null);
    setLoadOk(false);
    try {
      const [formRes, deceasedAt, admissionRes] = await Promise.all([
        api.get<AdmissionClinicalFormResponse>(`/api/v1/admissions/${admissionId}/clinical-form`),
        fetchPatientDeceasedAtForAdmission(admissionId),
        api.get<AdmissionResponse>(`/api/v1/admissions/${admissionId}`),
      ]);
      setForm(fromResponse(formRes.data));
      setPatientDeceasedAt(deceasedAt);
      setAdmission(admissionRes.data);
      setLoadOk(true);
    } catch {
      setError('Impossible de charger le formulaire clinique.');
      setPatientDeceasedAt(null);
      setAdmission(null);
    } finally {
      setLoading(false);
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setMessage(null);
    try {
      const { data } = await api.put<AdmissionClinicalFormResponse>(
        `/api/v1/admissions/${admissionId}/clinical-form`,
        toPayload(form),
      );
      setForm(fromResponse(data));
      setMessage('Formulaire clinique enregistre.');
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer le formulaire clinique."));
    } finally {
      setSaving(false);
    }
  }

  function updateField<K extends keyof ClinicalFormState>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  return (
    <>
      <p style={{ color: 'var(--muted)', marginTop: 0, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <Link to="/admissions">Retour aux admissions</Link>
        <span>admission #{id}</span>
      </p>

      {message && (
        <div
          className="card"
          style={{ marginBottom: '1rem', borderColor: 'rgba(61, 154, 237, 0.5)', color: 'var(--text)' }}
        >
          {message}
        </div>
      )}
      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && loadOk && (
        <>
          {patientDeceasedAt ? (
            <PatientDeceasedBanner
              deceasedAt={patientDeceasedAt}
              detail="Le formulaire clinique est en lecture seule."
            />
          ) : admissionClosed ? (
            <div
              className="card"
              style={{
                marginBottom: '1rem',
                borderColor: 'rgba(232, 93, 106, 0.45)',
                background: 'rgba(232, 93, 106, 0.06)',
              }}
            >
              <strong>Séjour clôturé</strong>
              <p style={{ margin: '0.35rem 0 0', color: 'var(--muted)' }}>
                {admission?.status === 'SORTI'
                  ? "Cette admission est sortie. La saisie du formulaire clinique n'est plus possible."
                  : "Ce séjour est déclaré avec décès. La saisie du formulaire clinique n'est plus possible."}
              </p>
            </div>
          ) : null}

          <form onSubmit={onSubmit} className="card" style={{ display: 'grid', gap: '0.85rem' }}>
          <h3 style={{ marginTop: 0, marginBottom: '0.25rem', display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <span>Formulaire clinique</span>
            <span style={{ color: 'var(--text)', fontWeight: 500 }}>{connectedCaregiverName}</span>
          </h3>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="antecedentsText">Antecedents</label>
            <textarea
              id="antecedentsText"
              rows={4}
              value={form.antecedentsText}
              onChange={(e) => updateField('antecedentsText', e.target.value)}
              disabled={clinicalFormLocked}
              readOnly={clinicalFormLocked}
            />
          </div>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="anamnesisText">Anamnèse (histoire de la maladie)</label>
              <textarea
                id="anamnesisText"
                rows={4}
                value={form.anamnesisText}
                onChange={(e) => updateField('anamnesisText', e.target.value)}
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="paraclinicalText">Paraclinique (examens complémentaires)</label>
              <textarea
                id="paraclinicalText"
                rows={4}
                value={form.paraclinicalText}
                onChange={(e) => updateField('paraclinicalText', e.target.value)}
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
            </div>
          </div>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="physicalExamMiscText">Examen clinique (synthese)</label>
              <textarea
                id="physicalExamMiscText"
                rows={5}
                value={form.physicalExamMiscText}
                onChange={(e) => updateField('physicalExamMiscText', e.target.value)}
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="conclusionText">Conclusion</label>
              <textarea
                id="conclusionText"
                rows={5}
                value={form.conclusionText}
                onChange={(e) => updateField('conclusionText', e.target.value)}
                disabled={clinicalFormLocked}
                readOnly={clinicalFormLocked}
              />
            </div>
          </div>
          <div>
            <button type="submit" className="btn btn-primary" disabled={saving || clinicalFormLocked}>
              {saving ? 'Enregistrement...' : 'Enregistrer le formulaire'}
            </button>
          </div>
        </form>
        </>
      )}
    </>
  );
}
