import { AppButton } from "../ui";

type RecommendationCardProps = {
  id: string;
  name: string;
  bloodTypeNeeded: string;
  dateLimit: string;
  urgency: "LOW" | "MEDIUM" | "CRITICAL";
  distanceInKm: number | null;
  goalBloodBags: number | null;
  fulfilledBloodBags: number | null;
  goalReached: boolean;
  onAccept: (id: string) => void;
};

function formatDate(input: string): string {
  if (!input) {
    return "Prazo não informado";
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

function getUrgencyVisual(urgency: "LOW" | "MEDIUM" | "CRITICAL") {
  if (urgency === "CRITICAL") {
    return {
      label: "Crítica",
      badge: "bg-primary text-white",
      accent: "border-primary",
      chip: "bg-[#fff2f0] text-primary",
      progress: "bg-primary",
    };
  }

  if (urgency === "MEDIUM") {
    return {
      label: "Média",
      badge: "bg-amber-500 text-white",
      accent: "border-amber-500",
      chip: "bg-amber-50 text-amber-700",
      progress: "bg-amber-500",
    };
  }

  return {
    label: "Baixa",
    badge: "bg-secondary text-white",
    accent: "border-secondary",
    chip: "bg-surface-container-high text-secondary",
    progress: "bg-secondary",
  };
}

export function RecommendationCard({
  id,
  name,
  bloodTypeNeeded,
  dateLimit,
  urgency,
  distanceInKm,
  goalBloodBags,
  fulfilledBloodBags,
  goalReached,
  onAccept,
}: RecommendationCardProps) {
  const visual = getUrgencyVisual(urgency);
  const target = goalBloodBags ?? 0;
  const fulfilled = fulfilledBloodBags ?? 0;
  const progress = target > 0 ? Math.min(100, Math.round((fulfilled / target) * 100)) : 0;
  const remaining = target > 0 ? Math.max(target - fulfilled, 0) : null;

  return (
    <article
      className={`flex h-full flex-col rounded-[2rem] border border-surface-container-high border-l-4 bg-white p-5 sm:p-6 shadow-sm transition-shadow hover:shadow-md ${visual.accent}`}
    >
      <div className="flex items-start justify-between gap-3">
        <div className={`rounded-2xl px-4 py-3 ${visual.chip}`}>
          <p className="font-headline text-3xl font-black leading-none">{bloodTypeNeeded}</p>
          <p className="mt-1 text-[10px] font-bold uppercase tracking-wider opacity-80">Precisa</p>
        </div>
        <div className="flex flex-col items-end gap-1.5">
          <span className={`rounded-lg px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider ${visual.badge}`}>
            {visual.label}
          </span>
          {goalReached && (
            <span className="rounded-lg bg-emerald-100 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-emerald-700">
              Meta atingida
            </span>
          )}
        </div>
      </div>

      <div className="mt-5 min-w-0">
        <div className="flex items-start gap-2">
          <span className="material-symbols-outlined mt-0.5 text-base text-secondary">local_hospital</span>
          <div className="min-w-0">
            <h3 className="font-headline text-lg font-extrabold leading-snug text-on-surface truncate">{name}</h3>
            {distanceInKm != null && (
              <p className="mt-1 text-xs font-semibold text-text-secondary">
                {distanceInKm.toFixed(1)} km de distância
              </p>
            )}
          </div>
        </div>
      </div>

      <div className="mt-5 rounded-xl bg-surface-container-low px-4 py-3 space-y-3">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-wider text-secondary">Prazo limite</p>
          <p className="mt-0.5 text-sm font-bold text-on-surface">{formatDate(dateLimit)}</p>
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between text-xs font-semibold">
            <span className="text-text-secondary">Progresso</span>
            <span className="text-on-surface">
              {fulfilled} / {target > 0 ? target : "—"}
            </span>
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-surface-container-high">
            <div
              className={`h-full rounded-full ${goalReached ? "bg-emerald-500" : visual.progress}`}
              style={{ width: `${progress}%` }}
            />
          </div>
          {remaining != null && (
            <p className="mt-2 text-[11px] text-text-secondary">
              {goalReached
                ? "Meta concluída"
                : `${remaining} bolsa${remaining === 1 ? "" : "s"} restante${remaining === 1 ? "" : "s"}`}
            </p>
          )}
        </div>
      </div>

      <AppButton
        variant="danger"
        fullWidth
        onClick={() => onAccept(id)}
        disabled={goalReached}
        className="mt-5"
      >
        {goalReached ? "Meta já atingida" : "Agendar doação"}
      </AppButton>
    </article>
  );
}
