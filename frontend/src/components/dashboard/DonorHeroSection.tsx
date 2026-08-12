import { Link } from "react-router-dom";

type DonorHeroSectionProps = {
  userName: string;
  bloodType: string;
  daysRemaining: number;
  livesImpacted: number;
  recommendationCount: number;
  avatarIcon?: string | null;
};

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

export function DonorHeroSection({
  userName,
  bloodType,
  daysRemaining,
  livesImpacted,
  recommendationCount,
  avatarIcon,
}: DonorHeroSectionProps) {
  const canDonateNow = daysRemaining <= 0;
  const name = userName.trim() || "Doador";
  const activeIcon = avatarIcon || localStorage.getItem("bloodmatch_avatar_icon") || "water_drop";

  return (
    <section className="relative col-span-12 lg:col-span-8 overflow-hidden rounded-[2rem] border border-surface-container-high bg-white p-6 lg:p-8">
      <div className="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-[#fff2f0]" />
      <div className="absolute -left-10 bottom-0 h-28 w-28 rounded-full bg-[#eaf3f7] opacity-80" />

      <div className="relative z-10 flex flex-col gap-6">
        <div className="flex items-start gap-4">
          <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-2xl bg-pulse-gradient text-white shadow-md">
            {activeIcon ? (
              <span className="material-symbols-outlined text-3xl">{activeIcon}</span>
            ) : (
              <span className="font-headline text-xl font-black tracking-wide">{initialsFromName(name)}</span>
            )}
          </div>

          <div className="min-w-0 flex-1">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-secondary">Dashboard do doador</p>
            <h1 className="mt-1 font-headline text-3xl font-extrabold tracking-tight text-on-surface truncate sm:text-4xl">
              Olá, {name}
            </h1>
            <p className="mt-2 text-sm text-text-secondary">
              {canDonateNow
                ? "Você está elegível para doar. Confira as recomendações compatíveis."
                : `Aguarde ${daysRemaining} ${daysRemaining === 1 ? "dia" : "dias"} para a próxima doação.`}
            </p>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          <div className="rounded-2xl bg-surface-container-low px-4 py-4 text-center sm:text-left">
            <p className="font-headline text-2xl font-black text-primary">{bloodType || "—"}</p>
            <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Tipo</p>
          </div>
          <div className="rounded-2xl bg-surface-container-low px-4 py-4 text-center sm:text-left">
            {canDonateNow ? (
              <p className="font-headline text-xl font-black text-primary leading-tight">Pronto</p>
            ) : (
              <p className="font-headline text-2xl font-black text-on-surface">{daysRemaining}</p>
            )}
            <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">
              {canDonateNow ? "Para doar" : "Dias"}
            </p>
          </div>
          <div className="rounded-2xl bg-surface-container-low px-4 py-4 text-center sm:text-left">
            <p className="font-headline text-2xl font-black text-on-surface">{livesImpacted}</p>
            <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Vidas</p>
          </div>
          <div className="rounded-2xl bg-surface-container-low px-4 py-4 text-center sm:text-left">
            <p className="font-headline text-2xl font-black text-on-surface">{recommendationCount}</p>
            <p className="mt-1 text-[10px] font-bold uppercase tracking-wider text-secondary">Matches</p>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          <Link
            to="/profile"
            className="inline-flex items-center gap-1 rounded-xl bg-surface-container-low px-4 py-2.5 text-sm font-bold text-on-surface transition-colors hover:bg-surface-container-high"
          >
            <span className="material-symbols-outlined text-base text-secondary">manage_accounts</span>
            Editar perfil
          </Link>
          {canDonateNow && (
            <Link
              to="/dashboard/recommendations"
              className="inline-flex items-center gap-1 rounded-xl bg-primary px-4 py-2.5 text-sm font-bold text-white transition-colors hover:bg-[#920f16]"
            >
              Ver recomendações
              <span className="material-symbols-outlined text-base">chevron_right</span>
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}
