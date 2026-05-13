import { useEffect, useState } from 'react';
import { api } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { HospitalServiceRequest, HospitalServiceResponse, PageHospitalServiceResponse } from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';
import { LIST_FETCH_PAGE_SIZE } from '../utils/listFetch';

export function HospitalServicesPage() {
  const [page, setPage] = useState<PageHospitalServiceResponse | null>(null);
  const [name, setName] = useState('');
  const [bedCapacity, setBedCapacity] = useState('');
  const [editId, setEditId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editBedCapacity, setEditBedCapacity] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    void loadServices();
  }, []);

  async function loadServices() {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({ page: '0', size: String(LIST_FETCH_PAGE_SIZE) });
      const { data } = await api.get<PageHospitalServiceResponse>(`/api/v1/hospital-services?${params}`);
      setPage(data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger les services hospitaliers.'));
    } finally {
      setLoading(false);
    }
  }

  async function onCreate(e: React.FormEvent) {
    e.preventDefault();
    const beds = Number.parseInt(bedCapacity, 10);
    if (!name.trim() || !Number.isFinite(beds) || beds <= 0) {
      setError('Nom du service et nombre de lits (> 0) requis.');
      return;
    }
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const payload: HospitalServiceRequest = { name: name.trim(), bedCapacity: beds };
      await api.post('/api/v1/hospital-services', payload);
      setName('');
      setBedCapacity('');
      setMessage('Service hospitalier créé.');
      await loadServices();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de créer le service hospitalier.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function onUpdate(e: React.FormEvent) {
    e.preventDefault();
    const beds = Number.parseInt(editBedCapacity, 10);
    if (editId == null || !editName.trim() || !Number.isFinite(beds) || beds <= 0) return;
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      const payload: HospitalServiceRequest = { name: editName.trim(), bedCapacity: beds };
      await api.put(`/api/v1/hospital-services/${editId}`, payload);
      setEditId(null);
      setEditName('');
      setEditBedCapacity('');
      setMessage('Service hospitalier modifié.');
      await loadServices();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de modifier le service hospitalier.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function toggleActive(item: HospitalServiceResponse) {
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.patch(`/api/v1/hospital-services/${item.id}/status`, { active: !item.active });
      setMessage(item.active ? 'Service désactivé.' : 'Service activé.');
      await loadServices();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de changer le statut.'));
    } finally {
      setSubmitting(false);
    }
  }

  async function remove(item: HospitalServiceResponse) {
    if (!window.confirm(`Supprimer le service "${item.name}" ?`)) return;
    setSubmitting(true);
    setError(null);
    setMessage(null);
    try {
      await api.delete(`/api/v1/hospital-services/${item.id}`);
      setMessage('Service supprimé.');
      await loadServices();
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de supprimer le service.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      {message && <div className="card" style={{ marginBottom: '1rem', borderColor: 'rgba(61,154,237,0.5)' }}>{message}</div>}
      {error && <div className="error-banner">{error}</div>}

      <div className="card" style={{ marginBottom: '1rem' }}>
        <h3 style={{ marginTop: 0 }}>Créer un service</h3>
        <form onSubmit={onCreate} style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
          <div className="field" style={{ flex: '1 1 260px', marginBottom: 0 }}>
            <label htmlFor="hs-name">Nom du service *</label>
            <input id="hs-name" value={name} onChange={(e) => setName(e.target.value)} required />
          </div>
          <div className="field" style={{ flex: '0 0 180px', marginBottom: 0 }}>
            <label htmlFor="hs-beds">Nombre de lits *</label>
            <input id="hs-beds" type="number" min={1} value={bedCapacity} onChange={(e) => setBedCapacity(e.target.value)} required />
          </div>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Traitement...' : 'Créer'}
          </button>
        </form>
      </div>

      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && page && (
        <div className="card table-wrap">
          <ScrollTableRegion>
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Service</th>
                  <th>Lits</th>
                  <th>Statut</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {page.content.length === 0 ? (
                  <tr><td colSpan={5} style={{ color: 'var(--muted)' }}>Aucun service.</td></tr>
                ) : (
                  page.content.map((item) => (
                    <tr key={item.id}>
                      <td>{item.id}</td>
                      <td>{item.name}</td>
                      <td>{item.bedCapacity}</td>
                      <td>{item.active ? 'Actif' : 'Inactif'}</td>
                      <td>
                        <button
                          type="button"
                          className="btn btn-ghost"
                          onClick={() => {
                            setEditId(item.id);
                            setEditName(item.name);
                            setEditBedCapacity(String(item.bedCapacity));
                          }}
                        >
                          Modifier
                        </button>{' '}
                        <button type="button" className="btn btn-ghost" onClick={() => void toggleActive(item)} disabled={submitting}>
                          {item.active ? 'Désactiver' : 'Activer'}
                        </button>{' '}
                        <button type="button" className="btn btn-danger" onClick={() => void remove(item)} disabled={submitting}>
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
            itemLabelPlural="service(s)"
          />
        </div>
      )}

      {editId != null && (
        <div className="card" style={{ marginTop: '1rem' }}>
          <h3 style={{ marginTop: 0 }}>Modifier service #{editId}</h3>
          <form onSubmit={onUpdate} style={{ display: 'flex', gap: '0.75rem', alignItems: 'flex-end', flexWrap: 'wrap' }}>
            <div className="field" style={{ flex: '1 1 260px', marginBottom: 0 }}>
              <label htmlFor="hs-edit-name">Nom du service *</label>
              <input id="hs-edit-name" value={editName} onChange={(e) => setEditName(e.target.value)} required />
            </div>
            <div className="field" style={{ flex: '0 0 180px', marginBottom: 0 }}>
              <label htmlFor="hs-edit-beds">Nombre de lits *</label>
              <input
                id="hs-edit-beds"
                type="number"
                min={1}
                value={editBedCapacity}
                onChange={(e) => setEditBedCapacity(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Traitement...' : 'Enregistrer'}
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => setEditId(null)} disabled={submitting}>
              Annuler
            </button>
          </form>
        </div>
      )}
    </>
  );
}
