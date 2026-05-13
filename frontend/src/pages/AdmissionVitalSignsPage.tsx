import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { VitalSignCreateRequest, VitalSignResponse, VitalSignSlot } from '../api/types';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { fetchPatientDeceasedAtForAdmission } from '../utils/admissionPatientDeceased';

const slotLabels: Record<VitalSignSlot, string> = {
  MATIN: 'Matin',
  SOIR: 'Soir',
  JOURNEE: 'Journee',
};

function toNullableInt(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number.parseInt(trimmed, 10);
  return Number.isNaN(parsed) ? undefined : parsed;
}

function toNullableFloat(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const parsed = Number.parseFloat(trimmed);
  return Number.isNaN(parsed) ? undefined : parsed;
}

export function AdmissionVitalSignsPage() {
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);

  const [items, setItems] = useState<VitalSignResponse[]>([]);
  const [recordedAt, setRecordedAt] = useState('');
  const [slot, setSlot] = useState('');
  const [systolicBp, setSystolicBp] = useState('');
  const [diastolicBp, setDiastolicBp] = useState('');
  const [pulseBpm, setPulseBpm] = useState('');
  const [temperatureCelsius, setTemperatureCelsius] = useState('');
  const [weightKg, setWeightKg] = useState('');
  const [diuresisMl, setDiuresisMl] = useState('');
  const [stoolsNote, setStoolsNote] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [patientDeceasedAt, setPatientDeceasedAt] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(admissionId)) {
      setError("ID d'admission invalide.");
      setLoading(false);
      return;
    }
    void loadVitalSigns();
  }, [admissionId]);

  async function loadVitalSigns() {
    setLoading(true);
    setError(null);
    try {
      const [{ data }, deceasedAt] = await Promise.all([
        api.get<VitalSignResponse[]>(`/api/v1/admissions/${admissionId}/vital-signs`),
        fetchPatientDeceasedAtForAdmission(admissionId),
      ]);
      setItems(data);
      setPatientDeceasedAt(deceasedAt);
    } catch {
      setError('Impossible de charger les constantes.');
      setPatientDeceasedAt(null);
    } finally {
      setLoading(false);
    }
  }

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!recordedAt) {
      setError("La date/heure du releve est obligatoire.");
      return;
    }

    const payload: VitalSignCreateRequest = {
      recordedAt,
      systolicBp: toNullableInt(systolicBp),
      diastolicBp: toNullableInt(diastolicBp),
      pulseBpm: toNullableInt(pulseBpm),
      temperatureCelsius: toNullableFloat(temperatureCelsius),
      weightKg: toNullableFloat(weightKg),
      diuresisMl: toNullableInt(diuresisMl),
      stoolsNote: stoolsNote.trim() || undefined,
    };
    if (slot) payload.slot = slot as VitalSignSlot;

    setSubmitting(true);
    setError(null);
    try {
      await api.post<VitalSignResponse>(`/api/v1/admissions/${admissionId}/vital-signs`, payload);
      setRecordedAt('');
      setSlot('');
      setSystolicBp('');
      setDiastolicBp('');
      setPulseBpm('');
      setTemperatureCelsius('');
      setWeightKg('');
      setDiuresisMl('');
      setStoolsNote('');
      await loadVitalSigns();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'enregistrer la constante."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <h1 className="page-title">Constantes admission #{id}</h1>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/admissions">Retour aux admissions</Link>
      </p>

      {!loading && (
        <PatientDeceasedBanner
          deceasedAt={patientDeceasedAt}
          detail="Aucun nouveau relevé ne peut être ajouté pour ce patient."
        />
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginTop: 0 }}>Ajouter un releve</h3>
        <form onSubmit={onSubmit} style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="recordedAt">Date et heure *</label>
            <input
              id="recordedAt"
              type="datetime-local"
              value={recordedAt}
              onChange={(e) => setRecordedAt(e.target.value)}
              required
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="slot">Creneau</label>
            <select id="slot" value={slot} onChange={(e) => setSlot(e.target.value)} disabled={Boolean(patientDeceasedAt)}>
              <option value="">Non precise</option>
              <option value="MATIN">Matin</option>
              <option value="SOIR">Soir</option>
              <option value="JOURNEE">Journee</option>
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="systolic">TA systolique</label>
            <input id="systolic" inputMode="numeric" value={systolicBp} onChange={(e) => setSystolicBp(e.target.value)} placeholder="120" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="diastolic">TA diastolique</label>
            <input id="diastolic" inputMode="numeric" value={diastolicBp} onChange={(e) => setDiastolicBp(e.target.value)} placeholder="80" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="pulse">Pouls (bpm)</label>
            <input id="pulse" inputMode="numeric" value={pulseBpm} onChange={(e) => setPulseBpm(e.target.value)} placeholder="72" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="temp">Temperature (C)</label>
            <input id="temp" inputMode="decimal" value={temperatureCelsius} onChange={(e) => setTemperatureCelsius(e.target.value)} placeholder="37.2" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="weight">Poids (kg)</label>
            <input id="weight" inputMode="decimal" value={weightKg} onChange={(e) => setWeightKg(e.target.value)} placeholder="68.5" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="diuresis">Diurese (ml)</label>
            <input id="diuresis" inputMode="numeric" value={diuresisMl} onChange={(e) => setDiuresisMl(e.target.value)} placeholder="500" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
            <label htmlFor="stoolsNote">Selles / observation</label>
            <input id="stoolsNote" value={stoolsNote} onChange={(e) => setStoolsNote(e.target.value)} placeholder="Observation libre" disabled={Boolean(patientDeceasedAt)} />
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <button type="submit" className="btn btn-primary" disabled={submitting || Boolean(patientDeceasedAt)}>
              {submitting ? 'Enregistrement...' : 'Enregistrer'}
            </button>
          </div>
        </form>
      </div>

      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Date/heure</th>
                  <th>Creneau</th>
                  <th>TA</th>
                  <th>Pouls</th>
                  <th>Temp.</th>
                  <th>Poids</th>
                  <th>Diurese</th>
                  <th>Selles</th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={8} style={{ color: 'var(--muted)' }}>
                      Aucun releve pour cette admission.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{new Date(item.recordedAt).toLocaleString('fr-FR')}</td>
                      <td>{item.slot ? slotLabels[item.slot] : '-'}</td>
                      <td>
                        {item.systolicBp ?? '-'} / {item.diastolicBp ?? '-'}
                      </td>
                      <td>{item.pulseBpm ?? '-'}</td>
                      <td>{item.temperatureCelsius ?? '-'}</td>
                      <td>{item.weightKg ?? '-'}</td>
                      <td>{item.diuresisMl ?? '-'}</td>
                      <td>{item.stoolsNote ?? '-'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter totalElements={items.length} displayedCount={items.length} itemLabelPlural="relevé(s)" />
        </div>
      )}
    </>
  );
}
