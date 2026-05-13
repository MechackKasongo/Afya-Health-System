import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PatientCreateRequest, PatientResponse } from '../api/types';

export function PatientCreatePage() {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [sex, setSex] = useState('M');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [address, setAddress] = useState('');
  const [postName, setPostName] = useState('');
  const [employer, setEmployer] = useState('');
  const [employeeId, setEmployeeId] = useState('');
  const [profession, setProfession] = useState('');
  const [spouseName, setSpouseName] = useState('');
  const [spouseProfession, setSpouseProfession] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !birthDate || !sex.trim()) {
      setError('Veuillez renseigner les champs obligatoires.');
      return;
    }

    const payload: PatientCreateRequest = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      birthDate,
      sex: sex.trim(),
      phone: phone.trim() || undefined,
      email: email.trim() || undefined,
      address: address.trim() || undefined,
      postName: postName.trim() || undefined,
      employer: employer.trim() || undefined,
      employeeId: employeeId.trim() || undefined,
      profession: profession.trim() || undefined,
      spouseName: spouseName.trim() || undefined,
      spouseProfession: spouseProfession.trim() || undefined,
    };

    setSubmitting(true);
    setError(null);
    try {
      const { data } = await api.post<PatientResponse>('/api/v1/patients', payload);
      navigate('/patients');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de creer le patient.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <p style={{ color: 'var(--muted)', marginTop: 0 }}>
        <Link to="/patients">Retour aux patients</Link>
      </p>
      {error && <div className="error-banner">{error}</div>}
      <form onSubmit={onSubmit} className="card" style={{ display: 'grid', gap: '0.75rem', maxWidth: 760, margin: '0 auto' }}>
        <h3 style={{ marginTop: 0, marginBottom: '0.25rem' }}>Identification</h3>
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="firstName">Prenom *</label>
            <input id="firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="lastName">Nom *</label>
            <input id="lastName" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="postName">Post-nom</label>
            <input id="postName" value={postName} onChange={(e) => setPostName(e.target.value)} />
          </div>
        </div>
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="birthDate">Date naissance *</label>
            <input id="birthDate" type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} required />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="sex">Sexe *</label>
            <select id="sex" value={sex} onChange={(e) => setSex(e.target.value)}>
              <option value="M">M</option>
              <option value="F">F</option>
            </select>
          </div>
        </div>
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="phone">Telephone</label>
            <input id="phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </div>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="address">Adresse</label>
          <input id="address" value={address} onChange={(e) => setAddress(e.target.value)} />
        </div>
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="profession">Profession</label>
            <input id="profession" value={profession} onChange={(e) => setProfession(e.target.value)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="employer">Employeur</label>
            <input id="employer" value={employer} onChange={(e) => setEmployer(e.target.value)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="employeeId">Matricule</label>
            <input id="employeeId" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} />
          </div>
        </div>
        <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="spouseName">Nom du conjoint</label>
            <input id="spouseName" value={spouseName} onChange={(e) => setSpouseName(e.target.value)} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="spouseProfession">Profession du conjoint</label>
            <input id="spouseProfession" value={spouseProfession} onChange={(e) => setSpouseProfession(e.target.value)} />
          </div>
        </div>
        <div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Création...' : 'Créer le patient'}
          </button>
        </div>
      </form>
    </>
  );
}
