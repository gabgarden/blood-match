import { useState } from "react";
import { AppButton, Modal, InlineAlert } from "../ui";
import { completeDonation, rescheduleDonation } from "../../services/donationService";
import { extractApiErrorMessage } from "../../utils/apiError";

export type DonationHistoryItem = {
  id: string;
  version: number;
  location: string;
  donationDate: string | null;
  status: string;
};

type DonationHistoryProps = {
  items: DonationHistoryItem[];
  isLoading: boolean;
  errorMessage: string | null;
  onChanged?: () => void;
};

type ManageMode = "complete" | "reschedule";

const fieldClass =
  "w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all";

function formatDate(input: string | null): string {
  if (!input) {
    return "Data não informada";
  }

  const parsed = new Date(input);
  if (Number.isNaN(parsed.getTime())) {
    return input;
  }

  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(parsed);
}

function isPendingStatus(status: string): boolean {
  const normalized = status.trim().toUpperCase();
  return ["SCHEDULED", "PENDING", "AGENDADO", "EM_ANDAMENTO"].includes(normalized);
}

function getStatusBadge(status: string) {
  const normalized = status.trim().toUpperCase();

  if (["COMPLETED", "CONCLUIDO", "CONCLUÍDO", "DONE"].includes(normalized)) {
    return {
      label: "Concluída",
      bgClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
      icon: "check_circle",
    };
  }

  if (["SCHEDULED", "PENDING", "AGENDADO", "EM_ANDAMENTO"].includes(normalized)) {
    return {
      label: "Pendente / Agendada",
      bgClass: "bg-amber-50 text-amber-800 border-amber-200",
      icon: "schedule",
    };
  }

  if (["CANCELLED", "CANCELED", "CANCELADO"].includes(normalized)) {
    return {
      label: "Cancelada",
      bgClass: "bg-rose-50 text-rose-700 border-rose-200",
      icon: "cancel",
    };
  }

  return {
    label: "Registrada",
    bgClass: "bg-slate-100 text-slate-700 border-slate-200",
    icon: "verified",
  };
}

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

