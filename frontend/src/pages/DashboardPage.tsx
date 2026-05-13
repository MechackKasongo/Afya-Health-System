import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import type {
  MetricResponse,
  PageAdmissionResponse,
  PageConsultationResponse,
  PageHospitalServiceResponse,
  PagePatientResponse,
  PageUrgenceResponse,
} from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';

type DashboardKpis = {
  patients: number | null;
  admissions: number | null;
  urgences: number | null;
  occupancyPercent: number | null;
  consultations: number | null;
  hospitalServices: number | null;
};

const emptyKpis: DashboardKpis = {
  patients: null,
  admissions: null,
  urgences: null,
  occupancyPercent: null,
  consultations: null,
  hospitalServices: null,
};

function IconUsers() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
    </svg>
  );
}

function IconBed() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M7 14c1.66 0 3-1.34 3-3S8.66 8 7 8s-3 1.34-3 3 1.34 3 3 3zm12-7H11v7H3V7H1v13h2v-2h18v2h2V9c0-1.1-.9-2-2-2z" />
    </svg>
  );
}

function IconAlert() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z" />
    </svg>
  );
}

function IconChart() {
  return (
    <svg className="dashboard-kpi-card__icon" width="48" height="48" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
    </svg>
  );
}

function IconSmUsers() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
    </svg>
  );
}

function IconSmBed() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M7 14c1.66 0 3-1.34 3-3S8.66 8 7 8s-3 1.34-3 3 1.34 3 3 3zm12-7H11v7H3V7H1v13h2v-2h18v2h2V9c0-1.1-.9-2-2-2z" />
    </svg>
  );
}

function IconSmAlert() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z" />
    </svg>
  );
}

function IconSmChart() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z" />
    </svg>
  );
}

function IconSmFolder() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z" />
    </svg>
  );
}

function IconSmBuilding() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 7V3H2v18h20V7H12zM6 19H4v-2h2v2zm0-4H4v-2h2v2zm0-4H4V9h2v2zm0-4H4V5h2v2zm4 12H8v-2h2v2zm0-4H8v-2h2v2zm0-4H8V9h2v2zm0-4H8V5h2v2zm10 12h-8v-2h2v-2h-2v-2h2v-2h-2V9h8v10zm-2-8h-2v2h2v-2zm0 4h-2v2h2v-2z" />
    </svg>
  );
}

function IconSmKey() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12.65 10A5.99 5.99 0 007 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 005.65-4H17v2h4v-2h2v-4H12.65zM7 15c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" />
    </svg>
  );
}

type QuickVariant = 'accent' | 'success' | 'warning' | 'deep' | 'danger';

function QuickTile({
  to,
  title,
  description,
  variant,
  icon,
}: {
  to: string;
  title: string;
  description: string;
  variant: QuickVariant;
  icon: ReactNode;
}) {
  return (
    <Link to={to} className={`dashboard-quick-tile dashboard-quick-tile--${variant}`}>
      <span className="dashboard-quick-tile__icon-wrap">{icon}</span>
      <div className="dashboard-quick-tile__body">
        <h3 className="dashboard-quick-tile__title">{title}</h3>
        <p className="dashboard-quick-tile__desc">{description}</p>
      </div>
      <span className="dashboard-quick-tile__chevron" aria-hidden>
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
          <path d="M9 6l6 6-6 6" />
        </svg>
      </span>
    </Link>
  );
}

function formatKpiValue(n: number | null, loading: boolean): string {
  if (loading) return '…';
  if (n === null) return '—';
  return String(n);
}

