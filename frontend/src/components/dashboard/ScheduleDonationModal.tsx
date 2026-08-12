import { useState } from "react";
import type { Recommendation } from "../../hooks/useDonorDashboard";

type ScheduleDonationModalProps = {
  isOpen: boolean;
  recommendation: Recommendation | null;
  onClose: () => void;
  onConfirm: (requestId: string, expectedDate: string) => void;
  isSubmitting?: boolean;
};

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

function createGoogleCalendarUrl(title: string, details: string, location: string, dateIso: string): string {
  const cleanDate = dateIso.replace(/-/g, "");
  const start = `${cleanDate}T090000Z`;
  const end = `${cleanDate}T110000Z`;
  const params = new URLSearchParams({
    action: "TEMPLATE",
    text: title,
    details: `${details}\n\nAgendado via BloodMatch.`,
    location,
    dates: `${start}/${end}`,
  });
  return `https://calendar.google.com/calendar/render?${params.toString()}`;
}

export function ScheduleDonationModal({
  isOpen,
  recommendation,
  onClose,
  onConfirm,
  isSubmitting = false,
}: ScheduleDonationModalProps) {
  const [expectedDate, setExpectedDate] = useState(todayIsoDate());
  const [isSuccess, setIsSuccess] = useState(false);

  if (!isOpen || !recommendation) {
    return null;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!recommendation) return;
    await onConfirm(recommendation.id, expectedDate);
    setIsSuccess(true);
  }

  function handleCloseModal() {
    setIsSuccess(false);
    onClose();
  }

  const calendarUrl = createGoogleCalendarUrl(
    `Doação de Sangue (${recommendation.bloodTypeNeeded}) - BloodMatch`,
    `Doação agendada no ${recommendation.bloodCenterName} para o tipo ${recommendation.bloodTypeNeeded}.`,
    recommendation.bloodCenterName,
    expectedDate
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="w-full max-w-lg overflow-hidden rounded-[2rem] bg-white p-6 sm:p-8 shadow-2xl border border-surface-container-high animate-in zoom-in-95">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className={`flex h-12 w-12 items-center justify-center rounded-2xl ${isSuccess ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-primary"}`}>
              <span className="material-symbols-outlined text-2xl">{isSuccess ? "verified" : "event_available"}</span>
            </div>
            <div>
              <h3 className="font-headline text-xl font-extrabold text-on-surface">
                {isSuccess ? "Doação Agendada!" : "Agendar Doação Pendente"}
              </h3>
              <p className="text-xs text-text-secondary">
                {isSuccess ? "Sua intenção de doação foi salva com sucesso." : "Confirme a data prevista para comparecer ao hemocentro."}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleCloseModal}
            disabled={isSubmitting}
            className="rounded-xl p-2 text-text-secondary hover:bg-surface-container-low transition-colors"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {isSuccess ? (
          <div className="mt-6 space-y-5">
            <div className="rounded-2xl bg-emerald-50/70 border border-emerald-100 p-4 space-y-2">
              <p className="text-sm font-bold text-emerald-800">
                🎉 Tudo pronto! Lembre-se de beber bastante água e levar documento original com foto.
              </p>
              <p className="text-xs text-emerald-700">
                Data marcada: <strong className="font-extrabold">{expectedDate.split("-").reverse().join("/")}</strong> no {recommendation.bloodCenterName}.
              </p>
            </div>

            <div className="flex flex-col gap-2.5">
              <a
                href={calendarUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 py-3 text-sm font-bold text-white shadow-md hover:bg-blue-700 transition-colors"
              >
                <span className="material-symbols-outlined text-lg">calendar_add_on</span>
                Adicionar ao Google Agenda
              </a>

              <button
                type="button"
                onClick={handleCloseModal}
                className="rounded-xl bg-surface-container-low px-5 py-3 text-sm font-bold text-on-surface hover:bg-surface-container-high transition-colors"
              >
                Concluir
              </button>
            </div>
          </div>
        ) : (
          <>
            <div className="mt-6 rounded-2xl bg-surface-container-low p-4 border border-surface-container-high space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider text-secondary">
                  Hemocentro / Hospital
                </span>
                <span className="inline-flex items-center gap-1 rounded-md bg-red-100 px-2 py-0.5 text-xs font-extrabold text-primary">
                  <span className="material-symbols-outlined text-sm">bloodtype</span>
                  {recommendation.bloodTypeNeeded}
                </span>
              </div>
              <p className="font-headline text-base font-bold text-on-surface">
                {recommendation.bloodCenterName}
              </p>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <div>
                <label htmlFor="expectedDate" className="block text-xs font-bold uppercase tracking-wider text-secondary mb-1.5">
                  Data Prevista da Doação
                </label>
                <input
                  id="expectedDate"
                  type="date"
                  min={todayIsoDate()}
                  value={expectedDate}
                  onChange={(e) => setExpectedDate(e.target.value)}
                  required
                  className="w-full rounded-xl border border-surface-container-high bg-white px-4 py-3 text-sm font-bold text-on-surface shadow-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>

              <p className="text-xs text-text-secondary leading-relaxed">
                Ao agendar, sua intenção de doação é registrada. Após realizar a doação no local, o hemocentro ou você poderão confirmar a conclusão.
              </p>

              <div className="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
                <button
                  type="button"
                  onClick={onClose}
                  disabled={isSubmitting}
                  className="rounded-xl px-5 py-3 text-sm font-bold text-text-secondary hover:bg-surface-container-low transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-6 py-3 text-sm font-bold text-white shadow-lg shadow-primary/20 hover:bg-[#920f16] transition-colors disabled:opacity-50"
                >
                  {isSubmitting ? (
                    <>
                      <span className="material-symbols-outlined animate-spin text-base">progress_activity</span>
                      Agendando...
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-base">check</span>
                      Confirmar Agendamento
                    </>
                  )}
                </button>
              </div>
            </form>
          </>
        )}
      </div>
    </div>
  );
}
