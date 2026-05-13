import { useEffect, useMemo, useState } from 'react';
import { api, getStoredAccessToken } from '../api/client';
import { getApiErrorMessage } from '../api/error';
import type { ExportResponse, MetricResponse } from '../api/types';
import { ScrollTableRegion, TableResultFooter } from '../components/ScrollTableRegion';

type OccupancyByServiceRow = {
  serviceName: string;
  occupiedBeds: number;
  bedCapacity: number;
  occupancyRatePercent: number;
};

type OccupancySummary = {
  overallRatePercent: number;
  occupiedBeds: number;
  totalBeds: number;
};

type AverageLengthOfStay = {
  days: number;
  closedAdmissions: number;
};

function occupancyTone(rate: number): string {
  if (rate >= 85) return '#e85d6a';
  if (rate >= 60) return '#f4a261';
  return '#2ec4b6';
}

function toNumber(v: unknown): number | null {
  if (typeof v === 'number' && Number.isFinite(v)) return v;
  if (typeof v === 'string' && v.trim() !== '') {
    const parsed = Number(v);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function formatNumberFr(value: number): string {
  return new Intl.NumberFormat('fr-FR').format(value);
}

type ReportingMetrics = {
  occupancy: MetricResponse;
  occupancyByService: MetricResponse;
  admissions: MetricResponse;
  urgences: MetricResponse;
  averageLengthOfStay: MetricResponse;
};

type AuditEvent = {
  id: number;
  source: string;
  entityType: string;
  entityId: number;
  eventType: string;
  message: string | null;
  createdAt: string;
};

type ReportingPeriod = '7' | '30' | '90' | 'all';

type AuditSortColumn = 'createdAt' | 'source' | 'eventType' | 'entity' | 'details';

function AuditSortArrows({ active, dir }: { active: boolean; dir: 'asc' | 'desc' }) {
  return (
    <span className="audit-sort-arrows" aria-hidden>
      <svg
        viewBox="0 0 24 24"
        width={11}
        height={11}
        className={active && dir === 'asc' ? 'audit-sort-arrows__tri audit-sort-arrows__tri--on' : 'audit-sort-arrows__tri'}
      >
        <path fill="currentColor" d="M12 6l-6 7h12z" />
      </svg>
      <svg
        viewBox="0 0 24 24"
        width={11}
        height={11}
        className={active && dir === 'desc' ? 'audit-sort-arrows__tri audit-sort-arrows__tri--on' : 'audit-sort-arrows__tri'}
      >
        <path fill="currentColor" d="M12 18l6-7H6z" />
      </svg>
    </span>
  );
}

export function ReportingPage() {
  const [metrics, setMetrics] = useState<ReportingMetrics | null>(null);
  const [auditEvents, setAuditEvents] = useState<AuditEvent[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [exportMessage, setExportMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [period, setPeriod] = useState<ReportingPeriod>('30');

  /** Filtres audit : source + plage de dates uniquement. */
  const [auditFilterSource, setAuditFilterSource] = useState('');
  const [auditFilterDateFrom, setAuditFilterDateFrom] = useState('');
  const [auditFilterDateTo, setAuditFilterDateTo] = useState('');
  const [auditSortColumn, setAuditSortColumn] = useState<AuditSortColumn>('createdAt');
  const [auditSortDir, setAuditSortDir] = useState<'asc' | 'desc'>('desc');

  useEffect(() => {
    void loadData();
  }, [period]);

  const daysParam = period === 'all' ? null : Number(period);
  const withDays = (path: string) => (daysParam ? `${path}?days=${daysParam}` : path);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      const [occupancy, occupancyByService, admissions, urgences, averageLengthOfStay, auditEvents] = await Promise.all([
        api.get<MetricResponse>('/api/v1/stats/occupancy'),
        api.get<MetricResponse>('/api/v1/stats/occupancy-by-service'),
        api.get<MetricResponse>(withDays('/api/v1/stats/admissions')),
        api.get<MetricResponse>(withDays('/api/v1/stats/urgences')),
        api.get<MetricResponse>(withDays('/api/v1/stats/average-length-of-stay')),
        api.get<AuditEvent[]>(withDays('/api/v1/audit/events')),
      ]);

      setMetrics({
        occupancy: occupancy.data,
        occupancyByService: occupancyByService.data,
        admissions: admissions.data,
        urgences: urgences.data,
        averageLengthOfStay: averageLengthOfStay.data,
      });
      setAuditEvents(auditEvents.data);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Impossible de charger le reporting.'));
    } finally {
      setLoading(false);
    }
  }

  async function triggerExport(path: string) {
    setError(null);
    setExportMessage(null);
    try {
      const endpoint = withDays(path);
      const { data } = await api.post<ExportResponse>(endpoint);
      const fileName = data.reportType === 'occupancy' ? 'occupancy-report.csv' : 'activity-report.csv';
      await downloadExportCsv(data.downloadUrl, fileName);
      const reportLabel = data.reportType === 'occupancy' ? 'occupation' : 'activité';
      setExportMessage(`Export ${reportLabel} prêt. Téléchargement démarré.`);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible de lancer l'export."));
    }
  }

  async function downloadExportCsv(downloadUrl: string, fileName: string) {
    const token = getStoredAccessToken();
    const base = api.defaults.baseURL ?? '';
    const absolute = downloadUrl.startsWith('http') ? downloadUrl : `${base}${downloadUrl}`;
    const res = await fetch(absolute, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    });
    if (!res.ok) {
      throw new Error('Téléchargement impossible');
    }
    const blob = await res.blob();
    const href = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = href;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(href);
  }

  useEffect(() => {
    if (!exportMessage) return;
    const timer = window.setTimeout(() => {
      setExportMessage(null);
    }, 2400);
    return () => window.clearTimeout(timer);
  }, [exportMessage]);

  const occupancyByServiceRows = useMemo(() => {
    const v = metrics?.occupancyByService?.value;
    return Array.isArray(v) ? (v as OccupancyByServiceRow[]) : [];
  }, [metrics]);

  const occupancySummary = useMemo(() => {
    const raw = metrics?.occupancy?.value;
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
      const rec = raw as Record<string, unknown>;
      const overallRatePercent = toNumber(rec.overallRatePercent) ?? 0;
      const occupiedBeds = toNumber(rec.occupiedBeds) ?? 0;
      const totalBeds = toNumber(rec.totalBeds) ?? 0;
      return { overallRatePercent, occupiedBeds, totalBeds } as OccupancySummary;
    }
    return null;
  }, [metrics]);

  const admissionsCount = useMemo(() => toNumber(metrics?.admissions?.value) ?? 0, [metrics]);
  const urgencesCount = useMemo(() => toNumber(metrics?.urgences?.value) ?? 0, [metrics]);

  const averageLengthOfStay = useMemo(() => {
    const raw = metrics?.averageLengthOfStay?.value;
    if (raw && typeof raw === 'object' && !Array.isArray(raw)) {
      const rec = raw as Record<string, unknown>;
      return {
        days: toNumber(rec.days) ?? 0,
        closedAdmissions: toNumber(rec.closedAdmissions) ?? 0,
      } as AverageLengthOfStay;
    }
    return null;
  }, [metrics]);

  const filteredAuditEvents = useMemo(() => {
    if (!auditEvents?.length) return auditEvents;
    const fromTs = auditFilterDateFrom
      ? new Date(`${auditFilterDateFrom}T00:00:00`).getTime()
      : null;
    const toTs = auditFilterDateTo ? new Date(`${auditFilterDateTo}T23:59:59.999`).getTime() : null;

    return auditEvents.filter((ev) => {
      if (auditFilterSource && ev.source !== auditFilterSource) return false;
      if (fromTs !== null || toTs !== null) {
        const t = new Date(ev.createdAt).getTime();
        if (fromTs !== null && t < fromTs) return false;
        if (toTs !== null && t > toTs) return false;
      }
      return true;
    });
  }, [auditEvents, auditFilterSource, auditFilterDateFrom, auditFilterDateTo]);

  const sortedAuditEvents = useMemo(() => {
    if (!filteredAuditEvents?.length) return filteredAuditEvents;
    const dir = auditSortDir === 'asc' ? 1 : -1;
    const col = auditSortColumn;
    const copy = [...filteredAuditEvents];
    copy.sort((a, b) => {
      let cmp = 0;
      if (col === 'createdAt') {
        cmp = new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
      } else if (col === 'source') {
        cmp = a.source.localeCompare(b.source, 'fr');
      } else if (col === 'eventType') {
        cmp = a.eventType.localeCompare(b.eventType, 'fr');
      } else if (col === 'entity') {
        const sa = `${a.entityType} ${a.entityId}`;
        const sb = `${b.entityType} ${b.entityId}`;
        cmp = sa.localeCompare(sb, 'fr', { numeric: true });
      } else {
        cmp = (a.message ?? '').localeCompare(b.message ?? '', 'fr');
      }
      return cmp * dir;
    });
    return copy;
  }, [filteredAuditEvents, auditSortColumn, auditSortDir]);

  function toggleAuditSort(column: AuditSortColumn) {
    if (auditSortColumn === column) {
      setAuditSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setAuditSortColumn(column);
      setAuditSortDir(column === 'createdAt' ? 'desc' : 'asc');
    }
  }

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.85rem' }}>
        <label htmlFor="reporting-period" style={{ color: 'var(--muted)', fontSize: '0.92rem' }}>
          Période
        </label>
        <select
          id="reporting-period"
          value={period}
          onChange={(e) => setPeriod(e.target.value as ReportingPeriod)}
          style={{ minWidth: '9.5rem' }}
        >
          <option value="7">7 derniers jours</option>
          <option value="30">30 derniers jours</option>
          <option value="90">90 derniers jours</option>
          <option value="all">Depuis le début</option>
        </select>
        <button type="button" className="btn btn-ghost" onClick={() => void loadData()} disabled={loading}>
          {loading ? 'Actualisation…' : 'Actualiser'}
        </button>
      </div>
      <p style={{ color: 'var(--muted)', marginTop: 0, marginBottom: '1rem' }}>
        Tableau de synthèse KPI + déclenchement exports.
      </p>
      {exportMessage && (
        <div
          style={{
            marginBottom: '1rem',
            color: 'var(--muted)',
            fontSize: '0.9rem',
            background: 'rgba(46, 196, 182, 0.08)',
            border: '1px solid rgba(46, 196, 182, 0.35)',
            borderRadius: '8px',
            padding: '0.55rem 0.75rem',
            display: 'inline-block',
          }}
        >
          {exportMessage}
        </div>
      )}
      {error && <div className="error-banner">{error}</div>}
      {loading && <p style={{ color: 'var(--muted)' }}>Chargement…</p>}

      {!loading && metrics && (
        <>
          <div className="reporting-kpi-row">
            <div className="tile-link reporting-kpi-card" style={{ cursor: 'default' }}>
              <h3>Occupation globale</h3>
              <p>
                {occupancySummary
                  ? `${formatNumberFr(occupancySummary.overallRatePercent)}% · ${formatNumberFr(occupancySummary.occupiedBeds)} / ${formatNumberFr(occupancySummary.totalBeds)}`
                  : '-'}
              </p>
            </div>
            <div className="tile-link reporting-kpi-card" style={{ cursor: 'default' }}>
              <h3>Occupation lits</h3>
              <p>{occupancySummary ? `${formatNumberFr(occupancySummary.overallRatePercent)}%` : '-'}</p>
            </div>
            <div className="tile-link reporting-kpi-card" style={{ cursor: 'default' }}>
              <h3>Admissions</h3>
              <p>{formatNumberFr(admissionsCount)}</p>
            </div>
            <div className="tile-link reporting-kpi-card" style={{ cursor: 'default' }}>
              <h3>Urgences</h3>
              <p>{formatNumberFr(urgencesCount)}</p>
            </div>
            <div className="tile-link reporting-kpi-card" style={{ cursor: 'default' }}>
              <h3>Durée moyenne séjour</h3>
              <p>
                {averageLengthOfStay ? `${formatNumberFr(averageLengthOfStay.days)} j` : '-'}
                {averageLengthOfStay ? (
                  <span style={{ color: 'var(--muted)', marginLeft: '0.4rem' }}>
                    ({formatNumberFr(averageLengthOfStay.closedAdmissions)} dossiers clos)
                  </span>
                ) : null}
              </p>
            </div>
          </div>

          <div className="reporting-bottom-row">
            <div className="card table-wrap" style={{ marginTop: '1rem' }}>
              <h3 style={{ marginTop: 0 }}>Occupation par service</h3>
              <ScrollTableRegion>
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Service</th>
                      <th>Occupés</th>
                      <th>Capacité lits</th>
                      <th>Taux (%)</th>
                    </tr>
                  </thead>
                  <tbody>
                    {occupancyByServiceRows.length === 0 ? (
                      <tr>
                        <td colSpan={4} style={{ color: 'var(--muted)' }}>
                          Aucune donnée de service disponible.
                        </td>
                      </tr>
                    ) : (
                      occupancyByServiceRows.map((row) => (
                        <tr key={row.serviceName}>
                          <td>{row.serviceName}</td>
                          <td>{row.occupiedBeds}</td>
                          <td>{row.bedCapacity}</td>
                          <td>
                            <span
                              style={{
                                display: 'inline-block',
                                minWidth: '4.5rem',
                                textAlign: 'center',
                                borderRadius: '999px',
                                padding: '0.15rem 0.55rem',
                                fontWeight: 700,
                                color: '#fff',
                                background: occupancyTone(row.occupancyRatePercent),
                              }}
                            >
                              {row.occupancyRatePercent}%
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </ScrollTableRegion>
              <TableResultFooter
                totalElements={occupancyByServiceRows.length}
                displayedCount={occupancyByServiceRows.length}
                itemLabelPlural="service(s)"
              />
            </div>

            <div className="card" style={{ marginTop: '1rem', marginBottom: '1rem' }}>
              <h3 style={{ marginTop: 0 }}>Exports</h3>
              <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                <button type="button" className="btn btn-primary" onClick={() => void triggerExport('/api/v1/exports/activity-report')}>
                  Export activité
                </button>
                <button type="button" className="btn btn-primary" onClick={() => void triggerExport('/api/v1/exports/occupancy-report')}>
                  Export occupation
                </button>
              </div>
            </div>
          </div>

          <div className="card">
            <h3 style={{ marginTop: 0 }}>Audit</h3>
            {auditEvents === null ? (
              <p style={{ margin: '0.35rem 0', color: 'var(--muted)' }}>Chargement…</p>
            ) : auditEvents.length === 0 ? (
              <p style={{ margin: '0.35rem 0', color: 'var(--muted)' }}>Aucun événement d'audit pour le moment.</p>
            ) : (
              <>
                <div className="audit-filter-row">
                  <div className="field audit-filter-row__field">
                    <label htmlFor="audit-filter-source">Source</label>
                    <select
                      id="audit-filter-source"
                      value={auditFilterSource}
                      onChange={(e) => setAuditFilterSource(e.target.value)}
                    >
                      <option value="">Toutes</option>
                      <option value="URGENCES">URGENCES</option>
                      <option value="ADMISSIONS">ADMISSIONS</option>
                    </select>
                  </div>
                  <div className="field audit-filter-row__field">
                    <label htmlFor="audit-filter-from">Date du</label>
                    <input
                      id="audit-filter-from"
                      type="date"
                      value={auditFilterDateFrom}
                      onChange={(e) => setAuditFilterDateFrom(e.target.value)}
                    />
                  </div>
                  <div className="field audit-filter-row__field">
                    <label htmlFor="audit-filter-to">Date au</label>
                    <input
                      id="audit-filter-to"
                      type="date"
                      value={auditFilterDateTo}
                      onChange={(e) => setAuditFilterDateTo(e.target.value)}
                    />
                  </div>
                </div>
                {filteredAuditEvents?.length === 0 ? (
                  <p style={{ margin: '0.35rem 0', color: 'var(--muted)' }}>
                    Aucun événement ne correspond aux filtres.
                  </p>
                ) : (
                  <div className="table-wrap">
                    <ScrollTableRegion>
                      <table className="data-table">
                        <thead>
                          <tr>
                            <th>
                              <button
                                type="button"
                                className="audit-th-sort"
                                aria-label="Trier par date"
                                onClick={() => toggleAuditSort('createdAt')}
                              >
                                Date
                                <AuditSortArrows
                                  active={auditSortColumn === 'createdAt'}
                                  dir={auditSortDir}
                                />
                              </button>
                            </th>
                            <th>
                              <button
                                type="button"
                                className="audit-th-sort"
                                aria-label="Trier par source"
                                onClick={() => toggleAuditSort('source')}
                              >
                                Source
                                <AuditSortArrows active={auditSortColumn === 'source'} dir={auditSortDir} />
                              </button>
                            </th>
                            <th>
                              <button
                                type="button"
                                className="audit-th-sort"
                                aria-label="Trier par action"
                                onClick={() => toggleAuditSort('eventType')}
                              >
                                Action
                                <AuditSortArrows active={auditSortColumn === 'eventType'} dir={auditSortDir} />
                              </button>
                            </th>
                            <th>
                              <button
                                type="button"
                                className="audit-th-sort"
                                aria-label="Trier par entité"
                                onClick={() => toggleAuditSort('entity')}
                              >
                                Entité
                                <AuditSortArrows active={auditSortColumn === 'entity'} dir={auditSortDir} />
                              </button>
                            </th>
                            <th>
                              <button
                                type="button"
                                className="audit-th-sort"
                                aria-label="Trier par détails"
                                onClick={() => toggleAuditSort('details')}
                              >
                                Détails
                                <AuditSortArrows active={auditSortColumn === 'details'} dir={auditSortDir} />
                              </button>
                            </th>
                          </tr>
                        </thead>
                        <tbody>
                          {(sortedAuditEvents ?? []).map((ev) => (
                            <tr key={`${ev.source}-${ev.id}-${ev.entityType}-${ev.entityId}`}>
                              <td>
                                {ev.createdAt ? new Date(ev.createdAt).toLocaleString('fr-FR') : '-'}
                              </td>
                              <td>{ev.source}</td>
                              <td>{ev.eventType}</td>
                              <td>
                                {ev.entityType} #{ev.entityId}
                              </td>
                              <td style={{ color: 'var(--muted)' }}>
                                {ev.message && ev.message.trim() !== '' ? ev.message : '-'}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </ScrollTableRegion>
                    <TableResultFooter
                      totalElements={auditEvents.length}
                      displayedCount={sortedAuditEvents?.length ?? 0}
                      itemLabelPlural="événement(s)"
                    />
                  </div>
                )}
              </>
            )}
          </div>
        </>
      )}
    </>
  );
}
