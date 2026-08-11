import { AppButton } from "../ui";
import { RequestEditPanel } from "./RequestEditPanel";
import type { UserDonationRequestCard } from "../../services/donationService";

type RequestCardProps = {
  request: UserDonationRequestCard;
  isBusy: boolean;
  onNotify: (requestId: string) => void;
  onDelete: (requestId: string) => void;
  onUpdated: (requestId: string, patch: Partial<UserDonationRequestCard>) => void;
};

type RequestUrgency = UserDonationRequestCard["urgency"];

function urgencyStyle(urgency: RequestUrgency) {
  if (urgency === "Crítica") {
    return {
      badge: "bg-primary text-white",
      progress: "bg-primary",
      accent: "border-primary",
      chip: "bg-[#fff2f0] text-primary",
    };
  }

  if (urgency === "Média") {
    return {
      badge: "bg-amber-500 text-white",
      progress: "bg-amber-500",
      accent: "border-amber-500",
      chip: "bg-amber-50 text-amber-700",
    };
  }

  return {
    badge: "bg-secondary text-white",
    progress: "bg-secondary",
    accent: "border-secondary",
    chip: "bg-surface-container-high text-secondary",
  };
}

function statusBadge(request: UserDonationRequestCard): { label: string; className: string } {
  if (request.goalReached) {
    return { label: "Meta atingida", className: "bg-emerald-100 text-emerald-700" };
  }

  if (request.expired) {
    return { label: "Expirada", className: "bg-rose-100 text-rose-700" };
  }

  if (request.active) {
    return { label: "Ativa", className: "bg-sky-100 text-sky-700" };
  }

  return { label: "Inativa", className: "bg-surface-container-high text-secondary" };
}

export function RequestCard({ request, isBusy, onNotify, onDelete, onUpdated }: RequestCardProps) {
  const style = urgencyStyle(request.urgency);
  const status = statusBadge(request);
  const progressTarget = request.goalBloodBags > 0 ? request.goalBloodBags : 1;
  const progress = Math.min(100, Math.round((request.fulfilledBloodBags / progressTarget) * 100));
  const canNotify = !isBusy && !request.expired && !request.goalReached;

  return (
    <article
      className={`flex h-full flex-col rounded-[2rem] border border-surface-container-high border-l-4 bg-white p-5 sm:p-6 shadow-sm ${style.accent}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className={`rounded-2xl px-4 py-3 ${style.chip}`}>
          <p className="font-headline text-3xl font-black leading-none">{request.bloodType}</p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-wider opacity-80">Tipo</p>
        </div>

        <div className="flex flex-col items-end gap-1.5">
          <span className={`rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider ${style.badge}`}>
            {request.urgency}
          </span>
          <span className={`rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider ${status.className}`}>
            {status.label}
          </span>
        </div>
      </div>

      <div className="mt-5 min-w-0">
        <div className="flex items-start gap-2">
          <span className="material-symbols-outlined mt-0.5 text-base text-secondary">local_hospital</span>
          <div className="min-w-0">
            <h3 className="font-headline text-lg font-extrabold text-on-surface leading-snug truncate">
              {request.bloodCenterName}
            </h3>
            <p className="mt-1 text-xs text-text-secondary">
              {request.bloodCenterPhoneNumber
                ? request.bloodCenterPhoneNumber
                : "Telefone não informado"}
            </p>
          </div>
        </div>
      </div>

      <div className="mt-5 grid grid-cols-3 gap-2 text-center">
        <div className="rounded-xl bg-surface-container-low px-2 py-3">
          <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Meta</p>
          <p className="mt-1 font-headline text-xl font-black text-on-surface">{request.goalBloodBags}</p>
        </div>
        <div className="rounded-xl bg-surface-container-low px-2 py-3">
          <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Recebidas</p>
          <p className="mt-1 font-headline text-xl font-black text-on-surface">{request.fulfilledBloodBags}</p>
        </div>
        <div className="rounded-xl bg-surface-container-low px-2 py-3">
          <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Faltam</p>
          <p className="mt-1 font-headline text-xl font-black text-primary">{request.remainingBloodBags}</p>
        </div>
      </div>

      <div className="mt-5">
        <div className="mb-2 flex items-center justify-between text-xs font-semibold">
          <span className="text-text-secondary">Progresso</span>
          <span className="text-on-surface">{progress}%</span>
        </div>
        <div className="h-2.5 w-full overflow-hidden rounded-full bg-surface-container-high">
          <div
            className={`h-full rounded-full transition-all ${request.goalReached ? "bg-emerald-500" : style.progress}`}
            style={{ width: `${progress}%` }}
          />
        </div>
        <p className="mt-2 text-xs text-text-secondary">
          {request.fulfilledBloodBags} de {request.goalBloodBags} bolsas
        </p>
      </div>

      <div className="mt-5 grid grid-cols-2 gap-3 rounded-xl bg-surface-container-low px-3 py-3">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Solicitada</p>
          <p className="mt-0.5 text-xs font-bold text-on-surface">{request.dateRequestedLabel}</p>
        </div>
        <div>
          <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Prazo</p>
          <p className="mt-0.5 text-xs font-bold text-on-surface">{request.deadlineLabel}</p>
        </div>
      </div>

      <div className="mt-auto pt-1">
        <RequestEditPanel
          request={request}
          disabled={isBusy}
          onUpdated={(patch) => onUpdated(request.id, patch)}
        />

        <div className="mt-3 flex flex-col gap-2 sm:flex-row">
          <AppButton
            type="button"
            variant="secondary"
            className="flex flex-1 items-center justify-center text-xs gap-1"
            disabled={!canNotify}
            onClick={() => onNotify(request.id)}
          >
            <span className="material-symbols-outlined text-sm">campaign</span>
            {isBusy ? "Aguarde..." : "Notificar"}
          </AppButton>
          <AppButton
            type="button"
            variant="danger"
            className="flex flex-1 items-center justify-center text-xs gap-1"
            disabled={isBusy}
            onClick={() => onDelete(request.id)}
          >
            <span className="material-symbols-outlined text-sm">delete</span>
            Remover
          </AppButton>
        </div>
      </div>
    </article>
  );
}
