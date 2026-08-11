import { AppCard } from "../ui";

export function RequestGuide() {
  const steps = [
    {
      number: 1,
      title: "Verifique o Tipo Sanguíneo",
      description:
        "Garanta que o tipo sanguíneo selecionado é o correto para evitar desperdício de logística.",
    },
    {
      number: 2,
      title: "Hemocentro correto",
      description:
        "Busque o hemocentro pelo nome e confira cidade/UF antes de publicar. Só o local selecionado receberá as doações.",
    },
    {
      number: 3,
      title: "Impacto da Urgência",
      description:
        "Requisições críticas têm prioridade nas recomendações para doadores compatíveis na região.",
    },
  ];

  return (
    <AppCard className="p-8 bg-surface-container-low border-0">
      <h4 className="font-headline font-extrabold text-xl mb-6 flex items-center gap-2">
        <span className="material-symbols-outlined text-[#ae131a]">lightbulb</span>
        Guia de Precisão
      </h4>

      <div className="space-y-6">
        {steps.map((step) => (
          <div key={step.number} className="flex gap-4">
            <div className="w-10 h-10 shrink-0 bg-white rounded-full flex items-center justify-center text-[#ae131a] font-bold">
              {step.number}
            </div>
            <div>
              <p className="text-sm font-bold text-on-surface mb-1">{step.title}</p>
              <p className="text-xs text-secondary leading-relaxed">{step.description}</p>
            </div>
          </div>
        ))}
      </div>
    </AppCard>
  );
}
