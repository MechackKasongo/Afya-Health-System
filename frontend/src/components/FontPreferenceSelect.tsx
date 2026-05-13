import { useEffect, useState } from 'react';
import { applyFontPreference, FONT_OPTIONS, getStoredFontId, type FontOptionId } from '../ui/fontPreference';

type Props = {
  /** Pour éviter les doublons d’id si plusieurs instances (ex. login vs layout). */
  selectId?: string;
};

export function FontPreferenceSelect({ selectId = 'ui-font-family' }: Props) {
  const [fontId, setFontId] = useState<FontOptionId>(() => getStoredFontId());

  useEffect(() => {
    applyFontPreference(fontId);
  }, [fontId]);

  return (
    <label className="font-preference" htmlFor={selectId}>
      <span className="font-preference__label">Police</span>
      <select
        id={selectId}
        className="font-preference__select"
        value={fontId}
        onChange={(e) => setFontId(e.target.value as FontOptionId)}
        title="Police de l’interface (lisible sur fond clair)"
      >
        {FONT_OPTIONS.map((opt) => (
          <option key={opt.id} value={opt.id}>
            {opt.label}
          </option>
        ))}
      </select>
    </label>
  );
}
