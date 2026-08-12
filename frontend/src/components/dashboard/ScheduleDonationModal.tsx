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

export function ScheduleDonationModal({
  isOpen,
  recommendation,
  onClose,
  onConfirm,
  isSubmitting = false,
}: ScheduleDonationModalProps) {
  const [expectedDate, setExpectedDate] = useState(todayIsoDate());

  if (!isOpen || !recommendation) {
    return null;
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!recommendation) return;
    onConfirm(recommendation.id, expectedDate);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4 animate-in fade-in">
      <div className="w-full max-w-lg overflow-hidden rounded-[2rem] bg-white p-6 sm:p-8 shadow-2xl border border-surface-container-high animate-in zoom-in-95">
        <div className="flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-primary">
              <span className="material-symbols-outlined text-2xl">event_available</span>
            </div>
            <div>
              <h3 className="font-headline text-xl font-extrabold text-on-surface">
                Agendar Doação Pendente
              </h3>
              <p className="text-xs text-text-secondary">
                Confirme a data prevista para comparecer ao hemocentro.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="rounded-xl p-2 text-text-secondary hover:bg-surface-container-low transition-colors"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Resumo do Hemocentro */}
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

        {/* Formulario com seleção de data */}
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

          {/* Ações */}
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
      </div>
    </div>
  );
}
