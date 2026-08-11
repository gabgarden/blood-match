import { AppButton, AppCard, BloodCenterSearchField, InlineAlert, type BloodCenterSelection } from "../ui";
import { BloodTypeSelector } from "./BloodTypeSelector";

type CreateRequestFormProps = {
  bloodType: string;
  onBloodTypeChange: (type: string) => void;
  quantity: number;
  onQuantityChange: (qty: number) => void;
  urgency: "BAIXA" | "MÉDIA" | "CRÍTICA";
  onUrgencyChange: (urg: "BAIXA" | "MÉDIA" | "CRÍTICA") => void;
  bloodCenter: BloodCenterSelection | null;
  onBloodCenterChange: (selection: BloodCenterSelection | null) => void;
  description: string;
  onDescriptionChange: (desc: string) => void;
  dateLimit: string;
  onDateLimitChange: (date: string) => void;
  onSubmit: (e: React.FormEvent) => void;
  isLoading: boolean;
  errorMessage: string | null;
};

export function CreateRequestForm({
  bloodType,
  onBloodTypeChange,
  quantity,
  onQuantityChange,
  urgency,
  onUrgencyChange,
  bloodCenter,
  onBloodCenterChange,
  description,
  onDescriptionChange,
  dateLimit,
  onDateLimitChange,
  onSubmit,
  isLoading,
  errorMessage,
}: CreateRequestFormProps) {
  const isFormValid = bloodType && quantity > 0 && Boolean(bloodCenter?.organizationId) && dateLimit;

  return (
    <AppCard className="p-10 bg-white border-0">
      <h2 className="font-headline text-3xl font-extrabold mb-8 text-on-surface">Detalhes da Requisição</h2>

      {errorMessage && <InlineAlert tone="error" message={errorMessage} className="mb-6" />}

      <form onSubmit={onSubmit} className="space-y-8">
        <BloodTypeSelector selected={bloodType} onSelect={onBloodTypeChange} />

        <div className="grid grid-cols-2 gap-6">
          <div className="space-y-2">
            <label className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider">
              Quantidade de Bolsas
            </label>
            <div className="relative">
              <input
                type="number"
                min="1"
                max="999"
                value={quantity || ""}
                onChange={(e) => onQuantityChange(Math.max(1, parseInt(e.target.value) || 0))}
                placeholder="Ex: 12"
                className="w-full bg-surface-container-highest border-none rounded-xl p-4 focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all placeholder:text-gray-400"
              />
              <span className="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 material-symbols-outlined text-sm">
                opacity
              </span>
            </div>
          </div>

          <div className="space-y-2">
            <label className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider">
              Nível de Urgência
            </label>
            <div className="flex bg-surface-container-high p-1 rounded-xl gap-1">
              {(["BAIXA", "MÉDIA", "CRÍTICA"] as const).map((level) => (
                <button
                  key={level}
                  type="button"
                  onClick={() => onUrgencyChange(level)}
                  className={`flex-1 py-3 text-xs font-bold rounded-lg transition-all ${
                    urgency === level
                      ? "bg-white text-primary shadow-sm"
                      : "bg-transparent text-secondary hover:bg-surface-container-low"
                  }`}
                >
                  {level}
                </button>
              ))}
            </div>
          </div>
        </div>

        <BloodCenterSearchField
          value={bloodCenter}
          onChange={onBloodCenterChange}
          required
          disabled={isLoading}
        />

        <div className="space-y-2">
          <label className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider">
            Data Limite
          </label>
          <input
            type="date"
            value={dateLimit}
            onChange={(e) => onDateLimitChange(e.target.value)}
            className="w-full bg-surface-container-highest border-none rounded-xl p-4 focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all"
          />
        </div>

        <div className="space-y-2">
          <label className="block font-label text-sm font-semibold text-secondary uppercase tracking-wider">
            Destinado a (opcional)
          </label>
          <textarea
            value={description}
            onChange={(e) => onDescriptionChange(e.target.value)}
            placeholder="Nome do paciente ou orientações específicas..."
            rows={4}
            className="w-full bg-surface-container-highest border-none rounded-xl p-4 focus:ring-0 focus:bg-surface-container-lowest focus:border-l-4 focus:border-primary transition-all placeholder:text-gray-400 resize-none"
          />
        </div>

        <div className="pt-4">
          <AppButton
            variant="danger"
            fullWidth
            disabled={!isFormValid || isLoading}
            className="py-5 text-base gap-3"
          >
            <span>{isLoading ? "Publicando..." : "PUBLICAR REQUISIÇÃO"}</span>
            {!isLoading && <span className="material-symbols-outlined text-sm">send</span>}
          </AppButton>
        </div>
      </form>
    </AppCard>
  );
}
