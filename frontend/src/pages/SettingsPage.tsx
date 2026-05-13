import { Link } from 'react-router-dom';
import { FontPreferenceSelect } from '../components/FontPreferenceSelect';
import { ThemePreferenceSelect } from '../components/ThemePreferenceSelect';
import { THEME_OPTIONS } from '../ui/themePreference';

export function SettingsPage() {
  return (
    <>
      <h1 className="page-title">Paramètres</h1>
      <p style={{ color: 'var(--muted)', marginTop: 0, marginBottom: '1.25rem' }}>
        <Link to="/">Retour au tableau de bord</Link>
      </p>

      <section className="card settings-card">
        <h2 style={{ marginTop: 0 }}>Apparence</h2>

        <div className="settings-appearance-fields">
          <div className="settings-field-block">
            <h3 className="settings-field-block__title">Thème</h3>
            <ThemePreferenceSelect selectId="ui-theme-settings" />
            <ul className="settings-hint-list">
              {THEME_OPTIONS.map((t) => (
                <li key={t.id}>
                  <strong>{t.label}</strong> — {t.description}
                </li>
              ))}
            </ul>
          </div>

          <div className="settings-field-block">
            <h3 className="settings-field-block__title">Police</h3>
            <FontPreferenceSelect selectId="ui-font-settings" />
            <p className="settings-field-block__hint">
              Les familles proposées couvrent l’interface entière : « Merriweather » pour une lecture prolongée,
              « Atkinson Hyperlegible » pour le confort visuel, « Police système » sans téléchargement de fichiers.
            </p>
          </div>
        </div>
      </section>
    </>
  );
}
