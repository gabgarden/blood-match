export const AVATAR_ICONS = [
  { id: "water_drop", label: "Gota", icon: "water_drop" },
  { id: "favorite", label: "Coração", icon: "favorite" },
  { id: "medical_services", label: "Cruz Médica", icon: "medical_services" },
  { id: "local_hospital", label: "Hemocentro", icon: "local_hospital" },
  { id: "shield_person", label: "Doador", icon: "shield_person" },
  { id: "bloodtype", label: "Tipo Sanguíneo", icon: "bloodtype" },
  { id: "award_star", label: "Estrela", icon: "award_star" },
  { id: "volunteer_activism", label: "Apoio", icon: "volunteer_activism" },
];

type AvatarIconSelectorProps = {
  selectedIcon: string;
  onSelectIcon: (iconId: string) => void;
  showLabels?: boolean;
};

export function AvatarIconSelector({ selectedIcon, onSelectIcon, showLabels = false }: AvatarIconSelectorProps) {
  return (
    <div className="grid grid-cols-4 gap-2">
      {AVATAR_ICONS.map((item) => {
        const isSelected = selectedIcon === item.id;
        return (
          <button
            key={item.id}
            type="button"
            onClick={() => onSelectIcon(item.id)}
            title={item.label}
            aria-label={item.label}
            className={`group relative flex items-center justify-center p-2 rounded-xl border transition-all ${
              isSelected
                ? "bg-[#fff2f0] border-[#f5c6c2] ring-2 ring-primary shadow-xs text-primary scale-105"
                : "bg-[#faf8f6] border-[#f0eae5] text-secondary hover:bg-[#fff2f0]/70 hover:border-[#f5d5d1] hover:text-primary"
            }`}
          >
            <div
              className={`flex h-9 w-9 items-center justify-center rounded-lg transition-all ${
                isSelected
                  ? "bg-primary text-white shadow-xs"
                  : "bg-white text-primary shadow-xs group-hover:scale-105"
              }`}
            >
              <span className="material-symbols-outlined text-lg">{item.icon}</span>
            </div>
            {showLabels && (
              <span className="mt-1 text-[10px] font-bold truncate max-w-full text-on-surface">
                {item.label}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
