interface TypeToggleProps {
  selected: "person" | "organization";
  onChange: (type: "person" | "organization") => void;
}

export function TypeToggle({ selected, onChange }: TypeToggleProps) {
  return (
    <div className="flex p-1 bg-gray-100 rounded-lg mb-6">
   
      <button
        type="button" // Evita submeter o form por acidente
        onClick={() => onChange("person")}
        className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
          selected === "person"
            ? "bg-white text-red-600 shadow-sm"
            : "text-gray-500 hover:text-gray-700"
        }`}
      >
        Pessoa Física
      </button>
      <button
        type="button"
        onClick={() => onChange("organization")}
        className={`flex-1 py-2 text-sm font-medium rounded-md transition-all ${
          selected === "organization"
            ? "bg-white text-red-600 shadow-sm"
            : "text-gray-500 hover:text-gray-700"
        }`}
      >
        Organização
      </button>
    </div>
  );
}