export function DashboardPage() {
  const { user } = useAuth();
  const isAdmin = hasRole(user, 'ROLE_ADMIN');
  const isReception = hasRole(user, 'ROLE_RECEPTION');
  const isMedecin = hasRole(user, 'ROLE_MEDECIN');
  const isInfirmier = hasRole(user, 'ROLE_INFIRMIER');

  const canPatients = isAdmin || isReception || isMedecin || isInfirmier;
  const canAdmissions = canPatients;
  const canUrgences = isAdmin || isMedecin || isInfirmier;
  const canConsultations = isAdmin || isMedecin || isInfirmier;

  const [kpis, setKpis] = useState<DashboardKpis>(emptyKpis);
  const [kpiLoading, setKpiLoading] = useState(true);
  const [now, setNow] = useState(() => new Date());

  const displayName = user?.fullName?.trim() || user?.username?.trim() || 'Utilisateur';

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 30000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    let cancelled = false;

    async function loadKpis() {
      setKpiLoading(true);
      const next: DashboardKpis = { ...emptyKpis };

      const tasks: Promise<void>[] = [];

      if (canPatients) {
        tasks.push(
          api
            .get<PagePatientResponse>('/api/v1/patients?page=0&size=1')
            .then((r) => {
              next.patients = r.data.totalElements;
            })
            .catch(() => {
              next.patients = null;
            }),
        );
      }

      if (canAdmissions) {
        tasks.push(
          api
            .get<PageAdmissionResponse>('/api/v1/admissions?page=0&size=1')
            .then((r) => {
              next.admissions = r.data.totalElements;
            })
            .catch(() => {
              next.admissions = null;
            }),
        );
      }

      if (canUrgences) {
        tasks.push(
          api
            .get<PageUrgenceResponse>('/api/v1/urgences?page=0&size=1')
            .then((r) => {
              next.urgences = r.data.totalElements;
            })
            .catch(() => {
              next.urgences = null;
            }),
        );
      }

      if (canConsultations && !isAdmin) {
        tasks.push(
          api
            .get<PageConsultationResponse>('/api/v1/consultations?page=0&size=1')
            .then((r) => {
              next.consultations = r.data.totalElements;
            })
            .catch(() => {
              next.consultations = null;
            }),
        );
      }

      if (isReception && !isAdmin) {
        tasks.push(
          api
            .get<PageHospitalServiceResponse>('/api/v1/hospital-services?page=0&size=1')
            .then((r) => {
              next.hospitalServices = r.data.totalElements;
            })
            .catch(() => {
              next.hospitalServices = null;
            }),
        );
      }

      if (isAdmin) {
        tasks.push(
          api
            .get<MetricResponse>('/api/v1/stats/occupancy')
            .then((r) => {
              const v = r.data.value;
              if (v && typeof v === 'object' && v !== null && 'overallRatePercent' in v) {
                next.occupancyPercent = Number((v as { overallRatePercent: number }).overallRatePercent);
              }
            })
            .catch(() => {
              next.occupancyPercent = null;
            }),
        );
      }

      await Promise.all(tasks);
      if (!cancelled) {
        setKpis(next);
        setKpiLoading(false);
      }
    }

    void loadKpis();
    return () => {
      cancelled = true;
    };
  }, [canPatients, canAdmissions, canUrgences, canConsultations, isAdmin, isReception]);

  const datetimeLabel = useMemo(
    () =>
      now.toLocaleString('fr-FR', {
        weekday: 'long',
        day: 'numeric',
        month: 'long',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }),
    [now],
  );

  const slot4 = useMemo(() => {
    if (isAdmin) {
      return {
        label: 'Occupation lits',
        value: kpiLoading ? '…' : kpis.occupancyPercent === null ? '—' : `${Math.round(kpis.occupancyPercent)}%`,
        hint: 'Taux global',
        to: '/reporting' as const,
        variant: 'deep' as const,
        icon: <IconChart />,
      };
    }
    if (isReception) {
      return {
        label: 'Services hospitaliers',
        value: formatKpiValue(kpis.hospitalServices, kpiLoading),
        hint: 'Référentiel',
        to: '/hospital-services' as const,
        variant: 'deep' as const,
        icon: <IconChart />,
      };
    }
    return {
      label: 'Consultations',
      value: formatKpiValue(kpis.consultations, kpiLoading),
      hint: 'Volume enregistré',
      to: '/consultations' as const,
      variant: 'deep' as const,
      icon: <IconChart />,
    };
  }, [isAdmin, isReception, kpis, kpiLoading]);

  return (
    <>
      <header className="dashboard-header">
        <div>
          <h1>Bon retour, {displayName} !</h1>
        </div>
        <div className="dashboard-datetime">{datetimeLabel}</div>
      </header>

      <div className="dashboard-kpi-row">
        <Link to="/patients" className="dashboard-kpi-card dashboard-kpi-card--accent">
          <IconUsers />
          <span className="dashboard-kpi-card__label">Patients</span>
          <span className="dashboard-kpi-card__value">{formatKpiValue(kpis.patients, kpiLoading)}</span>
          <span className="dashboard-kpi-card__hint">Enregistrements totaux</span>
        </Link>

        <Link to="/admissions" className="dashboard-kpi-card dashboard-kpi-card--success">
          <IconBed />
          <span className="dashboard-kpi-card__label">Admissions</span>
          <span className="dashboard-kpi-card__value">{formatKpiValue(kpis.admissions, kpiLoading)}</span>
          <span className="dashboard-kpi-card__hint">Toutes périodes confondues</span>
        </Link>

        {canUrgences ? (
          <Link to="/urgences" className="dashboard-kpi-card dashboard-kpi-card--warning">
            <IconAlert />
            <span className="dashboard-kpi-card__label">Passages urgences</span>
            <span className="dashboard-kpi-card__value">{formatKpiValue(kpis.urgences, kpiLoading)}</span>
            <span className="dashboard-kpi-card__hint">Volume des passages</span>
          </Link>
        ) : (
          <div className="dashboard-kpi-card dashboard-kpi-card--warning dashboard-kpi-card--static">
            <IconAlert />
            <span className="dashboard-kpi-card__label">Passages urgences</span>
            <span className="dashboard-kpi-card__value">—</span>
            <span className="dashboard-kpi-card__hint">Indicateur réservé au pôle soins (médecins / infirmiers)</span>
          </div>
        )}

        <Link to={slot4.to} className={`dashboard-kpi-card dashboard-kpi-card--${slot4.variant}`}>
          {slot4.icon}
          <span className="dashboard-kpi-card__label">{slot4.label}</span>
          <span className="dashboard-kpi-card__value">{slot4.value}</span>
          <span className="dashboard-kpi-card__hint">{slot4.hint}</span>
        </Link>
      </div>

      <h2 className="dashboard-section-title">Accès rapides</h2>
      <div className="dashboard-quick-grid">
        {(isAdmin || isReception) && (
          <QuickTile
            to="/patients"
            title="Patients"
            description="Registre administratif, recherche, dossier"
            variant="accent"
            icon={<IconSmUsers />}
          />
        )}
        {(isAdmin || isReception || isMedecin || isInfirmier) && (
          <QuickTile
            to="/admissions"
            title="Admissions & séjour"
            description="Liste des admissions avec filtres patient et statut"
            variant="success"
            icon={<IconSmBed />}
          />
        )}
        {(isAdmin || isMedecin || isInfirmier) && (
          <QuickTile
            to="/urgences"
            title="Urgences"
            description="Passages, triage, orientation et clôture"
            variant="warning"
            icon={<IconSmAlert />}
          />
        )}
        {(isAdmin || isMedecin || isInfirmier) && (
          <QuickTile
            to="/consultations"
            title="Consultations"
            description="Observations, diagnostics et demandes d&apos;examen"
            variant="deep"
            icon={<IconSmChart />}
          />
        )}
        {(isAdmin || isMedecin || isInfirmier) && (
          <QuickTile
            to="/medical-records"
            title="Dossiers médicaux"
            description="Allergies, antécédents, problèmes et documents"
            variant="accent"
            icon={<IconSmFolder />}
          />
        )}
        {(isAdmin || isReception) && (
          <QuickTile
            to="/hospital-services"
            title="Services hôpitaux"
            description="Référentiel des services hospitaliers"
            variant="success"
            icon={<IconSmBuilding />}
          />
        )}
        {isAdmin && (
          <>
            <QuickTile
              to="/users"
              title="Utilisateurs"
              description="Comptes, rôles et statuts"
              variant="danger"
              icon={<IconSmKey />}
            />
            <QuickTile
              to="/reporting"
              title="Reporting"
              description="KPI, audit et exports d&apos;activité / occupation"
              variant="deep"
              icon={<IconSmChart />}
            />
          </>
        )}
      </div>
    </>
  );
}
