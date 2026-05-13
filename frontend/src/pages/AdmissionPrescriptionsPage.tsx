import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';
import { fetchPatientDeceasedAtForAdmission } from '../utils/admissionPatientDeceased';
import type {
  PrescriptionLineCreateRequest,
  PrescriptionLineResponse,
  PrescriptionLineUpdateRequest,
} from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

type FormState = {
  medicationName: string;
  dosageText: string;
  frequencyText: string;
  instructionsText: string;
  prescriberName: string;
  startDate: string;
  endDate: string;
};

const emptyForm: FormState = {
  medicationName: '',
  dosageText: '',
  frequencyText: '',
  instructionsText: '',
  prescriberName: '',
  startDate: '',
  endDate: '',
};

export function AdmissionPrescriptionsPage() {
  const { user } = useAuth();
  const { id } = useParams<{ id: string }>();
  const admissionId = Number(id);
  const connectedCaregiverName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur connecté';

  const [items, setItems] = useState<PrescriptionLineResponse[]>([]);
  const [createForm, setCreateForm] = useState<FormState>(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<FormState>(emptyForm);
  const [editActive, setEditActive] = useState(true);
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
    void loadItems();
  }, [admissionId]);

  useEffect(() => {
    if (patientDeceasedAt) {
      setEditingId(null);
      setEditForm(emptyForm);
      setEditActive(true);
    }
  }, [patientDeceasedAt]);

  async function loadItems() {
    setLoading(true);
    setError(null);
    try {
      const [{ data }, deceasedAt] = await Promise.all([
        api.get<PrescriptionLineResponse[]>(`/api/v1/admissions/${admissionId}/prescription-lines`),
        fetchPatientDeceasedAtForAdmission(admissionId),
      ]);
      setItems(data);
      setPatientDeceasedAt(deceasedAt);
    } catch {
      setError('Impossible de charger les prescriptions.');
      setPatientDeceasedAt(null);
    } finally {
      setLoading(false);
    }
  }

  function toCreatePayload(form: FormState): PrescriptionLineCreateRequest {
    return {
      medicationName: form.medicationName.trim(),
      dosageText: form.dosageText.trim() || undefined,
      frequencyText: form.frequencyText.trim() || undefined,
      instructionsText: form.instructionsText.trim() || undefined,
      prescriberName: form.prescriberName.trim() || connectedCaregiverName,
      startDate: form.startDate,
      endDate: form.endDate || undefined,
    };
  }

  async function onCreateSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!createForm.medicationName.trim() || !createForm.startDate) {
      setError('Medicament et date de debut sont obligatoires.');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      await api.post<PrescriptionLineResponse>(
        `/api/v1/admissions/${admissionId}/prescription-lines`,
        toCreatePayload(createForm),
      );
      setCreateForm(emptyForm);
      await loadItems();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'ajouter la prescription."));
    } finally {
      setSubmitting(false);
    }
  }

  function startEdit(item: PrescriptionLineResponse) {
    setEditingId(item.id);
    setEditForm({
      medicationName: item.medicationName,
      dosageText: item.dosageText ?? '',
      frequencyText: item.frequencyText ?? '',
      instructionsText: item.instructionsText ?? '',
      prescriberName: item.prescriberName ?? '',
      startDate: item.startDate ?? '',
      endDate: item.endDate ?? '',
    });
    setEditActive(item.active);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditForm(emptyForm);
    setEditActive(true);
  }

  async function onEditSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (editingId == null) return;
    if (!editForm.medicationName.trim() || !editForm.startDate) {
      setError('Medicament et date de debut sont obligatoires.');
      return;
    }

    const payload: PrescriptionLineUpdateRequest = {
      ...toCreatePayload(editForm),
      active: editActive,
    };

    setSubmitting(true);
    setError(null);
    try {
      await api.put<PrescriptionLineResponse>(
        `/api/v1/admissions/${admissionId}/prescription-lines/${editingId}`,
        payload,
      );
      cancelEdit();
      await loadItems();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de mettre a jour la prescription.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <p style={{ color: 'var(--muted)', marginTop: 0, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
        <Link to="/admissions">Retour aux admissions</Link>
        <span>admission #{id}</span>
      </p>

      {!loading && (
        <PatientDeceasedBanner
          deceasedAt={patientDeceasedAt}
          detail="Les prescriptions ne peuvent plus être créées ni modifiées pour ce patient."
        />
      )}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginTop: 0, display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <span>Ajouter une prescription</span>
          <span style={{ color: 'var(--text)', fontWeight: 500 }}>{connectedCaregiverName}</span>
        </h3>
        <form onSubmit={onCreateSubmit} style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="medication-name">Medicament *</label>
            <input
              id="medication-name"
              value={createForm.medicationName}
              onChange={(e) => setCreateForm((v) => ({ ...v, medicationName: e.target.value }))}
              placeholder="Ex. Ceftriaxone"
              required
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="dosage-text">Posologie</label>
            <input
              id="dosage-text"
              value={createForm.dosageText}
              onChange={(e) => setCreateForm((v) => ({ ...v, dosageText: e.target.value }))}
              placeholder="Ex. 1 g"
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="frequency-text">Frequence</label>
            <input
              id="frequency-text"
              value={createForm.frequencyText}
              onChange={(e) => setCreateForm((v) => ({ ...v, frequencyText: e.target.value }))}
              placeholder="Ex. 2x/j"
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="start-date">Debut *</label>
            <input
              id="start-date"
              type="date"
              value={createForm.startDate}
              onChange={(e) => setCreateForm((v) => ({ ...v, startDate: e.target.value }))}
              required
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="end-date">Fin</label>
            <input
              id="end-date"
              type="date"
              value={createForm.endDate}
              onChange={(e) => setCreateForm((v) => ({ ...v, endDate: e.target.value }))}
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="instructions-text">Instructions</label>
            <input
              id="instructions-text"
              value={createForm.instructionsText}
              onChange={(e) => setCreateForm((v) => ({ ...v, instructionsText: e.target.value }))}
              placeholder="Instructions de prise"
              disabled={Boolean(patientDeceasedAt)}
            />
          </div>
          <div style={{ alignSelf: 'end' }}>
            <button type="submit" className="btn btn-primary" disabled={submitting || Boolean(patientDeceasedAt)}>
              {submitting ? 'Enregistrement...' : 'Ajouter'}
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
                  <th>Medicament</th>
                  <th>Posologie</th>
                  <th>Frequence</th>
                  <th>Dates</th>
                  <th>Statut</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ color: 'var(--muted)' }}>
                      Aucune prescription pour cette admission.
                    </td>
                  </tr>
                ) : (
                  items.map((item) => (
                    <tr key={item.id}>
                      <td>{item.medicationName}</td>
                      <td>{item.dosageText ?? '-'}</td>
                      <td>{item.frequencyText ?? '-'}</td>
                      <td>
                        {item.startDate}
                        {item.endDate ? ` -> ${item.endDate}` : ''}
                      </td>
                      <td>{item.active ? 'Active' : 'Inactive'}</td>
                      <td>
                        <button type="button" className="btn btn-ghost" onClick={() => startEdit(item)} disabled={Boolean(patientDeceasedAt)}>
                          Modifier
                        </button>
                        {' '}
                        <Link to={`/admissions/${id}/prescriptions/${item.id}/administrations`}>Administrations</Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </ScrollTableRegion>
          <TableResultFooter totalElements={items.length} displayedCount={items.length} itemLabelPlural="prescription(s)" />
        </div>
      )}

      {editingId != null && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <h3 style={{ marginTop: 0 }}>Modifier la prescription #{editingId}</h3>
          <form onSubmit={onEditSubmit} style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-medication-name">Medicament *</label>
              <input
                id="edit-medication-name"
                value={editForm.medicationName}
                onChange={(e) => setEditForm((v) => ({ ...v, medicationName: e.target.value }))}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-dosage-text">Posologie</label>
              <input
                id="edit-dosage-text"
                value={editForm.dosageText}
                onChange={(e) => setEditForm((v) => ({ ...v, dosageText: e.target.value }))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-frequency-text">Frequence</label>
              <input
                id="edit-frequency-text"
                value={editForm.frequencyText}
                onChange={(e) => setEditForm((v) => ({ ...v, frequencyText: e.target.value }))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-prescriber-name">Prescripteur</label>
              <input
                id="edit-prescriber-name"
                value={editForm.prescriberName}
                onChange={(e) => setEditForm((v) => ({ ...v, prescriberName: e.target.value }))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-start-date">Debut *</label>
              <input
                id="edit-start-date"
                type="date"
                value={editForm.startDate}
                onChange={(e) => setEditForm((v) => ({ ...v, startDate: e.target.value }))}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-end-date">Fin</label>
              <input
                id="edit-end-date"
                type="date"
                value={editForm.endDate}
                onChange={(e) => setEditForm((v) => ({ ...v, endDate: e.target.value }))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0, gridColumn: '1 / -1' }}>
              <label htmlFor="edit-instructions-text">Instructions</label>
              <input
                id="edit-instructions-text"
                value={editForm.instructionsText}
                onChange={(e) => setEditForm((v) => ({ ...v, instructionsText: e.target.value }))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-active">Etat</label>
              <select id="edit-active" value={editActive ? 'true' : 'false'} onChange={(e) => setEditActive(e.target.value === 'true')}>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>
            <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '0.75rem' }}>
              <button type="submit" className="btn btn-primary" disabled={submitting || Boolean(patientDeceasedAt)}>
                {submitting ? 'Mise a jour...' : 'Enregistrer les modifications'}
              </button>
              <button type="button" className="btn btn-ghost" onClick={cancelEdit} disabled={submitting}>
                Annuler
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
