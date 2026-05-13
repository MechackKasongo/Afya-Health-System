/**
 * Thèmes d’interface : attribut `data-theme` sur `<html>` + localStorage.
 */

export const THEME_STORAGE_KEY = 'afya-ui-theme';

export type ThemeOptionId = 'light' | 'dark' | 'soft' | 'contrast';

export type ThemeOption = {
  id: ThemeOptionId;
  label: string;
  /** Court texte d’aide pour l’écran Paramètres */
  description: string;
};

export const THEME_OPTIONS: ThemeOption[] = [
  {
    id: 'light',
    label: 'Clair',
    description: 'Couleurs par défaut, fond clair et barre latérale bleu nuit.',
  },
  {
    id: 'dark',
    label: 'Sombre',
    description: 'Réduit la luminosité pour un usage en faible lumière.',
  },
  {
    id: 'soft',
    label: 'Doux',
    description: 'Tons verts/gris doux, moins fatigants pour de longues sessions.',
  },
  {
    id: 'contrast',
    label: 'Contraste élevé',
    description: 'Texte et bordures renforcés pour une meilleure lisibilité.',
  },
];

function isThemeOptionId(value: string | null): value is ThemeOptionId {
  return THEME_OPTIONS.some((o) => o.id === value);
}

export function getStoredThemeId(): ThemeOptionId {
  try {
    const raw = localStorage.getItem(THEME_STORAGE_KEY);
    if (isThemeOptionId(raw)) return raw;
  } catch {
    /* ignore */
  }
  return 'light';
}

export function applyThemePreference(id: ThemeOptionId): void {
  const theme = THEME_OPTIONS.find((o) => o.id === id)?.id ?? 'light';
  document.documentElement.setAttribute('data-theme', theme);
  try {
    localStorage.setItem(THEME_STORAGE_KEY, theme);
  } catch {
    /* ignore */
  }
}

export function initThemePreferenceFromStorage(): void {
  applyThemePreference(getStoredThemeId());
}
