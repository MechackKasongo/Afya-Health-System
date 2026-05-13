import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { PagePatientResponse, PatientCreateRequest, PatientResponse } from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

export function PatientsPage() {
  const [page, setPage] = useState<PagePatientResponse | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [filterInput, setFilterInput] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [sortBy, setSortBy] = useState<'id' | 'dossierNumber' | 'lastName' | 'firstName' | 'birthDate'>('id');
  const [sortDir, setSortDir] = useState<'desc' | 'asc'>('desc');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
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

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    const params = new URLSearchParams({ page: '0', size: String(LIST_FETCH_PAGE_SIZE), sortBy, sortDir });
    if (appliedQuery.trim()) params.set('query', appliedQuery.trim());
    api
      .get<PagePatientResponse>(`/api/v1/patients?${params}`)
      .then((res) => {
        if (!cancelled) setPage(res.data);
      })
      .catch(() => {
        if (!cancelled) setError('Impossible de charger les patients.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [appliedQuery, sortBy, sortDir, reloadKey]);

  function resetCreateForm() {
    setFirstName('');
    setLastName('');
    setBirthDate('');
    setSex('M');
    setPhone('');
    setEmail('');
    setAddress('');
    setPostName('');
    setEmployer('');
    setEmployeeId('');
    setProfession('');
    setSpouseName('');
    setSpouseProfession('');
    setCreateError(null);
  }

  async function onCreatePatientSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!firstName.trim() || !lastName.trim() || !birthDate || !sex.trim()) {
      setCreateError('Veuillez renseigner les champs obligatoires.');
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

    setSubmittingCreate(true);
    setCreateError(null);
    try {
      await api.post<PatientResponse>('/api/v1/patients', payload);
      setShowCreateDrawer(false);
      resetCreateForm();
      setReloadKey((k) => k + 1);
    } catch (err) {
      setCreateError(getApiErrorMessage(err, 'Impossible de créer le patient.'));
    } finally {
      setSubmittingCreate(false);
    }
  }

  function onSearchSubmit(e: React.FormEvent) {
    e.preventDefault();
    setAppliedQuery(filterInput);
  }

  return (
    <>
      <div className="card" style={{ marginBottom: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end', justifyContent: 'space-between' }}>
          <form onSubmit={onSearchSubmit} style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', alignItems: 'flex-end', flex: '1 1 520px' }}>
            <div className="field" style={{ flex: '1 1 220px', marginBottom: 0 }}>
              <label htmlFor="patient-search">Recherche (nom, dossier…)</label>
              <input
                id="patient-search"
                value={filterInput}
                onChange={(e) => setFilterInput(e.target.value)}
                placeholder="Ex. Martin ou DOS-"
              />
            </div>
            <div className="field" style={{ flex: '0 0 220px', marginBottom: 0 }}>
              <label htmlFor="patient-sort-by">Trier par</label>
              <select
                id="patient-sort-by"
                value={sortBy}
                onChange={(e) => {
                  setSortBy(e.target.value as typeof sortBy);
                }}
              >
                <option value="id">ID</option>
                <option value="dossierNumber">Dossier</option>
                <option value="lastName">Nom</option>
                <option value="firstName">Prénom</option>
                <option value="birthDate">Naissance</option>
              </select>
            </div>
            <div className="field" style={{ flex: '0 0 160px', marginBottom: 0 }}>
              <label htmlFor="patient-sort-dir">Ordre ↕</label>
              <select
                id="patient-sort-dir"
                value={sortDir}
                onChange={(e) => {
                  setSortDir(e.target.value as typeof sortDir);
                }}
              >
                <option value="desc">↓ Descendant</option>
                <option value="asc">↑ Ascendant</option>
              </select>
            </div>
            <button type="submit" className="btn btn-primary">
              Rechercher
            </button>
          </form>

          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setShowCreateDrawer(true);
              setCreateError(null);
            }}
          >
            + Nouveau patient
          </button>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && page && (
        <>
          <div className="card table-wrap">
            <ScrollTableRegion>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Dossier</th>
                    <th>Nom</th>
                    <th>Naissance</th>
                    <th>Sexe</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {page.content.length === 0 ? (
                    <tr>
                      <td colSpan={5} style={{ color: 'var(--muted)' }}>
                        Aucun patient.
                      </td>
                    </tr>
                  ) : (
                    page.content.map((p) => (
                      <tr key={p.id}>
                        <td>{p.dossierNumber}</td>
                        <td>
                          {[p.firstName, p.lastName, p.postName].filter(Boolean).join(' ')}
                        </td>
                        <td>{p.birthDate}</td>
                        <td>{p.sex}</td>
                        <td>
                          <Link to={`/patients/${p.id}`}>Détail</Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </ScrollTableRegion>
            <TableResultFooter
              totalElements={page.totalElements}
              displayedCount={page.content.length}
              itemLabelPlural="patient(s)"
            />
          </div>
        </>
      )}

      {showCreateDrawer && (
        <>
          <div
            role="presentation"
            onClick={() => setShowCreateDrawer(false)}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'color-mix(in srgb, var(--accent) 14%, transparent)',
              zIndex: 39,
            }}
          />
          <aside
            style={{
              position: 'fixed',
              top: 0,
              right: 0,
              height: '100vh',
              width: 'min(50vw, 760px)',
              minWidth: '360px',
              background: 'var(--surface)',
              borderLeft: '1px solid var(--border)',
              zIndex: 40,
              overflowY: 'auto',
              padding: '1rem',
              boxShadow: '0 10px 40px rgba(2, 6, 23, 0.25)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <strong>Créer un patient</strong>
              <button type="button" className="btn btn-ghost" onClick={() => setShowCreateDrawer(false)}>
                Fermer
              </button>
            </div>
            {createError && <div className="error-banner">{createError}</div>}
            <form onSubmit={onCreatePatientSubmit} className="card" style={{ display: 'grid', gap: '0.75rem' }}>
              <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-firstName">Prenom *</label>
                  <input id="drawer-firstName" value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-lastName">Nom *</label>
                  <input id="drawer-lastName" value={lastName} onChange={(e) => setLastName(e.target.value)} required />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-postName">Post-nom</label>
                  <input id="drawer-postName" value={postName} onChange={(e) => setPostName(e.target.value)} />
                </div>
              </div>
              <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-birthDate">Date naissance *</label>
                  <input id="drawer-birthDate" type="date" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} required />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-sex">Sexe *</label>
                  <select id="drawer-sex" value={sex} onChange={(e) => setSex(e.target.value)}>
                    <option value="M">M</option>
                    <option value="F">F</option>
                  </select>
                </div>
              </div>
              <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-phone">Telephone</label>
                  <input id="drawer-phone" value={phone} onChange={(e) => setPhone(e.target.value)} />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-email">Email</label>
                  <input id="drawer-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
                </div>
              </div>
              <div className="field" style={{ marginBottom: 0 }}>
                <label htmlFor="drawer-address">Adresse</label>
                <input id="drawer-address" value={address} onChange={(e) => setAddress(e.target.value)} />
              </div>
              <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-profession">Profession</label>
                  <input id="drawer-profession" value={profession} onChange={(e) => setProfession(e.target.value)} />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-employer">Employeur</label>
                  <input id="drawer-employer" value={employer} onChange={(e) => setEmployer(e.target.value)} />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-employeeId">Matricule</label>
                  <input id="drawer-employeeId" value={employeeId} onChange={(e) => setEmployeeId(e.target.value)} />
                </div>
              </div>
              <div style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))' }}>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-spouseName">Nom du conjoint</label>
                  <input id="drawer-spouseName" value={spouseName} onChange={(e) => setSpouseName(e.target.value)} />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="drawer-spouseProfession">Profession du conjoint</label>
                  <input id="drawer-spouseProfession" value={spouseProfession} onChange={(e) => setSpouseProfession(e.target.value)} />
                </div>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button type="submit" className="btn btn-primary" disabled={submittingCreate}>
                  {submittingCreate ? 'Création...' : 'Créer le patient'}
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => {
                    setShowCreateDrawer(false);
                    resetCreateForm();
                  }}
                >
                  Annuler
                </button>
              </div>
            </form>
          </aside>
        </>
      )}
    </>
  );
}
