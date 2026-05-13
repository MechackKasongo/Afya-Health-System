import { useEffect, useState } from 'react';
import { applyThemePreference, getStoredThemeId, THEME_OPTIONS, type ThemeOptionId } from '../ui/themePreference';

type Props = {
  selectId?: string;
};

export function ThemePreferenceSelect({ selectId = 'ui-theme' }: Props) {
  const [themeId, setThemeId] = useState<ThemeOptionId>(() => getStoredThemeId());

  useEffect(() => {
    applyThemePreference(themeId);
  }, [themeId]);

  return (
    <label className="font-preference" htmlFor={selectId}>
      <span className="font-preference__label">Thème</span>
      <select
        id={selectId}
        className="font-preference__select"
        value={themeId}
        onChange={(e) => setThemeId(e.target.value as ThemeOptionId)}
        title="Thème d’affichage (clair ou sombre)"
      >
        {THEME_OPTIONS.map((opt) => (
          <option key={opt.id} value={opt.id}>
            {opt.label}
          </option>
        ))}
      </select>
    </label>
  );
}
