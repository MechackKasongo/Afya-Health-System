import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, getStoredAccessToken } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type {
  CredentialsLogPreviewResponse,
  HospitalServiceResponse,
  PageHospitalServiceResponse,
  PageUserResponse,
  PasswordPreviewResponse,
  RoleOptionResponse,
  UserCreateRequest,
  UserResponse,
  UserUpdateRequest,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

function isValidEmail(email: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function getApiBaseURL(): string {
  return import.meta.env.VITE_API_BASE_URL ?? '';
}

function slugLetters(s: string): string {
  return s
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/[^a-zA-Z]/g, '')
    .toLowerCase();
}

async function downloadCredentialsCsv(): Promise<void> {
  const token = getStoredAccessToken();
  const res = await fetch(`${getApiBaseURL()}/api/v1/users/credentials-log.csv`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'comptes-utilisateurs-afya.csv';
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

async function downloadCredentialsFile(): Promise<void> {
  const token = getStoredAccessToken();
  const res = await fetch(`${getApiBaseURL()}/api/v1/users/credentials-log`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  const blob = await res.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'comptes-utilisateurs-afya.txt';
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function printCredentialsContent(text: string) {
  const w = window.open('', '_blank');
  if (!w) return;
  w.document.write('<html><head><title>Comptes utilisateurs AFYA</title></head><body></body></html>');
  const pre = w.document.createElement('pre');
  pre.style.whiteSpace = 'pre-wrap';
  pre.style.fontFamily = 'ui-monospace, monospace';
  pre.style.fontSize = '12px';
  pre.textContent = text;
  w.document.body.appendChild(pre);
  w.document.close();
  w.focus();
  w.print();
}

export function UsersPage() {
  const { user: currentUser } = useAuth();
  const [page, setPage] = useState<PageUserResponse | null>(null);
  const [queryInput, setQueryInput] = useState('');
  const [appliedQuery, setAppliedQuery] = useState('');
  const [sortBy, setSortBy] = useState<'id' | 'username' | 'fullName' | 'active'>('id');
  const [sortDir, setSortDir] = useState<'desc' | 'asc'>('desc');
  const [roles, setRoles] = useState<RoleOptionResponse[]>([]);
  const [hospitalServicesCatalog, setHospitalServicesCatalog] = useState<HospitalServiceResponse[]>([]);
  const [createHospitalServiceIds, setCreateHospitalServiceIds] = useState<number[]>([]);
  const [editHospitalServiceIds, setEditHospitalServiceIds] = useState<number[]>([]);
  const [createFirstName, setCreateFirstName] = useState('');
  const [createLastName, setCreateLastName] = useState('');
  const [createPostName, setCreatePostName] = useState('');
  const [createEmail, setCreateEmail] = useState('');
  const [createRole, setCreateRole] = useState('ROLE_RECEPTION');
  const [createPasswordLength, setCreatePasswordLength] = useState<12 | 16>(16);
  const [passwordPreview, setPasswordPreview] = useState<string | null>(null);
  const [passwordVariation, setPasswordVariation] = useState(0);
  const [pwdSuggestLoading, setPwdSuggestLoading] = useState(false);
  const [creationResult, setCreationResult] = useState<{ username: string; password: string } | null>(null);
  const [credentialsModalOpen, setCredentialsModalOpen] = useState(false);
  const [credentialsPreview, setCredentialsPreview] = useState<CredentialsLogPreviewResponse | null>(null);
  const [credentialsPreviewLoading, setCredentialsPreviewLoading] = useState(false);
  const [credentialsDeleting, setCredentialsDeleting] = useState(false);
  const [credentialsDownloadBusy, setCredentialsDownloadBusy] = useState(false);
  const [credentialsCsvBusy, setCredentialsCsvBusy] = useState(false);
  const [editUserId, setEditUserId] = useState<number | null>(null);
  const [editFullName, setEditFullName] = useState('');
  const [editEmail, setEditEmail] = useState('');
  const [editRole, setEditRole] = useState('ROLE_RECEPTION');
  const [editPassword, setEditPassword] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [showCreateDrawer, setShowCreateDrawer] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    void loadRoles();
    void (async () => {
      try {
        const { data } = await api.get<PageHospitalServiceResponse>(
          '/api/v1/hospital-services?activeOnly=true&page=0&size=500'
        );
        setHospitalServicesCatalog(data.content);
      } catch {
        setHospitalServicesCatalog([]);
      }
    })();
  }, []);

  useEffect(() => {
    if (!message) return;
    const timer = window.setTimeout(() => setMessage(null), 2500);
    return () => window.clearTimeout(timer);
  }, [message]);

  useEffect(() => {
    void loadUsers();
  }, [appliedQuery, sortBy, sortDir]);

  const suggestedUsername = useMemo(() => {
    const a = slugLetters(createFirstName);
    const b = slugLetters(createLastName);
    const j = `${a}${b}`;
    return j || '—';
  }, [createFirstName, createLastName]);

  const fetchPasswordSuggestion = useCallback(
    async (variation: number) => {
      if (!createFirstName.trim() || !createLastName.trim()) {
        return;
      }
      setPwdSuggestLoading(true);
      setCreateError(null);
      try {
        const { data } = await api.post<PasswordPreviewResponse>('/api/v1/users/password-preview', {
          firstName: createFirstName.trim(),
          lastName: createLastName.trim(),
          postName: createPostName.trim() || undefined,
          generatedPasswordLength: createPasswordLength,
          variation,
        });
        setPasswordPreview(data.password);
        setPasswordVariation(variation);
      } catch (err) {
        setCreateError(getApiErrorMessage(err, 'Impossible de proposer un mot de passe.'));
      } finally {
        setPwdSuggestLoading(false);
      }
    },
    [createFirstName, createLastName, createPostName, createPasswordLength]
  );

  useEffect(() => {
    if (!createFirstName.trim() || !createLastName.trim()) {
      setPasswordPreview(null);
      setPasswordVariation(0);
      return;
    }
    const handle = window.setTimeout(() => {
      void fetchPasswordSuggestion(0);
    }, 420);
    return () => window.clearTimeout(handle);
  }, [createFirstName, createLastName, createPostName, createPasswordLength, fetchPasswordSuggestion]);

  async function loadRoles() {
    try {
      const { data } = await api.get<RoleOptionResponse[]>('/api/v1/users/roles');
      setRoles(data);
      if (data.length > 0) {
        setCreateRole(data[0].name);
        setEditRole(data[0].name);
      }
    } catch {
      // keep graceful fallback, create/update will still show backend validation message
      setRoles([]);
    }
  }

  async function loadUsers() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        page: '0',
        size: String(LIST_FETCH_PAGE_SIZE),
        sortBy,
        sortDir,
      });
      if (appliedQuery.trim()) params.set('query', appliedQuery.trim());
      const { data } = await api.get<PageUserResponse>(`/api/v1/users?${params.toString()}`);
      setPage(data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les utilisateurs.'));
    } finally {
      setLoading(false);
    }
  }

  function resetCreateUserForm() {
    setCreateFirstName('');
    setCreateLastName('');
    setCreatePostName('');
    setCreateEmail('');
    setCreateRole(roles[0]?.name ?? 'ROLE_RECEPTION');
    setCreateHospitalServiceIds([]);
    setCreatePasswordLength(16);
    setPasswordPreview(null);
    setPasswordVariation(0);
    setCreateError(null);
    setCreationResult(null);
  }

  async function onCreateUser(e: React.FormEvent) {
    e.preventDefault();
    if (!createFirstName.trim() || !createLastName.trim() || !createRole.trim()) {
      setCreateError('Prénom, nom et rôle sont requis.');
      return;
    }
    if (createEmail.trim() && !isValidEmail(createEmail.trim())) {
      setCreateError('Format email invalide.');
      return;
    }
    if (!passwordPreview?.trim()) {
      setCreateError('Attendez la proposition de mot de passe ou cliquez sur ↻ pour en générer une.');
      return;
    }
    const payload: UserCreateRequest = {
      firstName: createFirstName.trim(),
      lastName: createLastName.trim(),
      postName: createPostName.trim() || undefined,
      email: createEmail.trim() || undefined,
      role: createRole.trim(),
      generatedPasswordLength: createPasswordLength,
      passwordVariation,
      hospitalServiceIds: createHospitalServiceIds,
    };
    setSubmittingCreate(true);
    setCreateError(null);
    setMessage(null);
    try {
      const { data } = await api.post<UserResponse>('/api/v1/users', payload);
      setCreationResult({
        username: data.username,
        password: data.generatedPassword ?? '',
      });
      setCreateFirstName('');
      setCreateLastName('');
      setCreatePostName('');
      setCreateEmail('');
      setCreateRole(roles[0]?.name ?? 'ROLE_RECEPTION');
      setPasswordPreview(null);
      setPasswordVariation(0);
      setCreateError(null);
      setMessage('Utilisateur créé — mot de passe généré (fichiers TXT et CSV sur le serveur).');
      await loadUsers();
    } catch (err) {
      setCreateError(getApiErrorMessage(err, "Impossible de créer l'utilisateur."));
    } finally {
      setSubmittingCreate(false);
    }
  }

  async function openCredentialsModal() {
    setCredentialsModalOpen(true);
    setCredentialsPreview(null);
    setCredentialsPreviewLoading(true);
    setError(null);
    try {
      const { data } = await api.get<CredentialsLogPreviewResponse>('/api/v1/users/credentials-log/preview');
      setCredentialsPreview(data);
    } catch (err) {
      setCredentialsModalOpen(false);
      setError(getApiErrorMessage(err, 'Impossible de charger le fichier des comptes.'));
    } finally {
      setCredentialsPreviewLoading(false);
    }
  }

  async function handleDeleteCredentialsFile() {
    if (!window.confirm('Supprimer définitivement le fichier des comptes sur le serveur ?')) return;
    setCredentialsDeleting(true);
    setError(null);
    try {
      await api.delete('/api/v1/users/credentials-log');
      const { data } = await api.get<CredentialsLogPreviewResponse>('/api/v1/users/credentials-log/preview');
      setCredentialsPreview(data);
      setMessage('Fichier des comptes supprimé.');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de supprimer le fichier.'));
    } finally {
      setCredentialsDeleting(false);
    }
  }

  async function handleDownloadFromModal() {
    setCredentialsDownloadBusy(true);
    setError(null);
    try {
      await downloadCredentialsFile();
    } catch (err) {
      const msg =
        err instanceof Error && err.message.trim() ? err.message : 'Impossible de télécharger le fichier.';
      setError(msg);
    } finally {
      setCredentialsDownloadBusy(false);
    }
  }

  async function handleDownloadCsvFromModal() {
    setCredentialsCsvBusy(true);
    setError(null);
    try {
      await downloadCredentialsCsv();
    } catch (err) {
      const msg =
        err instanceof Error && err.message.trim() ? err.message : 'Impossible de télécharger le CSV.';
      setError(msg);
    } finally {
      setCredentialsCsvBusy(false);
    }
  }

  function startEdit(user: UserResponse) {
    setEditUserId(user.id);
    setEditFullName(user.fullName);
    setEditEmail(user.email ?? '');
    setEditRole(user.roles[0] ?? roles[0]?.name ?? 'ROLE_RECEPTION');
    setEditHospitalServiceIds([...(user.hospitalServiceIds ?? [])]);
    setEditPassword('');
  }

  async function onUpdateUser(e: React.FormEvent) {
    e.preventDefault();
    if (editUserId == null) return;
    if (!editFullName.trim() || !editRole.trim()) {
      setError('Nom complet et rôle sont requis.');
      return;
    }
    if (editEmail.trim() && !isValidEmail(editEmail.trim())) {
      setError('Format email invalide.');
      return;
    }
    const payload: UserUpdateRequest = {
      fullName: editFullName.trim(),
      email: editEmail.trim() || undefined,
      role: editRole.trim(),
      password: editPassword.trim() || undefined,
      hospitalServiceIds: editHospitalServiceIds,
    };
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.put<UserResponse>(`/api/v1/users/${editUserId}`, payload);
      setMessage('Utilisateur mis à jour.');
      setEditUserId(null);
      await loadUsers();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de mettre à jour l'utilisateur."));
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleStatus(user: UserResponse) {
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.patch<UserResponse>(`/api/v1/users/${user.id}/status`, { active: !user.active });
      setMessage(user.active ? 'Utilisateur désactivé.' : 'Utilisateur activé.');
      await loadUsers();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de changer le statut utilisateur."));
    } finally {
      setSubmitting(false);
    }
  }

  async function deleteUser(user: UserResponse) {
    if (!window.confirm(`Supprimer l'utilisateur ${user.username} ?`)) {
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.delete(`/api/v1/users/${user.id}`);
      setMessage('Utilisateur supprimé.');
      await loadUsers();
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de supprimer l'utilisateur."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
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

      <div className="card" style={{ marginBottom: '1rem' }}>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setAppliedQuery(queryInput);
          }}
          style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-end', flexWrap: 'wrap' }}
        >
          <div className="field" style={{ flex: '1 1 260px', marginBottom: 0 }}>
            <label htmlFor="u-query">Recherche (username, nom)</label>
            <input id="u-query" value={queryInput} onChange={(e) => setQueryInput(e.target.value)} />
          </div>
          <div className="field" style={{ flex: '0 0 180px', marginBottom: 0 }}>
            <label htmlFor="u-sort-by">Trier par</label>
            <select
              id="u-sort-by"
              value={sortBy}
              onChange={(e) => {
                setSortBy(e.target.value as typeof sortBy);
              }}
            >
              <option value="id">ID</option>
              <option value="username">Username</option>
              <option value="fullName">Nom complet</option>
              <option value="active">Statut</option>
            </select>
          </div>
          <div className="field" style={{ flex: '0 0 160px', marginBottom: 0 }}>
            <label htmlFor="u-sort-dir">Ordre ↕</label>
            <select
              id="u-sort-dir"
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
          <button
            type="button"
            className="btn btn-ghost"
            disabled={credentialsPreviewLoading}
            onClick={() => void openCredentialsModal()}
            title="Voir le fichier des comptes, puis télécharger ou supprimer"
          >
            Fichier des comptes
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setShowCreateDrawer(true);
              setCreateError(null);
              setCreationResult(null);
            }}
          >
            Créer utilisateur
          </button>
        </form>
      </div>

      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && page && (
        <>
          <div className="card table-wrap">
            <ScrollTableRegion>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Username</th>
                    <th>Email</th>
                    <th>Nom complet</th>
                    <th>Rôles</th>
                    <th>Services</th>
                    <th>Statut</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {page.content.length === 0 ? (
                    <tr><td colSpan={8} style={{ color: 'var(--muted)' }}>Aucun utilisateur.</td></tr>
                  ) : (
                    page.content.map((u: UserResponse) => (
                      <tr key={u.id}>
                        <td>{u.id}</td>
                        <td>{u.username}</td>
                        <td>{u.email || '-'}</td>
                        <td>{u.fullName}</td>
                        <td>{u.roles.join(', ')}</td>
                        <td title={(u.hospitalServiceIds ?? []).join(', ')}>
                          {(u.hospitalServiceIds?.length ?? 0) === 0 ? '—' : `${u.hospitalServiceIds?.length} affect.`}
                        </td>
                        <td>{u.active ? 'Actif' : 'Inactif'}</td>
                        <td>
                          <button type="button" className="btn btn-ghost" onClick={() => startEdit(u)}>Modifier</button>{' '}
                          <button
                            type="button"
                            className="btn btn-ghost"
                            onClick={() => void toggleStatus(u)}
                            disabled={submitting || currentUser?.username === u.username}
                          >
                            {u.active ? 'Désactiver' : 'Activer'}
                          </button>{' '}
                          <button
                            type="button"
                            className="btn btn-danger"
                            onClick={() => void deleteUser(u)}
                            disabled={submitting || currentUser?.username === u.username}
                          >
                            Supprimer
                          </button>
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
              itemLabelPlural="utilisateur(s)"
            />
          </div>
        </>
      )}

      {showCreateDrawer && (
        <>
          <div
            role="presentation"
            onClick={() => {
              if (!creationResult) {
                setShowCreateDrawer(false);
                resetCreateUserForm();
              }
            }}
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
              width: 'min(50vw, 640px)',
              minWidth: '360px',
              maxWidth: '100vw',
              background: 'var(--surface)',
              borderLeft: '1px solid var(--border)',
              zIndex: 40,
              overflowY: 'auto',
              padding: '1.25rem',
              boxShadow: '0 10px 40px rgba(2, 6, 23, 0.25)',
              boxSizing: 'border-box',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
              <strong>Créer un utilisateur</strong>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={() => {
                  setShowCreateDrawer(false);
                  resetCreateUserForm();
                }}
              >
                Fermer
              </button>
            </div>
            {createError && <div className="error-banner">{createError}</div>}
            {creationResult && (
              <div className="card" style={{ marginBottom: '0.75rem', borderColor: 'rgba(61,154,237,0.45)' }}>
                <p style={{ marginTop: 0, marginBottom: '0.5rem' }}>
                  Compte créé : <strong>{creationResult.username}</strong>
                </p>
                <p style={{ margin: '0 0 0.5rem', fontSize: '0.85rem', color: 'var(--muted)' }}>
                  Mot de passe généré (copiez-le maintenant ; il est aussi ajouté au fichier export sur le serveur).
                </p>
                <code
                  style={{
                    display: 'block',
                    wordBreak: 'break-all',
                    marginBottom: '0.75rem',
                    padding: '0.35rem 0.5rem',
                    background: 'rgba(2, 6, 23, 0.04)',
                    borderRadius: '0.35rem',
                  }}
                >
                  {creationResult.password}
                </code>
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => void navigator.clipboard.writeText(creationResult.password)}
                  >
                    Copier le mot de passe
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => {
                      setShowCreateDrawer(false);
                      resetCreateUserForm();
                    }}
                  >
                    Terminer
                  </button>
                </div>
              </div>
            )}
            {!creationResult && (
              <form
                onSubmit={onCreateUser}
                className="card"
                style={{
                  display: 'grid',
                  gap: '0.9rem',
                  gridTemplateColumns: '1fr 1fr',
                  width: '100%',
                  maxWidth: '100%',
                  boxSizing: 'border-box',
                }}
              >
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="u-firstname">Prénom *</label>
                  <input id="u-firstname" value={createFirstName} onChange={(e) => setCreateFirstName(e.target.value)} required />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="u-lastname">Nom *</label>
                  <input id="u-lastname" value={createLastName} onChange={(e) => setCreateLastName(e.target.value)} required />
                </div>
                <div className="field" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
                  <label htmlFor="u-postname">Post-nom</label>
                  <input id="u-postname" value={createPostName} onChange={(e) => setCreatePostName(e.target.value)} />
                </div>
                <div className="field" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
                  <label htmlFor="u-suggested-user">Nom d&apos;utilisateur</label>
                  <input
                    id="u-suggested-user"
                    readOnly
                    value={suggestedUsername}
                    style={{ background: 'rgba(2, 6, 23, 0.04)', width: '100%' }}
                  />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="u-email">Email (optionnel)</label>
                  <input id="u-email" type="email" value={createEmail} onChange={(e) => setCreateEmail(e.target.value)} />
                </div>
                <div className="field" style={{ marginBottom: 0 }}>
                  <label htmlFor="u-role">Rôle *</label>
                  <select id="u-role" value={createRole} onChange={(e) => setCreateRole(e.target.value)} required>
                    {roles.length === 0 && <option value="">Chargement des rôles…</option>}
                    {roles.map((role) => (
                      <option key={role.id} value={role.name}>
                        {role.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="field" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
                  <label htmlFor="u-hospital-services-create">
                    Services hospitaliers (périmètre admissions — médecin·e·s & infirmier·ère·s)
                  </label>
                  <select
                    id="u-hospital-services-create"
                    multiple
                    value={createHospitalServiceIds.map(String)}
                    onChange={(e) => {
                      const opts = Array.from(e.target.selectedOptions);
                      setCreateHospitalServiceIds(opts.map((o) => Number(o.value)));
                    }}
                    style={{ width: '100%', minHeight: '7rem' }}
                  >
                    {hospitalServicesCatalog.map((s) => (
                      <option key={s.id} value={s.id}>
                        {s.name}
                      </option>
                    ))}
                  </select>
                  <span style={{ fontSize: '0.8rem', color: 'var(--muted)', display: 'block', marginTop: '0.25rem' }}>
                    Ctrl ou ⌘ pour plusieurs choix. Vide : pas de filtre par service (voir documentation périmètre).
                  </span>
                </div>
                <div className="field" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
                  <label htmlFor="u-pwd-len">Longueur du mot de passe</label>
                  <select
                    id="u-pwd-len"
                    value={createPasswordLength}
                    onChange={(e) => setCreatePasswordLength(Number(e.target.value) as 12 | 16)}
                    style={{ width: '100%', maxWidth: '280px' }}
                  >
                    <option value={12}>12 caractères</option>
                    <option value={16}>16 caractères</option>
                  </select>
                </div>
                <div style={{ gridColumn: '1 / -1' }}>
                  <label htmlFor="u-pwd-preview" style={{ display: 'block', marginBottom: '0.35rem' }}>
                    Mot de passe proposé
                  </label>
                  <div
                    style={{
                      display: 'flex',
                      gap: '0.5rem',
                      alignItems: 'stretch',
                      width: '100%',
                    }}
                  >
                    <input
                      id="u-pwd-preview"
                      readOnly
                      value={passwordPreview ?? ''}
                      placeholder={pwdSuggestLoading ? 'Génération…' : '—'}
                      style={{
                        flex: 1,
                        minWidth: 0,
                        height: '2.5rem',
                        boxSizing: 'border-box',
                        fontFamily: 'ui-monospace, monospace',
                        background: 'rgba(2, 6, 23, 0.04)',
                        borderRadius: '0.35rem',
                        border: '1px solid var(--border)',
                        padding: '0 0.65rem',
                      }}
                    />
                    <button
                      type="button"
                      className="btn btn-ghost"
                      title="Autre combinaison"
                      aria-label="Régénérer le mot de passe"
                      disabled={pwdSuggestLoading || !createFirstName.trim() || !createLastName.trim()}
                      onClick={() => void fetchPasswordSuggestion(passwordVariation + 1)}
                      style={{
                        flex: '0 0 2.75rem',
                        width: '2.75rem',
                        height: '2.5rem',
                        minWidth: '2.75rem',
                        boxSizing: 'border-box',
                        padding: 0,
                        display: 'inline-flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '1.35rem',
                        lineHeight: 1,
                        borderRadius: '0.35rem',
                        border: '1px solid var(--border)',
                      }}
                    >
                      ↻
                    </button>
                  </div>
                </div>
                <div
                  style={{
                    gridColumn: '1 / -1',
                    display: 'flex',
                    gap: '0.5rem',
                    flexWrap: 'wrap',
                    justifyContent: 'flex-start',
                    paddingTop: '0.15rem',
                  }}
                >
                  <button type="submit" className="btn btn-primary" disabled={submittingCreate || pwdSuggestLoading}>
                    {submittingCreate ? 'Création…' : 'Créer'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-ghost"
                    onClick={() => {
                      setShowCreateDrawer(false);
                      resetCreateUserForm();
                    }}
                    disabled={submittingCreate}
                  >
                    Annuler
                  </button>
                </div>
              </form>
            )}
          </aside>
        </>
      )}

      {credentialsModalOpen && (
        <>
          <div
            role="presentation"
            onClick={() => {
              setCredentialsModalOpen(false);
              setCredentialsPreview(null);
            }}
            style={{
              position: 'fixed',
              inset: 0,
              background: 'rgba(2, 6, 23, 0.45)',
              zIndex: 45,
            }}
          />
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="credentials-modal-title"
            style={{
              position: 'fixed',
              left: '50%',
              top: '50%',
              transform: 'translate(-50%, -50%)',
              width: 'min(92vw, 720px)',
              maxHeight: 'min(85vh, 640px)',
              background: 'var(--surface)',
              borderRadius: '0.5rem',
              border: '1px solid var(--border)',
              boxShadow: '0 18px 48px rgba(2, 6, 23, 0.28)',
              zIndex: 46,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
            }}
          >
            <div
              style={{
                padding: '0.75rem 1rem',
                borderBottom: '1px solid var(--border)',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: '0.5rem',
                flexWrap: 'wrap',
              }}
            >
              <strong id="credentials-modal-title">Fichier des comptes</strong>
              <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={credentialsDownloadBusy || !!credentialsPreview?.empty}
                  onClick={() => void handleDownloadFromModal()}
                >
                  {credentialsDownloadBusy ? 'Téléchargement…' : 'Télécharger TXT'}
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  disabled={credentialsCsvBusy || !!credentialsPreview?.empty}
                  onClick={() => void handleDownloadCsvFromModal()}
                >
                  {credentialsCsvBusy ? 'Téléchargement…' : 'Télécharger CSV'}
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  disabled={!credentialsPreview || credentialsPreview.empty}
                  onClick={() => credentialsPreview && printCredentialsContent(credentialsPreview.content)}
                >
                  Imprimer
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  disabled={credentialsDeleting || !credentialsPreview || credentialsPreview.empty}
                  onClick={() => void handleDeleteCredentialsFile()}
                >
                  {credentialsDeleting ? 'Suppression…' : 'Supprimer le fichier'}
                </button>
                <button
                  type="button"
                  className="btn btn-ghost"
                  onClick={() => {
                    setCredentialsModalOpen(false);
                    setCredentialsPreview(null);
                  }}
                >
                  Fermer
                </button>
              </div>
            </div>
            <div style={{ padding: '0.75rem 1rem', flex: 1, overflow: 'auto', minHeight: 0 }}>
              {credentialsPreviewLoading && <p style={{ color: 'var(--muted)', margin: 0 }}>Chargement…</p>}
              {!credentialsPreviewLoading && credentialsPreview && credentialsPreview.empty && (
                <p style={{ color: 'var(--muted)', margin: 0 }}>
                  Aucun fichier enregistré ou fichier vide. Il sera créé lors de la prochaine création d&apos;utilisateur.
                </p>
              )}
              {!credentialsPreviewLoading && credentialsPreview && !credentialsPreview.empty && (
                <>
                  <p style={{ margin: '0 0 0.5rem', fontSize: '0.85rem', color: 'var(--muted)' }}>
                    {credentialsPreview.lineCount} ligne(s) · {credentialsPreview.totalBytes} octet(s)
                    {credentialsPreview.truncated ? ' · aperçu tronqué' : ''}
                  </p>
                  <pre
                    style={{
                      margin: 0,
                      padding: '0.6rem',
                      background: 'rgba(2, 6, 23, 0.04)',
                      borderRadius: '0.35rem',
                      fontSize: '0.8rem',
                      whiteSpace: 'pre-wrap',
                      wordBreak: 'break-word',
                      maxHeight: 'min(52vh, 420px)',
                      overflow: 'auto',
                    }}
                  >
                    {credentialsPreview.content}
                  </pre>
                </>
              )}
            </div>
          </div>
        </>
      )}

      {editUserId != null && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <h3 style={{ marginTop: 0 }}>Modifier utilisateur #{editUserId}</h3>
          <form onSubmit={onUpdateUser} style={{ display: 'grid', gap: '0.75rem', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="u-edit-fullname">Nom complet *</label>
              <input id="u-edit-fullname" value={editFullName} onChange={(e) => setEditFullName(e.target.value)} required />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="u-edit-email">Email (optionnel)</label>
              <input id="u-edit-email" type="email" value={editEmail} onChange={(e) => setEditEmail(e.target.value)} />
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="u-edit-role">Rôle *</label>
              <select id="u-edit-role" value={editRole} onChange={(e) => setEditRole(e.target.value)} required>
                {roles.length === 0 && <option value="">Chargement des rôles…</option>}
                {roles.map((role) => (
                  <option key={role.id} value={role.name}>
                    {role.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ gridColumn: '1 / -1', marginBottom: 0 }}>
              <label htmlFor="u-hospital-services-edit">Services hospitaliers (périmètre admissions)</label>
              <select
                id="u-hospital-services-edit"
                multiple
                value={editHospitalServiceIds.map(String)}
                onChange={(e) => {
                  const opts = Array.from(e.target.selectedOptions);
                  setEditHospitalServiceIds(opts.map((o) => Number(o.value)));
                }}
                style={{ width: '100%', minHeight: '7rem' }}
              >
                {hospitalServicesCatalog.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="u-edit-password">Nouveau mot de passe (optionnel)</label>
              <input id="u-edit-password" type="password" value={editPassword} onChange={(e) => setEditPassword(e.target.value)} />
            </div>
            <div style={{ gridColumn: '1 / -1', display: 'flex', gap: '0.75rem' }}>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Traitement...' : 'Enregistrer'}
              </button>
              <button type="button" className="btn btn-ghost" onClick={() => setEditUserId(null)} disabled={submitting}>
                Annuler
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
