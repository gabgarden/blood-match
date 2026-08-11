import { useState } from "react";
import { AppButton, InlineAlert } from "../ui";
import {
  updateDonationRequestDateLimit,
  updateDonationRequestGoalBloodBags,
  type UserDonationRequestCard,
} from "../../services/donationService";
import { extractApiErrorMessage } from "../../utils/apiError";

type RequestEditPanelProps = {
  request: UserDonationRequestCard;
  disabled?: boolean;
  onUpdated: (patch: Partial<UserDonationRequestCard>) => void;
};

const fieldClass =
  "w-full bg-surface-container-highest border-none rounded-xl px-3 py-2.5 text-sm focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all";

function toDateInputValue(value: string | null): string {
  if (!value) {
    return "";
  }

  if (/^\d{4}-\d{2}-\d{2}/.test(value)) {
    return value.slice(0, 10);
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }

  return parsed.toISOString().slice(0, 10);
}

export function RequestEditPanel({ request, disabled = false, onUpdated }: RequestEditPanelProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [dateLimit, setDateLimit] = useState(toDateInputValue(request.dateLimit));
  const [goalBloodBags, setGoalBloodBags] = useState(String(request.goalBloodBags || ""));
  const [isSavingDate, setIsSavingDate] = useState(false);
  const [isSavingGoal, setIsSavingGoal] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const canEdit = !disabled && !request.expired && !request.goalReached;

  async function handleSaveDate(event: React.FormEvent) {
    event.preventDefault();
    if (!dateLimit) {
      setErrorMessage("Informe a nova data limite.");
      return;
    }

    setIsSavingDate(true);
    setErrorMessage(null);
    setFeedback(null);

    try {
      const result = await updateDonationRequestDateLimit(request.id, dateLimit);
      const nextDate = typeof result?.dateLimit === "string" ? result.dateLimit : dateLimit;
      onUpdated({
        dateLimit: nextDate,
        deadlineLabel: new Intl.DateTimeFormat("pt-BR", {
          day: "2-digit",
          month: "short",
          year: "numeric",
        }).format(new Date(nextDate)),
      });
      setFeedback("Prazo atualizado.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível atualizar o prazo."));
    } finally {
      setIsSavingDate(false);
    }
  }

  async function handleSaveGoal(event: React.FormEvent) {
    event.preventDefault();
    const parsedGoal = Number(goalBloodBags);
    if (!Number.isFinite(parsedGoal) || parsedGoal <= 0) {
      setErrorMessage("A meta de bolsas deve ser maior que zero.");
      return;
    }

    setIsSavingGoal(true);
    setErrorMessage(null);
    setFeedback(null);

    try {
      const result = await updateDonationRequestGoalBloodBags(request.id, parsedGoal);
      const nextGoal =
        typeof result?.goalBloodBags === "string" || typeof result?.goalBloodBags === "number"
          ? Number(result.goalBloodBags)
          : parsedGoal;

      onUpdated({
        goalBloodBags: nextGoal,
        remainingBloodBags: Math.max(nextGoal - request.fulfilledBloodBags, 0),
        goalReached: request.fulfilledBloodBags >= nextGoal,
      });
      setFeedback("Meta de bolsas atualizada.");
    } catch (error) {
      setErrorMessage(extractApiErrorMessage(error, "Não foi possível atualizar a meta."));
    } finally {
      setIsSavingGoal(false);
    }
  }

  return (
    <div className="mt-4">
      <button
        type="button"
        disabled={!canEdit}
        onClick={() => setIsOpen((current) => !current)}
        className="flex w-full items-center justify-between rounded-xl bg-surface-container-low px-4 py-2.5 text-xs font-bold text-on-surface transition-colors hover:bg-surface-container-high disabled:cursor-not-allowed disabled:opacity-50"
      >
        <span className="inline-flex items-center gap-2">
          <span className="material-symbols-outlined text-base text-secondary">edit</span>
          Editar prazo e meta
        </span>
        <span className="material-symbols-outlined text-base">{isOpen ? "expand_less" : "expand_more"}</span>
      </button>

      {isOpen && canEdit && (
        <div className="mt-3 space-y-4 rounded-2xl border border-surface-container-high bg-surface-container-lowest p-4">
          {feedback && <InlineAlert tone="success" message={feedback} />}
          {errorMessage && <InlineAlert tone="error" message={errorMessage} />}

          <form className="space-y-2" onSubmit={handleSaveDate}>
            <label className="block text-[10px] font-bold uppercase tracking-wider text-secondary">
              Novo prazo limite
            </label>
            <div className="flex flex-col gap-2 sm:flex-row">
              <input
                type="date"
                value={dateLimit}
                onChange={(event) => setDateLimit(event.target.value)}
                className={fieldClass}
                required
              />
              <AppButton type="submit" variant="secondary" className="px-4 text-xs" disabled={isSavingDate}>
                {isSavingDate ? "..." : "Salvar prazo"}
              </AppButton>
            </div>
          </form>

          <form className="space-y-2" onSubmit={handleSaveGoal}>
            <label className="block text-[10px] font-bold uppercase tracking-wider text-secondary">
              Nova meta de bolsas
            </label>
            <div className="flex flex-col gap-2 sm:flex-row">
              <input
                type="number"
                min="1"
                value={goalBloodBags}
                onChange={(event) => setGoalBloodBags(event.target.value)}
                className={fieldClass}
                required
              />
              <AppButton type="submit" variant="secondary" className="px-4 text-xs" disabled={isSavingGoal}>
                {isSavingGoal ? "..." : "Salvar meta"}
              </AppButton>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
