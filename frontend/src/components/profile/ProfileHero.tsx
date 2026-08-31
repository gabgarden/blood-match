import { useState } from "react";
import { AvatarIconSelector } from "./AvatarIconSelector";

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
  onSelectAvatarIcon?: (iconId: string) => void;
  onSaveName?: (newName: string) => Promise<void> | void;
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
  livesImpacted,
  daysRemaining,
  lastDonationDate,
  isDonor,
  avatarIcon,
  onSelectAvatarIcon,
  onSaveName,
}: ProfileHeroProps) {
  const [isPickerOpen, setIsPickerOpen] = useState(false);
  const [isEditingName, setIsEditingName] = useState(false);
  const [editNameInput, setEditNameInput] = useState(displayName);
  const [isSavingName, setIsSavingName] = useState(false);

  const name = displayName.trim() || "Seu perfil";
  const canDonateNow = (daysRemaining ?? 0) <= 0;

  async function handleNameSubmit(e: React.FormEvent) {
    e.preventDefault();
    const trimmed = editNameInput.trim();
    if (!trimmed || !onSaveName) return;

    setIsSavingName(true);
    try {
      await onSaveName(trimmed);
      setIsEditingName(false);
    } finally {
      setIsSavingName(false);
    }
  }

  return (
    <section className="relative rounded-[2rem] bg-white border border-surface-container-high p-6 lg:p-8">
      <div className="absolute inset-0 overflow-hidden rounded-[2rem] pointer-events-none">
        <div className="absolute -right-16 -top-16 h-56 w-56 rounded-full bg-[#fff2f0]" />
        <div className="absolute -left-10 bottom-0 h-32 w-32 rounded-full bg-[#eaf3f7] opacity-80" />
      </div>

      <div className="relative z-10 flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-start gap-4 min-w-0 flex-1">
          <div className="relative flex flex-col items-center shrink-0">
            <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-pulse-gradient text-white shadow-md">
              {avatarIcon ? (
                <span className="material-symbols-outlined text-3xl">{avatarIcon}</span>
              ) : (
                <span className="font-headline text-xl font-black tracking-wide">{initialsFromName(name)}</span>
              )}
            </div>

            {onSelectAvatarIcon && (
              <div className="relative mt-2">
                <button
                  type="button"
                  onClick={() => setIsPickerOpen((prev) => !prev)}
                  className="flex items-center gap-1 rounded-full border border-surface-container-high bg-surface-container-low px-2.5 py-0.5 text-[11px] font-bold text-secondary transition-all hover:bg-[#fff2f0] hover:text-primary hover:border-[#f5d5d1] shadow-xs"
                  title="Alterar ícone do perfil"
                >
                  <span className="material-symbols-outlined text-xs">edit</span>
                  <span>Alterar</span>
                </button>

                {isPickerOpen && (
                  <>
                    <div className="fixed inset-0 z-20" onClick={() => setIsPickerOpen(false)} />
                    <div className="absolute left-0 top-full mt-2 z-30 w-60 rounded-2xl border border-surface-container-high bg-white p-3 shadow-xl animate-in fade-in zoom-in-95 duration-150">
                      <div className="mb-2 flex items-center justify-between px-1">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-secondary">
                          Ícone do perfil
                        </span>
                        <button
                          type="button"
                          onClick={() => setIsPickerOpen(false)}
                          className="rounded-lg p-0.5 text-gray-400 hover:bg-surface-container-low hover:text-gray-600"
                        >
                          <span className="material-symbols-outlined text-sm">close</span>
                        </button>
                      </div>
                      <AvatarIconSelector
                        selectedIcon={avatarIcon || "water_drop"}
                        onSelectIcon={(iconId) => {
                          onSelectAvatarIcon(iconId);
                          setIsPickerOpen(false);
                        }}
                      />
                    </div>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="min-w-0 space-y-2">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-secondary">Meu perfil</p>
            
            {isEditingName ? (
              <form onSubmit={handleNameSubmit} className="flex flex-wrap items-center gap-2 pt-1">
                <input
                  type="text"
                  value={editNameInput}
                  onChange={(e) => setEditNameInput(e.target.value)}
                  className="rounded-xl border border-primary bg-white px-3 py-1 text-lg font-extrabold text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20"
                  placeholder="Seu nome"
                  required
                  autoFocus
                />
                <button
                  type="submit"
                  disabled={isSavingName}
                  className="rounded-xl bg-primary px-3 py-1.5 text-xs font-bold text-white shadow-xs hover:bg-[#920f16] disabled:opacity-50"
                >
                  {isSavingName ? "..." : "Salvar"}
                </button>
                <button
                  type="button"
                  onClick={() => setIsEditingName(false)}
                  className="rounded-xl bg-surface-container-high px-2.5 py-1.5 text-xs font-bold text-secondary hover:bg-surface-container-highest"
                >
                  <span className="material-symbols-outlined text-xs">close</span>
                </button>
              </form>
            ) : (
              <div className="flex items-center gap-2">
                <h1 className="font-headline text-3xl font-extrabold tracking-tight text-on-surface truncate">
                  {name}
                </h1>
                {onSaveName && (
                  <button
                    type="button"
                    onClick={() => {
                      setEditNameInput(displayName);
                      setIsEditingName(true);
                    }}
                    className="flex h-7 w-7 items-center justify-center rounded-full bg-surface-container-low text-secondary hover:bg-[#fff2f0] hover:text-primary transition-all border border-surface-container-high shrink-0"
                    title="Editar nome"
                  >
                    <span className="material-symbols-outlined text-xs">edit</span>
                  </button>
                )}
              </div>
            )}

            {bloodType && (
              <div className="flex flex-wrap gap-2 pt-1">
                <span className="inline-flex items-center gap-1 rounded-lg bg-[#fff2f0] px-2.5 py-1 text-[11px] font-bold uppercase tracking-wide text-primary">
                  <span className="material-symbols-outlined text-sm">bloodtype</span>
                  {bloodType}
                </span>
              </div>
            )}

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
          <div className="grid grid-cols-3 gap-1.5 sm:gap-2.5 shrink-0 self-center">
            <div className="rounded-2xl bg-surface-container-low px-2 py-2.5 sm:px-3 sm:py-3.5 text-center min-w-[4rem] sm:min-w-[5rem]">
              <p className="font-headline text-lg sm:text-2xl font-black text-primary">
                {typeof livesImpacted === "number" ? livesImpacted : "—"}
              </p>
              <p className="mt-0.5 text-[9px] sm:text-[10px] font-bold uppercase tracking-wider text-secondary">Vidas</p>
            </div>
            <div className="rounded-2xl bg-surface-container-low px-2 py-2.5 sm:px-3 sm:py-3.5 text-center min-w-[4rem] sm:min-w-[5rem]">
              {canDonateNow ? (
                <p className="font-headline text-sm sm:text-lg font-black text-primary leading-tight">Pronto</p>
              ) : (
                <p className="font-headline text-lg sm:text-2xl font-black text-on-surface">{daysRemaining}</p>
              )}
              <p className="mt-0.5 text-[9px] sm:text-[10px] font-bold uppercase tracking-wider text-secondary">
                {canDonateNow ? "Para doar" : "Dias"}
              </p>
            </div>
            <div className="rounded-2xl bg-surface-container-low px-2 py-2.5 sm:px-3 sm:py-3.5 text-center min-w-[4rem] sm:min-w-[5rem]">
              <p className="font-headline text-[10px] sm:text-xs font-black text-on-surface leading-tight truncate">
                {formatDate(lastDonationDate)}
              </p>
              <p className="mt-0.5 text-[9px] sm:text-[10px] font-bold uppercase tracking-wider text-secondary">Última</p>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
