type BloodTypeSelectorProps = {
  selected: string;
  onSelect: (bloodType: string) => void;
};

const bloodTypes = ["A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"];

export function BloodTypeSelector({ selected, onSelect }: BloodTypeSelectorProps) {
  return (
    <div>
      <label className="block font-label text-sm font-semibold text-secondary mb-4 uppercase tracking-wider">
        Tipo Sanguíneo Necessário
      </label>
      <div className="grid grid-cols-4 gap-3">
        {bloodTypes.map((type) => (
          <button
            key={type}
            type="button"
            onClick={() => onSelect(type)}
            className={`flex flex-col items-center justify-center p-4 rounded-xl transition-all ${
              selected === type
                ? "bg-[#ae131a] text-white shadow-lg"
                : "bg-surface-container-low hover:bg-primary/5 text-[#ae131a]"
            }`}
          >
            <span className="font-headline text-2xl font-black">{type}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
