export const AVATAR_ICONS = [
  { id: "water_drop", label: "Gota", icon: "water_drop" },
  { id: "favorite", label: "Coração", icon: "favorite" },
  { id: "medical_services", label: "Cruz Médica", icon: "medical_services" },
  { id: "vaccine", label: "Coleta", icon: "vaccine" },
  { id: "local_hospital", label: "Hemocentro", icon: "local_hospital" },
  { id: "shield_person", label: "Doador", icon: "shield_person" },
  { id: "bloodtype", label: "Tipo", icon: "bloodtype" },
  { id: "award_star", label: "Estrela", icon: "award_star" },
];

type AvatarIconSelectorProps = {
  selectedIcon: string;
  onSelectIcon: (iconId: string) => void;
};

export function AvatarIconSelector({ selectedIcon, onSelectIcon }: AvatarIconSelectorProps) {
  return (
    <div className="space-y-3">
      <label className="block text-xs font-bold uppercase tracking-wider text-secondary">
        Ícone de perfil minimalista
      </label>
      <div className="grid grid-cols-4 sm:grid-cols-8 gap-3">
        {AVATAR_ICONS.map((item) => {
          const isSelected = selectedIcon === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onSelectIcon(item.id)}
              title={item.label}
              className={`group flex flex-col items-center justify-center p-2.5 rounded-2xl border transition-all ${
                isSelected
                  ? "bg-[#fff2f0] border-[#f5c6c2] ring-2 ring-primary shadow-sm text-primary"
                  : "bg-[#faf8f6] border-[#f0eae5] text-secondary hover:bg-[#fff2f0]/60 hover:border-[#f5d5d1]"
              }`}
            >
              <div
                className={`flex h-10 w-10 items-center justify-center rounded-xl transition-all ${
                  isSelected
                    ? "bg-primary text-white shadow-md scale-105"
                    : "bg-white text-primary shadow-xs group-hover:scale-105"
                }`}
              >
                <span className="material-symbols-outlined text-xl">{item.icon}</span>
              </div>
              <span className="mt-1.5 text-[10px] font-bold truncate max-w-full text-on-surface">
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
