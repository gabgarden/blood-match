import { BLOOD_TYPES } from "../../services/bloodCenterService";

export type OrganizationKind = "blood_center" | "hospital";
export type DonatedRecently = "yes" | "no";

type ProfileIntentFieldsProps = {
  accountType: "person" | "organization";
  bloodType: string;
  weight: string;
  donatedRecently: DonatedRecently | "";
  lastDonationDate: string;
  lastDonationMin: string;
  lastDonationMax: string;
  organizationKind: OrganizationKind | "";
  onFieldChange: (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  onDonatedRecentlyChange: (value: DonatedRecently) => void;
  onOrganizationKindChange: (value: OrganizationKind) => void;
  labelStyle: string;
  inputStyle: string;
};

export function ProfileIntentFields({
  accountType,
  bloodType,
  weight,
  donatedRecently,
  lastDonationDate,
  lastDonationMin,
  lastDonationMax,
  organizationKind,
  onFieldChange,
  onDonatedRecentlyChange,
  onOrganizationKindChange,
  labelStyle,
  inputStyle,
}: ProfileIntentFieldsProps) {
  const showPersonFields = accountType === "person";

  return (
    <section className="space-y-3">
      {showPersonFields && (
        <>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div className="flex flex-col">
              <label className={labelStyle}>Tipo sanguíneo</label>
              <select name="bloodType" value={bloodType} onChange={onFieldChange} className={inputStyle} required>
                {BLOOD_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex flex-col">
              <label className={labelStyle}>Peso (kg)</label>
              <input
                name="weight"
                type="number"
                min="50"
                step="0.1"
                placeholder="Ex: 72.5"
                value={weight}
                onChange={onFieldChange}
                className={inputStyle}
                required
              />
              <p className="mt-1 text-[11px] text-gray-500">O mínimo para doar é 50 kg.</p>
            </div>
          </div>

          <div className="rounded-2xl border border-gray-100 bg-gray-50/80 p-3 space-y-3">
            <p className={labelStyle}>Você doou sangue nos últimos 3 meses?</p>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => onDonatedRecentlyChange("yes")}
                className={`rounded-xl px-3 py-2 text-sm font-bold transition-all ${
                  donatedRecently === "yes"
                    ? "bg-primary text-white shadow-sm"
                    : "bg-white text-gray-600 border border-gray-200 hover:border-primary hover:text-primary"
                }`}
              >
                Sim
              </button>
              <button
                type="button"
                onClick={() => onDonatedRecentlyChange("no")}
                className={`rounded-xl px-3 py-2 text-sm font-bold transition-all ${
                  donatedRecently === "no"
                    ? "bg-primary text-white shadow-sm"
                    : "bg-white text-gray-600 border border-gray-200 hover:border-primary hover:text-primary"
                }`}
              >
                Não
              </button>
            </div>

            {donatedRecently === "yes" && (
              <div className="flex flex-col">
                <label htmlFor="lastDonationDate" className={labelStyle}>
                  Data da última doação
                </label>
                <input
                  id="lastDonationDate"
                  name="lastDonationDate"
                  type="date"
                  min={lastDonationMin}
                  max={lastDonationMax}
                  value={lastDonationDate}
                  onChange={onFieldChange}
                  className={inputStyle}
                  required
                />
              </div>
            )}
          </div>
        </>
      )}

      {accountType === "organization" && (
        <div className="space-y-3">
          <p className={labelStyle}>Tipo de instituição</p>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            <button
              type="button"
              onClick={() => onOrganizationKindChange("blood_center")}
              className={`rounded-2xl border p-3 text-left transition-all ${
                organizationKind === "blood_center"
                  ? "border-primary bg-red-50 text-primary shadow-sm"
                  : "border-gray-200 bg-white text-gray-700 hover:border-primary/40"
              }`}
            >
              <span className="material-symbols-outlined text-xl">bloodtype</span>
              <p className="mt-1 text-sm font-bold">Banco de sangue</p>
              <p className="mt-0.5 text-[11px] font-medium text-gray-500 leading-snug">
                Gerencia estoque, agenda e marcações do hemocentro.
              </p>
            </button>
            <button
              type="button"
              onClick={() => onOrganizationKindChange("hospital")}
              className={`rounded-2xl border p-3 text-left transition-all ${
                organizationKind === "hospital"
                  ? "border-primary bg-red-50 text-primary shadow-sm"
                  : "border-gray-200 bg-white text-gray-700 hover:border-primary/40"
              }`}
            >
              <span className="material-symbols-outlined text-xl">local_hospital</span>
              <p className="mt-1 text-sm font-bold">Hospital</p>
              <p className="mt-0.5 text-[11px] font-medium text-gray-500 leading-snug">
                Também recebe o papel de hemocentro para estoque e agenda.
              </p>
            </button>
          </div>
          <p className="text-[11px] text-gray-500 leading-relaxed">
            A conta poderá gerenciar estoque e horários de doação. Hospital e banco de sangue também podem abrir
            requisições.
          </p>
        </div>
      )}

      <p className="rounded-xl bg-red-50/80 border border-red-100 px-3 py-2 text-[11px] font-medium text-red-800 leading-relaxed">
        Sua conta será ativada após a confirmação do e-mail.
      </p>
    </section>
  );
}
