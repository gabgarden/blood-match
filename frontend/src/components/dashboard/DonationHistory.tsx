import { useState } from "react";
import { AppButton, Modal, InlineAlert } from "../ui";
import { completeDonation, rescheduleDonation } from "../../services/donationService";
import { extractApiErrorMessage } from "../../utils/apiError";

export type DonationHistoryItem = {
  id: string;
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
    month: "short",
    year: "numeric",
  }).format(parsed);
}

function getStatusVisual(status: string): { label: string; className: string } {
  const normalized = status.trim().toUpperCase();

  if (["COMPLETED", "CONCLUIDO", "CONCLUÍDO", "DONE"].includes(normalized)) {
    return {
      label: "Concluída",
      className: "bg-emerald-100 text-emerald-700",
    };
  }

  if (["SCHEDULED", "PENDING", "AGENDADO", "EM_ANDAMENTO"].includes(normalized)) {
    return {
      label: "Pendente",
      className: "bg-amber-100 text-amber-700",
    };
  }

  if (["CANCELLED", "CANCELED", "CANCELADO"].includes(normalized)) {
    return {
      label: "Cancelada",
      className: "bg-rose-100 text-rose-700",
    };
  }

  if (normalized === "REGISTERED") {
    return {
      label: "Registrada",
      className: "bg-surface-container-high text-secondary",
    };
  }

  return {
    label: status || "Registrada",
    className: "bg-surface-container-high text-secondary",
  };
}

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10);
}

function isPendingStatus(status: string): boolean {
  const normalized = status.trim().toUpperCase();
  return ["SCHEDULED", "PENDING", "AGENDADO", "EM_ANDAMENTO"].includes(normalized);
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
    if (isSubmitting) {
      return;
    }

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
        await completeDonation(selectedDonation.id, actionDate);
        setActionFeedback("Doação marcada como concluída.");
      } else {
        await rescheduleDonation(selectedDonation.id, actionDate);
        setActionFeedback("Doação reagendada.");
      }

      onChanged?.();
      setTimeout(() => {
        setSelectedDonation(null);
      }, 700);
    } catch (error) {
      setActionError(
        extractApiErrorMessage(
          error,
          mode === "complete"
            ? "Não foi possível concluir a doação. Ela pode já estar concluída."
            : "Não foi possível reagendar. Só doações pendentes podem ser reagendadas.",
        ),
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="space-y-4">
      <div>
        <h2 className="font-headline text-2xl font-extrabold tracking-tight text-on-surface">Histórico de doações</h2>
        <p className="mt-1 text-sm text-text-secondary">
          Conclua ou reagende doações pendentes quando necessário.
        </p>
      </div>

      {actionFeedback && !selectedDonation && <InlineAlert tone="success" message={actionFeedback} />}

      {isLoading && (
        <div className="rounded-[2rem] border border-surface-container-high bg-white p-10 text-center">
          <span className="material-symbols-outlined animate-spin text-3xl text-primary">progress_activity</span>
          <p className="mt-3 text-sm text-text-secondary">Carregando histórico...</p>
        </div>
      )}

      {!isLoading && errorMessage && <InlineAlert tone="error" message={errorMessage} />}

      {!isLoading && !errorMessage && items.length === 0 && (
        <div className="rounded-[2rem] border border-dashed border-surface-container-highest bg-white px-6 py-12 text-center">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#fff2f0] text-primary">
            <span className="material-symbols-outlined text-2xl">water_drop</span>
          </div>
          <p className="mt-4 font-headline text-lg font-bold text-on-surface">Nenhuma doação registrada</p>
          <p className="mt-1 text-sm text-text-secondary">
            Seu histórico aparece aqui após agendar ou registrar uma doação.
          </p>
        </div>
      )}

      {!isLoading && !errorMessage && items.length > 0 && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {items.map((item) => {
            const statusVisual = getStatusVisual(item.status);
            const pending = isPendingStatus(item.status);

            return (
              <article
                key={item.id}
                className="flex flex-col gap-4 rounded-[1.75rem] border border-surface-container-high bg-white p-5"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-center gap-3">
                    <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#fff2f0]">
                      <span className="material-symbols-outlined text-base text-primary">local_hospital</span>
                    </div>
                    <p className="truncate text-sm font-bold text-on-surface">{item.location}</p>
                  </div>

                  <span
                    className={`shrink-0 rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider ${statusVisual.className}`}
                  >
                    {statusVisual.label}
                  </span>
                </div>

                <div className="rounded-xl bg-surface-container-low px-3 py-3">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Data</p>
                  <p className="mt-0.5 text-sm font-bold text-on-surface">{formatDate(item.donationDate)}</p>
                </div>

                <div className="mt-auto grid grid-cols-2 gap-2">
                  <AppButton
                    type="button"
                    variant="secondary"
                    className="text-xs py-2"
                    disabled={!pending}
                    onClick={() => openManage(item, "complete")}
                  >
                    Concluir
                  </AppButton>
                  <AppButton
                    type="button"
                    variant="secondary"
                    className="text-xs py-2"
                    disabled={!pending}
                    onClick={() => openManage(item, "reschedule")}
                  >
                    Reagendar
                  </AppButton>
                </div>
              </article>
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
