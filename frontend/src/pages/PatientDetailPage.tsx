import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PatientResponse, PatientUpdateRequest } from '../api/types';
import { PatientDeceasedBanner } from '../components/PatientDeceasedBanner';

function fillFormFromPatient(p: PatientResponse) {
  return {
    firstName: p.firstName,
    lastName: p.lastName,
    birthDate: p.birthDate,
    sex: p.sex,
    phone: p.phone ?? '',
    email: p.email ?? '',
    address: p.address ?? '',
    postName: p.postName ?? '',
    employer: p.employer ?? '',
    employeeId: p.employeeId ?? '',
    profession: p.profession ?? '',
    spouseName: p.spouseName ?? '',
    spouseProfession: p.spouseProfession ?? '',
  };
}

type FormState = ReturnType<typeof fillFormFromPatient>;

export function PatientDetailPage() {
  const { id } = useParams<{ id: string }>();
  const [patient, setPatient] = useState<PatientResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState<FormState | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadPatient = useCallback(() => {
    if (!id) return;
    let cancelled = false;
    api
      .get<PatientResponse>(`/api/v1/patients/${id}`)
      .then((res) => {
        if (!cancelled) {
          setPatient(res.data);
          setForm(fillFormFromPatient(res.data));
        }
      })
      .catch(() => {
        if (!cancelled) setError('Patient introuvable ou accès refusé.');
      });
    return () => {
      cancelled = true;
    };
  }, [id]);

  useEffect(() => {
    return loadPatient();
  }, [loadPatient]);

  useEffect(() => {
    if (patient?.deceasedAt) {
      setEditing(false);
    }
  }, [patient?.deceasedAt]);

  useEffect(() => {
    if (!message) return;
    const t = window.setTimeout(() => setMessage(null), 2800);
    return () => window.clearTimeout(t);
  }, [message]);

  function startEdit() {
    if (patient) setForm(fillFormFromPatient(patient));
    setEditing(true);
    setSaveError(null);
  }

  function cancelEdit() {
    if (patient) setForm(fillFormFromPatient(patient));
    setEditing(false);
    setSaveError(null);
  }

  async function onSave(e: React.FormEvent) {
    e.preventDefault();
    if (!id || !form) return;
    if (!form.firstName.trim() || !form.lastName.trim() || !form.birthDate || !form.sex.trim()) {
      setSaveError('Veuillez renseigner les champs obligatoires (prénom, nom, naissance, sexe).');
      return;
    }

    const payload: PatientUpdateRequest = {
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      birthDate: form.birthDate,
      sex: form.sex.trim(),
      phone: form.phone.trim() || null,
      email: form.email.trim() ? form.email.trim() : null,
      address: form.address.trim() || null,
      postName: form.postName.trim(),
      employer: form.employer.trim(),
      employeeId: form.employeeId.trim(),
      profession: form.profession.trim(),
      spouseName: form.spouseName.trim(),
      spouseProfession: form.spouseProfession.trim(),
    };

    setSubmitting(true);
    setSaveError(null);
    try {
      const { data } = await api.put<PatientResponse>(`/api/v1/patients/${id}`, payload);
      setPatient(data);
      setForm(fillFormFromPatient(data));
      setEditing(false);
      setMessage('Fiche patient enregistrée.');
    } catch (err) {
      setSaveError(getApiErrorMessage(err, 'Impossible de mettre à jour le patient.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (error) {
    return (
      <>
        <div className="error-banner">{error}</div>
        <Link to="/patients">← Retour à la liste</Link>
      </>
    );
  }

  if (!patient || !form) {
    return <p style={{ color: 'var(--muted)' }}>Chargement…</p>;
  }

  const rows: { label: string; value: string | null | undefined }[] = [
    { label: 'N° dossier', value: patient.dossierNumber },
    { label: 'Prénom', value: patient.firstName },
    { label: 'Nom', value: patient.lastName },
    { label: 'Post-nom', value: patient.postName },
    { label: 'Naissance', value: patient.birthDate },
    { label: 'Sexe', value: patient.sex },
    { label: 'Téléphone', value: patient.phone },
    { label: 'Email', value: patient.email },
    { label: 'Adresse', value: patient.address },
    { label: 'Employeur', value: patient.employer },
    { label: 'Matricule', value: patient.employeeId },
    { label: 'Profession', value: patient.profession },
    { label: 'Conjoint', value: patient.spouseName },
    { label: 'Profession du conjoint', value: patient.spouseProfession },
  ];

  return (
    <>
      <p style={{ marginBottom: '1rem' }}>
        <Link to="/patients">← Patients</Link>
      </p>
      <PatientDeceasedBanner
        deceasedAt={patient.deceasedAt}
        detail="La fiche administrative ne peut pas être modifiée."
      />

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
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'baseline', justifyContent: 'space-between', gap: '0.75rem', marginBottom: '0.5rem' }}>
        <h1 className="page-title" style={{ marginBottom: 0 }}>
          {[patient.firstName, patient.lastName, patient.postName].filter(Boolean).join(' ')}
        </h1>
        {!editing && !patient.deceasedAt && (
          <button type="button" className="btn btn-primary" onClick={startEdit}>
            Modifier les informations
          </button>
        )}
      </div>

      {editing ? (
        <form onSubmit={onSave} className="card" style={{ display: 'grid', gap: '0.75rem', maxWidth: 760 }}>
          {saveError && <div className="error-banner">{saveError}</div>}
          <p style={{ margin: 0, color: 'var(--muted)', fontSize: '0.875rem' }}>
            Le numéro de dossier (<strong>{patient.dossierNumber}</strong>) est figé après création.
          </p>

          <h3 style={{ marginTop: 0, marginBottom: '0.25rem' }}>Identification</h3>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-firstName">Prénom *</label>
              <input
                id="edit-firstName"
                value={form.firstName}
                onChange={(e) => setForm((f) => (f ? { ...f, firstName: e.target.value } : f))}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-lastName">Nom *</label>
              <input
                id="edit-lastName"
                value={form.lastName}
                onChange={(e) => setForm((f) => (f ? { ...f, lastName: e.target.value } : f))}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-postName">Post-nom</label>
              <input
                id="edit-postName"
                value={form.postName}
                onChange={(e) => setForm((f) => (f ? { ...f, postName: e.target.value } : f))}
              />
            </div>
          </div>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-birthDate">Date de naissance *</label>
              <input
                id="edit-birthDate"
                type="date"
                value={form.birthDate}
                onChange={(e) => setForm((f) => (f ? { ...f, birthDate: e.target.value } : f))}
                required
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-sex">Sexe *</label>
              <select id="edit-sex" value={form.sex} onChange={(e) => setForm((f) => (f ? { ...f, sex: e.target.value } : f))}>
                <option value="M">M</option>
                <option value="F">F</option>
              </select>
            </div>
          </div>

          <h3 style={{ marginBottom: '0.25rem' }}>Coordonnées</h3>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-phone">Téléphone</label>
              <input
                id="edit-phone"
                value={form.phone}
                onChange={(e) => setForm((f) => (f ? { ...f, phone: e.target.value } : f))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-email">Email</label>
              <input
                id="edit-email"
                type="email"
                value={form.email}
                onChange={(e) => setForm((f) => (f ? { ...f, email: e.target.value } : f))}
              />
            </div>
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="edit-address">Adresse</label>
            <input
              id="edit-address"
              value={form.address}
              onChange={(e) => setForm((f) => (f ? { ...f, address: e.target.value } : f))}
            />
          </div>

          <h3 style={{ marginBottom: '0.25rem' }}>Situation</h3>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-profession">Profession</label>
              <input
                id="edit-profession"
                value={form.profession}
                onChange={(e) => setForm((f) => (f ? { ...f, profession: e.target.value } : f))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-employer">Employeur</label>
              <input
                id="edit-employer"
                value={form.employer}
                onChange={(e) => setForm((f) => (f ? { ...f, employer: e.target.value } : f))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-employeeId">Matricule</label>
              <input
                id="edit-employeeId"
                value={form.employeeId}
                onChange={(e) => setForm((f) => (f ? { ...f, employeeId: e.target.value } : f))}
              />
            </div>
          </div>
          <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-spouseName">Conjoint</label>
              <input
                id="edit-spouseName"
                value={form.spouseName}
                onChange={(e) => setForm((f) => (f ? { ...f, spouseName: e.target.value } : f))}
              />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="edit-spouseProfession">Profession du conjoint</label>
              <input
                id="edit-spouseProfession"
                value={form.spouseProfession}
                onChange={(e) => setForm((f) => (f ? { ...f, spouseProfession: e.target.value } : f))}
              />
            </div>
          </div>

          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.25rem' }}>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Enregistrement…' : 'Enregistrer'}
            </button>
            <button type="button" className="btn btn-ghost" onClick={cancelEdit} disabled={submitting}>
              Annuler
            </button>
          </div>
        </form>
      ) : (
        <div className="card">
          <table className="data-table">
            <tbody>
              {rows.map((r) => (
                <tr key={r.label}>
                  <th style={{ width: '40%', color: 'var(--muted)', fontWeight: 600 }}>{r.label}</th>
                  <td>{r.value ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
