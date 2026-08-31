import { useState } from "react";

type TodayDonationBannerProps = {
  hospitalName?: string;
  expectedDate?: string;
  bloodType?: string;
};

export function TodayDonationBanner({
  hospitalName = "Hemocentro Regional de Campos",
  expectedDate,
  bloodType = "O+",
}: TodayDonationBannerProps) {
  const [isCheckedIn, setIsCheckedIn] = useState(false);
  const [dismissed, setDismissed] = useState(false);

  if (dismissed) return null;

  return (
    <div className="relative overflow-hidden rounded-[2rem] border border-red-200 bg-gradient-to-r from-[#fff2f0] via-white to-[#fff2f0] p-5 shadow-sm">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3.5">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary text-white shadow-md">
            <span className="material-symbols-outlined text-2xl">water_drop</span>
          </div>

          <div>
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-primary/10 px-2.5 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-primary">
                Lembrete de Doação
              </span>
              {bloodType && (
                <span className="text-xs font-bold text-secondary">
                  Tipo {bloodType}
                </span>
              )}
            </div>

            <h3 className="mt-1 font-headline text-lg font-black text-on-surface">
              {isCheckedIn ? "Check-in realizado! 🩸" : "Sua doação está aproximando-se!"}
            </h3>

            <p className="mt-0.5 text-xs text-text-secondary">
              {isCheckedIn
                ? `Que ótimo! Lembre-se de comparecer ao ${hospitalName} com documento original com foto.`
                : `Local: ${hospitalName}${expectedDate ? ` • Data: ${expectedDate}` : ""}`}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          {!isCheckedIn ? (
            <button
              type="button"
              onClick={() => setIsCheckedIn(true)}
              className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 text-xs font-extrabold text-white shadow-md hover:bg-[#920f16] transition-colors"
            >
              <span className="material-symbols-outlined text-sm">how_to_reg</span>
              Fazer Check-in
            </button>
          ) : (
            <span className="inline-flex items-center gap-1 rounded-xl bg-emerald-100 px-3 py-2 text-xs font-bold text-emerald-800">
              <span className="material-symbols-outlined text-sm">check_circle</span>
              Check-in OK
            </span>
          )}

          <button
            type="button"
            onClick={() => setDismissed(true)}
            className="rounded-xl p-2 text-text-secondary hover:bg-surface-container-high transition-colors"
            title="Fechar lembrete"
          >
            <span className="material-symbols-outlined text-sm">close</span>
          </button>
        </div>
      </div>
    </div>
  );
}
