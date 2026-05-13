/**
 * Choix de police d’interface : préférence stockée en localStorage et appliquée via la variable CSS `--font`.
 */

export const FONT_STORAGE_KEY = 'afya-ui-font';

export type FontOptionId = 'dm-sans' | 'inter' | 'source-sans' | 'atkinson' | 'merriweather' | 'system-ui';

export type FontOption = {
  id: FontOptionId;
  /** Libellé pour le sélecteur */
  label: string;
  /** Valeur complète pour font-family */
  stack: string;
};

export const FONT_OPTIONS: FontOption[] = [
  { id: 'dm-sans', label: 'DM Sans', stack: "'DM Sans', system-ui, sans-serif" },
  { id: 'inter', label: 'Inter', stack: "'Inter', system-ui, sans-serif" },
  { id: 'source-sans', label: 'Source Sans 3', stack: "'Source Sans 3', system-ui, sans-serif" },
  { id: 'atkinson', label: 'Atkinson Hyperlegible', stack: "'Atkinson Hyperlegible', system-ui, sans-serif" },
  { id: 'merriweather', label: 'Merriweather (serif)', stack: "'Merriweather', Georgia, serif" },
  { id: 'system-ui', label: 'Police système', stack: 'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif' },
];

function isFontOptionId(value: string | null): value is FontOptionId {
  return value != null && FONT_OPTIONS.some((o) => o.id === value);
}

export function getStoredFontId(): FontOptionId {
  try {
    const raw = localStorage.getItem(FONT_STORAGE_KEY);
    if (isFontOptionId(raw)) return raw;
  } catch {
    /* ignore */
  }
  return 'dm-sans';
}

export function applyFontPreference(id: FontOptionId): void {
  const opt = FONT_OPTIONS.find((o) => o.id === id) ?? FONT_OPTIONS[0];
  document.documentElement.style.setProperty('--font', opt.stack);
  try {
    localStorage.setItem(FONT_STORAGE_KEY, opt.id);
  } catch {
    /* ignore */
  }
}

/** À appeler au démarrage de l’app (avant le premier rendu si possible). */
export function initFontPreferenceFromStorage(): void {
  applyFontPreference(getStoredFontId());
}