export function DonationHistory({ items, isLoading, errorMessage, onChanged }: DonationHistoryProps) {
  const [selectedDonation, setSelectedDonation] = useState<DonationHistoryItem | null>(null);
  const [mode, setMode] = useState<ManageMode>("complete");
  const [actionDate, setActionDate] = useState(todayIsoDate());
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);
  const [actionFeedback, setActionFeedback] = useState<string | null>(null);

  function openManage(item: DonationHistoryItem, nextMode: ManageMode) {
    setSelectedDonation(item);
    setMode(nextMode);
    setActionDate(item.donationDate?.slice(0, 10) || todayIsoDate());
    setActionError(null);
    setActionFeedback(null);
  }

  function closeManage() {
    if (isSubmitting) return;
    setSelectedDonation(null);
    setActionError(null);
    setActionFeedback(null);
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!selectedDonation || !actionDate) {
      setActionError("Informe uma data válida.");
      return;
    }

    setIsSubmitting(true);
    setActionError(null);
    setActionFeedback(null);

    try {
      if (mode === "complete") {
        await completeDonation(selectedDonation.id, selectedDonation.version, actionDate);
        setActionFeedback("Doação marcada como concluída.");
      } else {
        await rescheduleDonation(selectedDonation.id, selectedDonation.version, actionDate);
        setActionFeedback("Doação reagendada.");
      }

      onChanged?.();
      setTimeout(() => setSelectedDonation(null), 700);
    } catch (error) {
      setActionError(
        extractApiErrorMessage(
          error,
          mode === "complete"
            ? "Não foi possível concluir a doação."
            : "Não foi possível reagendar.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="space-y-4 rounded-[2rem] border border-surface-container-high bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-headline text-xl font-extrabold tracking-tight text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-xl">history</span>
            Histórico Completo de Doações
          </h2>
          <p className="mt-0.5 text-xs text-text-secondary">
            Registro detalhado de todas as suas doações e comprovantes salvos.
          </p>
        </div>
      </div>

      {actionFeedback && !selectedDonation && <InlineAlert tone="success" message={actionFeedback} />}

      {isLoading && (
        <div className="rounded-2xl border border-surface-container-high bg-surface-container-lowest p-8 text-center">
          <span className="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
          <p className="mt-2 text-xs text-text-secondary">Carregando histórico...</p>
        </div>
      )}

      {!isLoading && errorMessage && <InlineAlert tone="error" message={errorMessage} />}

      {!isLoading && !errorMessage && items.length === 0 && (
        <div className="rounded-2xl border border-dashed border-surface-container-highest p-8 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff2f0] text-primary">
            <span className="material-symbols-outlined text-2xl">water_drop</span>
          </div>
          <p className="mt-3 font-headline text-base font-bold text-on-surface">Nenhuma doação registrada</p>
          <p className="mt-1 text-xs text-text-secondary">
            Seu histórico de bolsas doadas aparecerá listado aqui.
          </p>
        </div>
      )}

      {!isLoading && !errorMessage && items.length > 0 && (
        <div className="divide-y divide-surface-container-high border-t border-surface-container-high mt-4">
          {items.map((item) => {
            const badge = getStatusBadge(item.status);
            const pending = isPendingStatus(item.status);

            return (
              <div
                key={item.id}
                className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center sm:justify-between transition-colors hover:bg-surface-container-low/50 px-2 rounded-xl"
              >
                <div className="flex items-center gap-3.5 min-w-0">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-red-50 text-primary">
                    <span className="material-symbols-outlined text-xl">local_hospital</span>
                  </div>

                  <div className="min-w-0">
                    <p className="truncate font-headline text-sm font-bold text-on-surface">
                      {item.location}
                    </p>
                    <p className="text-xs text-text-secondary flex items-center gap-1 mt-0.5">
                      <span className="material-symbols-outlined text-xs">calendar_today</span>
                      {formatDate(item.donationDate)}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0">
                  <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-bold border ${badge.bgClass}`}>
                    <span className="material-symbols-outlined text-sm">{badge.icon}</span>
                    {badge.label}
                  </span>

                  {/* Ações aparecem APENAS se a doação for realmente PENDENTE */}
                  {pending && (
                    <div className="flex items-center gap-2">
                      <AppButton
                        type="button"
                        variant="secondary"
                        className="text-xs py-1.5 px-3"
                        onClick={() => openManage(item, "complete")}
                      >
                        Concluir
                      </AppButton>
                      <AppButton
                        type="button"
                        variant="secondary"
                        className="text-xs py-1.5 px-3"
                        onClick={() => openManage(item, "reschedule")}
                      >
                        Reagendar
                      </AppButton>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      <Modal
        open={!!selectedDonation}
        onClose={closeManage}
        title={mode === "complete" ? "Concluir doação" : "Reagendar doação"}
        description={
          selectedDonation
            ? `${selectedDonation.location} • ${formatDate(selectedDonation.donationDate)}`
            : undefined
        }
      >
        <form className="space-y-4" onSubmit={handleSubmit}>
          {actionFeedback && <InlineAlert tone="success" message={actionFeedback} />}
          {actionError && <InlineAlert tone="error" message={actionError} />}

          <div>
            <label className="mb-2 block text-xs font-bold uppercase tracking-wider text-secondary">
              {mode === "complete" ? "Data de conclusão" : "Nova data esperada"}
            </label>
            <input
              type="date"
              value={actionDate}
              onChange={(event) => setActionDate(event.target.value)}
              className={fieldClass}
              required
            />
          </div>

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <AppButton type="button" variant="secondary" onClick={closeManage} disabled={isSubmitting}>
              Cancelar
            </AppButton>
            <AppButton type="submit" variant="danger" disabled={isSubmitting}>
              {isSubmitting ? "Salvando..." : mode === "complete" ? "Confirmar conclusão" : "Confirmar reagendamento"}
            </AppButton>
          </div>
        </form>
      </Modal>
    </section>
  );
}
