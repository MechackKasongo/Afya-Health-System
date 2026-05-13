type PatientDeceasedBannerProps = {
  /** ISO local date/time from backend; when nullish, renders nothing */
  deceasedAt: string | null | undefined;
  /** Optional short second line (e.g. read-only reason) */
  detail?: string;
};

function formatDeceasedAt(iso: string): string {
  try {
    return new Date(iso).toLocaleString('fr-FR', { dateStyle: 'short', timeStyle: 'short' });
  } catch {
    return iso;
  }
}

/**
 * Prominent notice when a patient is recorded as deceased.
 * <p>Français : bandeau « patient décédé » pour bloquer l'édition côté UI.</p>
 */
export function PatientDeceasedBanner({ deceasedAt, detail }: PatientDeceasedBannerProps) {
  if (deceasedAt == null || deceasedAt === '') return null;
  return (
    <div
      role="status"
      style={{
        marginBottom: '0.75rem',
        padding: '0.55rem 0.75rem',
        borderRadius: '0.45rem',
        border: '1px solid rgba(160, 68, 68, 0.5)',
        background: 'rgba(160, 68, 68, 0.1)',
        color: 'var(--text)',
        fontSize: '0.92rem',
        lineHeight: 1.4,
      }}
    >
      <strong>Patient décédé</strong>
      <span style={{ marginLeft: '0.35rem', color: 'var(--muted)', fontWeight: 500 }}>
        (enregistré le {formatDeceasedAt(deceasedAt)})
      </span>
      {detail ? (
        <div style={{ marginTop: '0.35rem', fontSize: '0.88rem', color: 'var(--muted)' }}>{detail}</div>
      ) : null}
    </div>
  );
}
