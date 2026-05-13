import type { ReactNode } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import type { MeResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { hasRole } from '../auth/roles';

/** Ligne sous le titre : services affectés ou périmètre selon le rôle. */
function assignedServicesSubtitle(user: MeResponse): string {
  const names = user.hospitalServiceNames ?? [];
  if (names.length > 0) {
    return names.join(', ');
  }
  if (hasRole(user, 'ROLE_ADMIN') || hasRole(user, 'ROLE_RECEPTION')) {
    return 'Tous les services';
  }
  if (hasRole(user, 'ROLE_MEDECIN') || hasRole(user, 'ROLE_INFIRMIER')) {
    return 'Aucun service affecté';
  }
  return '—';
}

function NavIcon({ children }: { children: ReactNode }) {
  return (
    <span className="side-nav__glyph" aria-hidden>
      {children}
    </span>
  );
}

function IconDashboard() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
    </svg>
  );
}

function IconPatients() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" />
    </svg>
  );
}

function IconBed() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M7 14c1.66 0 3-1.34 3-3S8.66 8 7 8s-3 1.34-3 3 1.34 3 3 3zm12-7H11v7H3V7H1v13h2v-2h18v2h2V9c0-1.1-.9-2-2-2z" />
    </svg>
  );
}

function IconUrgence() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z" />
    </svg>
  );
}

function IconConsultation() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M19 3h-4.18C14.4 1.84 13.3 1 12 1c-1.3 0-2.4.84-2.82 2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 0c.55 0 1 .45 1 1s-.45 1-1 1-1-.45-1-1 .45-1 1-1zm2 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z" />
    </svg>
  );
}

function IconFolder() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z" />
    </svg>
  );
}

function IconBuilding() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M12 7V3H2v18h20V7H12zM6 19H4v-2h2v2zm0-4H4v-2h2v2zm0-4H4V9h2v2zm0-4H4V5h2v2zm4 12H8v-2h2v2zm0-4H8v-2h2v2zm0-4H8V9h2v2zm0-4H8V5h2v2zm10 12h-8v-2h2v-2h-2v-2h2v-2h-2V9h8v10zm-2-8h-2v2h2v-2zm0 4h-2v2h2v-2z" />
    </svg>
  );
}

function IconUsersAdmin() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M12.65 10A5.99 5.99 0 007 6c-3.31 0-6 2.69-6 6s2.69 6 6 6a5.99 5.99 0 005.65-4H17v2h4v-2h2v-4H12.65zM7 15c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2z" />
    </svg>
  );
}

function IconReporting() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor">
      <path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM7 10h2v7H7zm4-3h2v10h-2zm4-6h2v16h-2z" />
    </svg>
  );
}

function IconSettings() {
  return (
    <svg viewBox="0 0 24 24" width={18} height={18} fill="currentColor" aria-hidden>
      <path d="M19.14 12.94c.04-.31.06-.63.06-.94 0-.31-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z" />
    </svg>
  );
}

function IconBrand() {
  return (
    <svg className="nav-brand__mark" viewBox="0 0 24 24" width={22} height={22} fill="none" aria-hidden>
      <path
        d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9-4.03-9-9-9z"
        stroke="currentColor"
        strokeWidth="1.75"
      />
      <path d="M12 8v8M8 12h8" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" />
    </svg>
  );
}

export function Layout() {
  const { user, logout } = useAuth();
  const isAdmin = hasRole(user, 'ROLE_ADMIN');
  const isReception = hasRole(user, 'ROLE_RECEPTION');
  const isMedecin = hasRole(user, 'ROLE_MEDECIN');
  const isInfirmier = hasRole(user, 'ROLE_INFIRMIER');

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <NavLink to="/" className="nav-brand">
          <IconBrand />
          <span>Afya Health</span>
        </NavLink>
        <nav className="side-nav side-nav--main">
          <NavLink to="/" end>
            <NavIcon>
              <IconDashboard />
            </NavIcon>
            Tableau de bord
          </NavLink>
          {(isAdmin || isReception) && (
            <NavLink to="/patients">
              <NavIcon>
                <IconPatients />
              </NavIcon>
              Patients
            </NavLink>
          )}
          {(isAdmin || isReception || isMedecin || isInfirmier) && (
            <NavLink to="/admissions">
              <NavIcon>
                <IconBed />
              </NavIcon>
              Admissions
            </NavLink>
          )}
          {(isAdmin || isMedecin || isInfirmier) && (
            <NavLink to="/urgences">
              <NavIcon>
                <IconUrgence />
              </NavIcon>
              Urgences
            </NavLink>
          )}
          {(isAdmin || isMedecin || isInfirmier) && (
            <NavLink to="/consultations">
              <NavIcon>
                <IconConsultation />
              </NavIcon>
              Consultations
            </NavLink>
          )}
          {(isAdmin || isMedecin || isInfirmier) && (
            <NavLink to="/medical-records">
              <NavIcon>
                <IconFolder />
              </NavIcon>
              Dossiers medicaux
            </NavLink>
          )}
          {(isAdmin || isReception) && (
            <div className="side-nav-admin">
              <NavLink to="/hospital-services">
                <NavIcon>
                  <IconBuilding />
                </NavIcon>
                Services hopitaux
              </NavLink>
              {isAdmin && (
                <>
                  <NavLink to="/users">
                    <NavIcon>
                      <IconUsersAdmin />
                    </NavIcon>
                    Utilisateurs
                  </NavLink>
                  <NavLink to="/reporting">
                    <NavIcon>
                      <IconReporting />
                    </NavIcon>
                    Reporting
                  </NavLink>
                </>
              )}
            </div>
          )}
        </nav>
        <nav className="side-nav side-nav--footer" aria-label="Paramètres">
          <NavLink to="/settings">
            <NavIcon>
              <IconSettings />
            </NavIcon>
            Paramètres
          </NavLink>
        </nav>
      </aside>

      <section className="main-area">
        <header className="top-bar">
          <div className="top-bar-brand">
            <div className="top-bar-title">Plateforme clinique</div>
            {user && (
              <div className="top-bar-service-line" title={assignedServicesSubtitle(user)}>
                {assignedServicesSubtitle(user)}
              </div>
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap', justifyContent: 'flex-end' }}>
            {user && (
              <>
                <span className="nav-user">{user.fullName}</span>
                <button type="button" className="btn btn-danger" onClick={() => void logout()}>
                  Déconnexion
                </button>
              </>
            )}
          </div>
        </header>
        <main className="app-shell">
          <Outlet />
        </main>
      </section>
    </div>
  );
}
