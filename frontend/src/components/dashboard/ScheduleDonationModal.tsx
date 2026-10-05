import { useEffect, useState } from "react";
import type { Recommendation } from "../../hooks/useDonorDashboard";

type ScheduleDonationModalProps = {
  isOpen: boolean;
  recommendation: Recommendation | null;
  onClose: () => void;
  onConfirm: (requestId: string, expectedDate: string, expectedTime?: string) => Promise<boolean> | boolean | void;
  isSubmitting?: boolean;
  daysRemaining?: number;
};

function todayIsoDate(): string {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDaysIsoDate(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
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
  daysRemaining = 0,
}: ScheduleDonationModalProps) {
  const minDate = daysRemaining > 0 ? addDaysIsoDate(daysRemaining) : todayIsoDate();
  const [expectedDate, setExpectedDate] = useState(minDate);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setExpectedDate(minDate);
    setIsSuccess(false);
  }, [isOpen, recommendation?.id, minDate]);

  if (!isOpen || !recommendation) {
    return null;
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!recommendation) {
      return;
    }

    const ok = await onConfirm(recommendation.id, expectedDate);

    if (ok !== false) {
      setIsSuccess(true);
    }
  }

  function handleCloseModal() {
    setIsSuccess(false);
    onClose();
  }

  const calendarUrl = createGoogleCalendarUrl(
    `Doação de Sangue (${recommendation.bloodTypeNeeded}) - BloodMatch`,
    `Doação agendada no ${recommendation.bloodCenterName} para o tipo ${recommendation.bloodTypeNeeded}.`,
    recommendation.bloodCenterName,
    expectedDate,
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
                {isSuccess ? "Doação Agendada!" : "Registrar Intenção de Doação"}
              </h3>
              <p className="text-xs text-text-secondary">
                {isSuccess
                  ? "Sua intenção de doação foi salva com sucesso."
                  : "Confirme a data prevista para comparecer ao hemocentro."}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={handleCloseModal}
            className="rounded-full p-2 text-text-secondary hover:bg-surface-container-high transition-colors"
          >
            <span className="material-symbols-outlined text-xl">close</span>
          </button>
        </div>

        {isSuccess ? (
          <div className="mt-6 space-y-5 animate-in fade-in">
            <div className="rounded-2xl bg-emerald-50/60 border border-emerald-100 p-4 text-center">
              <p className="text-sm font-bold text-emerald-900">
                Agendamento pendente registrado!
              </p>
              <p className="mt-1 text-xs text-emerald-700">
                Data prevista: <span className="font-extrabold">{expectedDate.split("-").reverse().join("/")}</span> no {recommendation.bloodCenterName}.
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
                <span className="text-xs font-bold uppercase tracking-wider text-secondary">Hemocentro / Hospital</span>
                <span className="inline-flex items-center gap-1 rounded-md bg-red-100 px-2 py-0.5 text-xs font-extrabold text-primary">
                  <span className="material-symbols-outlined text-sm">bloodtype</span>
                  {recommendation.bloodTypeNeeded}
                </span>
              </div>
              <p className="font-headline text-base font-bold text-on-surface">{recommendation.bloodCenterName}</p>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <div>
                <label htmlFor="expectedDate" className="block text-xs font-bold uppercase tracking-wider text-secondary mb-1.5">
                  Data Prevista da Doação
                </label>
                <input
                  id="expectedDate"
                  type="date"
                  min={minDate}
                  value={expectedDate}
                  onChange={(event) => setExpectedDate(event.target.value)}
                  required
                  className="w-full rounded-xl border border-surface-container-high bg-white px-4 py-3 text-sm font-bold text-on-surface shadow-sm focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
                {daysRemaining > 0 && (
                  <p className="mt-2 text-xs text-amber-700 font-semibold bg-amber-50 p-2.5 rounded-xl border border-amber-100 flex items-center gap-1.5">
                    <span className="material-symbols-outlined text-sm shrink-0 text-amber-600">hourglass_top</span>
                    <span>
                      Como você está em intervalo de descanso ({daysRemaining} dias restantes), a data mínima permitida
                      foi definida para o seu primeiro dia de aptidão ({minDate.split("-").reverse().join("/")}).
                    </span>
                  </p>
                )}
              </div>

              <p className="text-xs text-text-secondary leading-relaxed">
                Ao registrar, sua intenção de doação é salva como pendente. Após realizar a doação presencialmente, ela
                poderá ser marcada como concluída para atualizar seu histórico e intervalo.
              </p>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={handleCloseModal}
                  className="rounded-xl px-4 py-2.5 text-sm font-bold text-text-secondary hover:bg-surface-container-high transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white shadow-md hover:bg-primary-hover disabled:opacity-60 transition-all"
                >
                  {isSubmitting ? (
                    <>
                      <span className="material-symbols-outlined animate-spin text-lg">progress_activity</span>
                      Salvando...
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-lg">check_circle</span>
                      Confirmar Intenção
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
