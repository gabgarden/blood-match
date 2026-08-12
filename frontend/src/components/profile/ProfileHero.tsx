type ProfileHeroProps = {
  displayName: string;
  bloodType: string | null;
  phoneNumber: string | null;
  address: string | null;
  roles: string[];
  livesImpacted: number | null;
  daysRemaining: number | null;
  lastDonationDate: string | null;
  isDonor: boolean;
  avatarIcon?: string | null;
};

const ROLE_LABELS: Record<string, string> = {
  DONOR: "Doador",
  REQUESTER: "Solicitante",
  BLOOD_CENTER: "Hemocentro",
  SYSTEM_ADMIN: "Admin",
};

function roleLabel(role: string): string {
  const key = role.trim().toUpperCase();
  return ROLE_LABELS[key] ?? key;
}

function initialsFromName(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) {
    return "?";
  }
  if (parts.length === 1) {
    return parts[0].slice(0, 2).toUpperCase();
  }
  return `${parts[0][0] ?? ""}${parts[1][0] ?? ""}`.toUpperCase();
}

function formatDate(iso: string | null): string {
  if (!iso) {
    return "Sem registro";
  }
  const [year, month, day] = iso.split("-");
  if (!year || !month || !day) {
    return iso;
  }
  return `${day}/${month}/${year}`;
}

export function ProfileHero({
  displayName,
  bloodType,
  phoneNumber,
  address,
  roles,
  livesImpacted,
  daysRemaining,
  lastDonationDate,
  isDonor,
  avatarIcon,
}: ProfileHeroProps) {
  const name = displayName.trim() || "Seu perfil";
  const canDonateNow = (daysRemaining ?? 0) <= 0;

  return (
    <section className="relative overflow-hidden rounded-[2rem] bg-white border border-surface-container-high p-6 lg:p-8">
      <div className="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-[#fff2f0]" />
      <div className="absolute -left-10 bottom-0 h-32 w-32 rounded-full bg-[#eaf3f7] opacity-80" />

      <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div className="flex items-start gap-4">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-pulse-gradient text-white shadow-md">
            {avatarIcon ? (
              <span className="material-symbols-outlined text-3xl">{avatarIcon}</span>
            ) : (
              <span className="font-headline text-xl font-black tracking-wide">{initialsFromName(name)}</span>
            )}
          </div>

          <div className="min-w-0 space-y-2">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-secondary">Meu perfil</p>
            <h1 className="font-headline text-3xl font-extrabold tracking-tight text-on-surface truncate">
              {name}
            </h1>

            <div className="flex flex-wrap gap-2">
              {roles.map((role) => (
                <span
                  key={role}
                  className="inline-flex items-center rounded-lg bg-surface-container-high px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-secondary"
                >
                  {roleLabel(role)}
                </span>
              ))}
              {bloodType && (
                <span className="inline-flex items-center gap-1 rounded-lg bg-[#fff2f0] px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-primary">
                  <span className="material-symbols-outlined text-sm">bloodtype</span>
                  {bloodType}
                </span>
              )}
            </div>

            <div className="space-y-1 pt-1 text-sm text-text-secondary">
              {phoneNumber && (
                <p className="flex items-center gap-2">
                  <span className="material-symbols-outlined text-base text-secondary">call</span>
                  {phoneNumber}
                </p>
              )}
              {address && (
                <p className="flex items-start gap-2">
                  <span className="material-symbols-outlined text-base text-secondary mt-0.5">location_on</span>
                  <span>{address}</span>
                </p>
              )}
            </div>
          </div>
        </div>

        {isDonor && (
          <div className="grid grid-cols-3 gap-3 w-full lg:w-auto lg:min-w-[22rem]">
            <div className="rounded-2xl bg-surface-container-low px-3 py-4 text-center">
              <p className="font-headline text-2xl font-black text-primary">
                {typeof livesImpacted === "number" ? livesImpacted : "—"}
              </p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Vidas</p>
            </div>
            <div className="rounded-2xl bg-surface-container-low px-3 py-4 text-center">
              {canDonateNow ? (
                <p className="font-headline text-lg font-black text-primary leading-tight">Pronto</p>
              ) : (
                <p className="font-headline text-2xl font-black text-on-surface">{daysRemaining}</p>
              )}
              <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">
                {canDonateNow ? "Para doar" : "Dias"}
              </p>
            </div>
            <div className="rounded-2xl bg-surface-container-low px-3 py-4 text-center">
              <p className="font-headline text-sm font-black text-on-surface leading-snug">
                {formatDate(lastDonationDate)}
              </p>
              <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Última</p>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
