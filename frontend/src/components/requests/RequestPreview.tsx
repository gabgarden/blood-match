type RequestPreviewProps = {
  bloodType: string;
  quantity: number;
  hospital: string;
  urgency: "BAIXA" | "MÉDIA" | "CRÍTICA";
};

export function RequestPreview({ bloodType, quantity, hospital, urgency }: RequestPreviewProps) {
  const urgencyColor =
    urgency === "CRÍTICA"
      ? "bg-[#ae131a]"
      : urgency === "MÉDIA"
        ? "bg-amber-500"
        : "bg-gray-500";

  return (
    <div className="bg-[#ae131a] bg-gradient-to-br from-[#ae131a] to-[#d2312f] rounded-[2rem] p-8 text-white shadow-2xl relative overflow-hidden">
      <div className="absolute -right-8 -top-8 w-48 h-48 bg-white/10 rounded-full blur-3xl" />

      <div className="relative z-10">
        <div className="flex justify-between items-start mb-12">
          <div className="flex flex-col">
            <span className="text-xs font-bold opacity-70 uppercase tracking-[0.2em]">Preview do Card</span>
            <h3 className="text-2xl font-extrabold font-headline">Status em Tempo Real</h3>
          </div>
          <div className="bg-white/20 backdrop-blur-md px-3 py-1 rounded-lg text-xs font-bold border border-white/20">
            AO VIVO
          </div>
        </div>

        <div className="flex items-end gap-6 mb-8">
          <div className="bg-white text-[#ae131a] w-20 h-20 rounded-[1.5rem] flex items-center justify-center shadow-2xl">
            <span className="font-headline text-4xl font-black">{bloodType || "—"}</span>
          </div>
          <div className="flex flex-col pb-1">
            <span className="text-4xl font-black font-headline">{quantity || "0"}</span>
            <span className="text-sm font-medium opacity-80">Bolsas Requisitadas</span>
          </div>
        </div>

        <div className="space-y-4">
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-sm opacity-70">local_hospital</span>
            <span className="text-sm font-medium">{hospital || "Hospital não informado"}</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-sm opacity-70">warning</span>
            <span className={`text-sm font-bold px-2 py-0.5 rounded ${urgencyColor}`}>{urgency}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
