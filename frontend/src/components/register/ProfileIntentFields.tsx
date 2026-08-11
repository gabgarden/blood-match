type ProfileIntentFieldsProps = {
  accountType: "person" | "organization";
  bloodType: string;
  weight: string;
  onFieldChange: (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => void;
  labelStyle: string;
  inputStyle: string;
};

const bloodTypes = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export function ProfileIntentFields({
  accountType,
  bloodType,
  weight,
  onFieldChange,
  labelStyle,
  inputStyle,
}: ProfileIntentFieldsProps) {
  const showPersonFields = accountType === "person";

  return (
    <section className="space-y-2">
      {showPersonFields && (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <div className="flex flex-col">
            <label className={labelStyle}>Tipo sanguíneo</label>
            <select name="bloodType" value={bloodType} onChange={onFieldChange} className={inputStyle} required>
              {bloodTypes.map((type) => (
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
              min="1"
              step="0.1"
              placeholder="Ex: 72.5"
              value={weight}
              onChange={onFieldChange}
              className={inputStyle}
              required
            />
          </div>
        </div>
      )}
    </section>
  );
}

