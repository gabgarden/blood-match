import { useState } from "react";

export type BloodTypeStock = {
  type: "A+" | "A-" | "B+" | "B-" | "AB+" | "AB-" | "O+" | "O-";
  percentage: number; // 0 - 100
  label: "Crítico" | "Alerta" | "Adequado";
};

// Dados padrão demonstrativos do Semáforo de Estoque de Sangue
const INITIAL_STOCKS: BloodTypeStock[] = [
  { type: "O-", percentage: 15, label: "Crítico" },
  { type: "O+", percentage: 65, label: "Alerta" },
  { type: "A-", percentage: 25, label: "Crítico" },
  { type: "A+", percentage: 85, label: "Adequado" },
  { type: "B-", percentage: 40, label: "Alerta" },
  { type: "B+", percentage: 75, label: "Adequado" },
  { type: "AB-", percentage: 20, label: "Crítico" },
  { type: "AB+", percentage: 90, label: "Adequado" },
];

function getStatusStyle(percentage: number) {
  if (percentage < 30) {
    return {
      barBg: "bg-red-500",
      badgeBg: "bg-red-100 text-red-700",
      icon: "error",
      text: "Crítico",
    };
  }
  if (percentage <= 70) {
    return {
      barBg: "bg-amber-500",
      badgeBg: "bg-amber-100 text-amber-700",
      icon: "warning",
      text: "Alerta",
    };
  }
  return {
    barBg: "bg-emerald-500",
    badgeBg: "bg-emerald-100 text-emerald-700",
    icon: "check_circle",
    text: "Adequado",
  };
}

export function BloodStockSemaphoreWidget() {
  const [stocks] = useState<BloodTypeStock[]>(INITIAL_STOCKS);

  const criticalCount = stocks.filter((s) => s.percentage < 30).length;

  return (
    <section className="col-span-12 overflow-hidden rounded-[2rem] border border-surface-container-high bg-white p-6 shadow-sm">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between mb-6">
        <div>
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-2xl">vital_signs</span>
            <h2 className="font-headline text-xl font-extrabold text-on-surface">
              Semáforo de Estoque de Sangue
            </h2>
          </div>
          <p className="mt-1 text-sm text-text-secondary">
            Nível atual estimado das bolsas nos hemocentros da região por tipo sanguíneo.
          </p>
        </div>

        {criticalCount > 0 && (
          <div className="inline-flex items-center gap-2 rounded-xl bg-red-50 px-3.5 py-1.5 text-xs font-bold text-red-700 border border-red-100">
            <span className="h-2.5 w-2.5 rounded-full bg-red-600 animate-ping" />
            {criticalCount} tipo{criticalCount > 1 ? "s" : ""} em nível crítico
          </div>
        )}
      </div>

      {/* Grid com os 8 Tipos Sanguíneos */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {stocks.map((item) => {
          const style = getStatusStyle(item.percentage);

          return (
            <div
              key={item.type}
              className="flex flex-col justify-between rounded-2xl border border-surface-container-high bg-surface-container-low p-4 transition-all hover:shadow-md"
            >
              <div className="flex items-center justify-between">
                <span className="font-headline text-2xl font-black text-on-surface">
                  {item.type}
                </span>
                <span
                  className={`inline-flex items-center gap-1 rounded-md px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-wider ${style.badgeBg}`}
                >
                  <span className="material-symbols-outlined text-xs">{style.icon}</span>
                  {style.text}
                </span>
              </div>

              {/* Barra de Progresso do Estoque */}
              <div className="mt-4">
                <div className="flex items-center justify-between text-xs font-bold text-text-secondary mb-1">
                  <span>Capacidade</span>
                  <span>{item.percentage}%</span>
                </div>
                <div className="h-2.5 w-full rounded-full bg-surface-container-high overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-500 ${style.barBg}`}
                    style={{ width: `${item.percentage}%` }}
                  />
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
