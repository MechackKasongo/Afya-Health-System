import { type FormEvent, useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { getApiErrorMessage } from '../api/error';
import { useAuth } from '../auth/AuthContext';

function IconHeroBrand() {
  return (
    <svg className="login-hero__brand-mark" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M12 3c-4.97 0-9 4.03-9 9s4.03 9 9 9 9-4.03 9-9-4.03-9-9-9z"
        stroke="currentColor"
        strokeWidth="1.35"
      />
      <path d="M12 8v8M8 12h8" stroke="currentColor" strokeWidth="1.35" strokeLinecap="round" />
    </svg>
  );
}

function IconUser() {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} fill="currentColor" aria-hidden>
      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
    </svg>
  );
}

function IconLock() {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} fill="currentColor" aria-hidden>
      <path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z" />
    </svg>
  );
}

function IconEnter() {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} fill="currentColor" aria-hidden>
      <path d="M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 2h-2v9H6v-4h-2v5a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V10a1 1 0 0 0-1-1z" />
    </svg>
  );
}

function IconEye() {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} fill="currentColor" aria-hidden>
      <path d="M12 5c-5.45 0-9.72 4.14-11 7 1.28 2.86 5.55 7 11 7s9.72-4.14 11-7c-1.28-2.86-5.55-7-11-7zm0 11.5A4.5 4.5 0 1 1 12 7a4.5 4.5 0 0 1 0 9.5zm0-7.2a2.7 2.7 0 1 0 0 5.4 2.7 2.7 0 0 0 0-5.4z" />
    </svg>
  );
}

function IconEyeOff() {
  return (
    <svg viewBox="0 0 24 24" width={20} height={20} fill="currentColor" aria-hidden>
      <path d="M12 6c4.28 0 7.74 2.88 9.22 6-0.46.96-1.17 2.06-2.13 3.08l1.42 1.42 1.27-1.27-19-19L1.5 2.5l3.1 3.1A14.58 14.58 0 0 0 .78 12c1.28 2.86 5.55 7 11 7 2.17 0 4.11-.58 5.77-1.5l2.95 2.95 1.27-1.27-3.08-3.08A14.48 14.48 0 0 0 23.22 12c-1.28-2.86-5.55-7-11-7-.98 0-1.92.12-2.8.34l1.53 1.53c.41-.07.82-.11 1.25-.11zm-5.26 5.86 1.5 1.5A3.8 3.8 0 0 1 8.2 12c0-.36.06-.7.16-1.03l1.43 1.43a2.1 2.1 0 0 0 2.81 2.81l1.43 1.43c-.33.1-.67.16-1.03.16a3.8 3.8 0 0 1-3.8-3.8c0-.36.05-.7.14-1.04l-1.5-1.5zM12 8.2c2.1 0 3.8 1.7 3.8 3.8 0 .36-.06.7-.16 1.03l-1.43-1.43a2.1 2.1 0 0 0-2.81-2.81L9.97 7.36c.33-.1.67-.16 1.03-.16z" />
    </svg>
  );
}

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [usernameOrEmail, setUsernameOrEmail] = useState('admin');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  if (user) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setPending(true);
    try {
      await login(usernameOrEmail, password);
      navigate('/', { replace: true });
    } catch (err) {
      setError(getApiErrorMessage(err, 'Identifiants invalides ou serveur indisponible.'));
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="login-page">
      <section className="login-hero">
        <div className="login-hero-content">
          <div className="login-hero-brand">
            <IconHeroBrand />
            <h2>Afya Health</h2>
          </div>
          <p className="login-hero-tagline">Plateforme clinique moderne et sécurisée.</p>
        </div>
      </section>

      <section className="login-panel">
        <div className="login-card">
          <div className="login-card__header">
            <h1>Connexion</h1>
          </div>
          {error && <div className="error-banner login-card__error">{error}</div>}
          <form className="login-form" onSubmit={(e) => void onSubmit(e)}>
            <div className="field login-field">
              <label htmlFor="username">Nom d’utilisateur ou e-mail</label>
              <div className="input-with-icon login-input-wrap">
                <span aria-hidden className="input-icon">
                  <IconUser />
                </span>
                <input
                  id="username"
                  className="login-input"
                  autoComplete="username"
                  value={usernameOrEmail}
                  onChange={(e) => setUsernameOrEmail(e.target.value)}
                  required
                />
              </div>
            </div>
            <div className="field login-field">
              <label htmlFor="password">Mot de passe</label>
              <div className="input-with-icon login-input-wrap">
                <span aria-hidden className="input-icon">
                  <IconLock />
                </span>
                <input
                  id="password"
                  className="login-input"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                />
                <button
                  type="button"
                  className="login-password-toggle"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <IconEyeOff /> : <IconEye />}
                </button>
              </div>
            </div>
            <button type="submit" className="btn btn-primary login-submit" disabled={pending}>
              {pending ? (
                'Connexion…'
              ) : (
                <>
                  <IconEnter />
                  Se connecter
                </>
              )}
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
